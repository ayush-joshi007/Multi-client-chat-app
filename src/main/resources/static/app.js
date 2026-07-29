let selectedUserId = null;
let selectedUserName = null;
let currentPage = 0;
let isLoadingMessages = false;
let hasMoreMessages = true;
let typingTimeoutId = null;
let typingStarted = false;
const PAGE_SIZE = 20;
const DEBUG = false;

document.body.style.visibility = "hidden";

const token = localStorage.getItem("token");

let client = null;
const websocketProtocol = window.location.protocol === "https:" ? "wss:" : "ws:";

function debugLog(...args) {
    if (DEBUG) {
        console.log(...args);
    }
}

function debugWarn(...args) {
    if (DEBUG) {
        console.warn(...args);
    }
}

function redirectToLogin() {
    localStorage.clear();

    if (client != null && client.connected) {
        client.deactivate();
    }

    window.location.replace("/login.html");
}

function parseJwtPayload(jwt) {
    const payload = jwt.split(".")[1];
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
        atob(base64)
            .split("")
            .map(function (char) {
                return "%" + ("00" + char.charCodeAt(0).toString(16)).slice(-2);
            })
            .join("")
    );

    return JSON.parse(jsonPayload);
}

function isTokenExpired(jwt) {
    try {
        const payload = parseJwtPayload(jwt);

        if (payload.exp == null) {
            return true;
        }

        return payload.exp * 1000 <= Date.now();
    } catch (error) {
        return true;
    }
}

function authenticatedFetch(url, options = {}) {
    const headers = {
        ...(options.headers || {}),
        Authorization: `Bearer ${token}`
    };

    return fetch(url, {
        ...options,
        headers: headers
    }).then(response => {
        if (response.status === 401 || response.status === 403) {
            redirectToLogin();
            throw new Error("Authentication expired");
        }

        if (!response.ok) {
            throw new Error("Request failed with status " + response.status);
        }

        return response;
    });
}

function validateStoredSession() {
    if (!token || isTokenExpired(token)) {
        redirectToLogin();
        return Promise.reject(new Error("Missing or expired token"));
    }

    return authenticatedFetch("/conversations")
        .then(response => response.json());
}

if (!token) {
    redirectToLogin();
    throw new Error("Missing authentication token");
}

client = new StompJs.Client({
    brokerURL: `${websocketProtocol}//${window.location.host}/ws`,
    connectHeaders: {
        Authorization: `Bearer ${token}`
    }
});

debugLog("app.js loaded");

function renderMessageStatus(timeStampDiv, formattedTime, message) {

    const myId = parseInt(localStorage.getItem("userId"));

    // Show timestamp for everyone.
    timeStampDiv.textContent = formattedTime;

    // Only sender sees ticks.
    if (message.senderId !== myId) {
        return;
    }

    // Public chat has no ticks.
    if (message.receiverId == null) {
        return;
    }

    switch (message.status) {

        case "SENT":
            timeStampDiv.textContent = formattedTime + " ✓";
            break;

        case "DELIVERED":
            timeStampDiv.textContent = formattedTime + " ✓✓";
            break;

        case "READ":
            timeStampDiv.innerHTML =
                formattedTime +
                ' <span class="status-tick read">✓✓</span>';
            break;

        default:
            timeStampDiv.textContent = formattedTime;
    }
}

function isOwnMessage(message) {
    const myId = parseInt(localStorage.getItem("userId"));

    return message.senderId === myId;
}

function startEditingMessage(message) {
    const editedContent = prompt("Edit message", message.content);

    if (editedContent == null) {
        return;
    }

    const trimmedContent = editedContent.trim();

    if (trimmedContent === "" || trimmedContent === message.content) {
        return;
    }

    client.publish({
        destination: "/app/edit",
        body: JSON.stringify({
            messageId: message.id,
            content: trimmedContent
        })
    });
}

function renderMessageActions(messageDiv, message) {
    let actionsDiv = messageDiv.querySelector(".message-actions");

    if (!isOwnMessage(message)) {
        if (actionsDiv != null) {
            actionsDiv.remove();
        }
        return;
    }

    if (actionsDiv != null) {
        actionsDiv.remove();
    }

    if (message.deleted) {
        return;
    }

    actionsDiv = document.createElement("div");
    actionsDiv.classList.add("message-actions");

    const editButton = document.createElement("button");
    editButton.type = "button";
    editButton.classList.add("edit-message-btn");
    editButton.textContent = "Edit";

    editButton.addEventListener("click", function () {
        startEditingMessage(message);
    });

    const deleteButton = document.createElement("button");
    deleteButton.type = "button";
    deleteButton.classList.add("delete-message-btn");
    deleteButton.textContent = "Delete";

    deleteButton.addEventListener("click", function () {
        startDeletingMessage(message);
    });

    actionsDiv.appendChild(editButton);
    actionsDiv.appendChild(deleteButton);

    const timeStampDiv = messageDiv.querySelector(".timeStamp");

    if (timeStampDiv == null) {
        messageDiv.appendChild(actionsDiv);
    }
    else {
        messageDiv.insertBefore(actionsDiv, timeStampDiv);
    }
}

function renderMessageText(messageDiv, message) {

    const myName = localStorage.getItem("userName");
    const editedText = message.edited ? " (edited)" : "";
    let contentSpan = messageDiv.querySelector(".message-content");

    if (contentSpan == null) {
        contentSpan = document.createElement("span");
        contentSpan.classList.add("message-content");
        messageDiv.insertBefore(contentSpan, messageDiv.firstChild);
    }

    if (message.userName === myName) {
        contentSpan.textContent =
            message.userName + " (You): " + message.content + editedText;
    }
    else {
        contentSpan.textContent =
            message.userName + ": " + message.content + editedText;
    }

    if (message.deleted) {
        contentSpan.classList.add("deleted-message");
    }
    else {
        contentSpan.classList.remove("deleted-message");
    }

    renderMessageActions(messageDiv, message);
}

function updateExistingMessageElement(messageDiv, message) {

    const timeStampDiv = messageDiv.querySelector(".timeStamp");

    renderMessageText(messageDiv, message);

    const formattedTime = new Date(message.createdAt)
        .toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit"
        });

    renderMessageStatus(timeStampDiv, formattedTime, message);

    messageDiv.appendChild(timeStampDiv);
}

function createMessageElement(message) {

    const messageDiv = document.createElement("div");
    const timeStampDiv = document.createElement("div");

    messageDiv.dataset.messageId = message.id;

    messageDiv.classList.add("message");
    timeStampDiv.classList.add("timeStamp");

    const myName = localStorage.getItem("userName");

    if (message.userName === myName) {
        messageDiv.classList.add("my-message");
    }
    else {
        messageDiv.classList.add("other-message");
    }

    renderMessageText(messageDiv, message);

    const formattedTime = new Date(message.createdAt)
        .toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit"
        });

    renderMessageStatus(timeStampDiv, formattedTime, message);

    messageDiv.appendChild(timeStampDiv);

    return messageDiv;
}

function removeChatStateElements(chatWindow) {
    chatWindow
        .querySelectorAll(".chat-empty-state, .chat-loading-state, .chat-error-state")
        .forEach(function (stateElement) {
            stateElement.remove();
        });
}

function removeChatLoadingElements(chatWindow) {
    chatWindow
        .querySelectorAll(".chat-loading-state")
        .forEach(function (loadingElement) {
            loadingElement.remove();
        });
}

function renderChatLoading(chatWindow, page) {
    const loadingDiv = document.createElement("div");

    loadingDiv.classList.add("chat-loading-state");
    loadingDiv.textContent = page === 0 ? "Loading messages..." : "Loading older messages...";

    if (page === 0) {
        chatWindow.innerHTML = "";
        chatWindow.appendChild(loadingDiv);
        return;
    }

    chatWindow.insertBefore(loadingDiv, chatWindow.firstChild);
}

function renderChatEmptyState(chatWindow) {
    const emptyDiv = document.createElement("div");

    emptyDiv.classList.add("chat-empty-state");
    emptyDiv.textContent = selectedUserId == null
        ? "No public messages yet. Start the conversation."
        : `No messages with ${selectedUserName} yet.`;

    chatWindow.appendChild(emptyDiv);
}

function renderChatErrorState(chatWindow) {
    const errorDiv = document.createElement("div");

    errorDiv.classList.add("chat-error-state");
    errorDiv.textContent = "Messages could not be loaded. Please try again.";

    chatWindow.innerHTML = "";
    chatWindow.appendChild(errorDiv);
}

function fetchMessages(page = 0) {
    debugLog(`[PAGINATION LOG] 1. fetchMessages() called for page = ${page}. Current state -> isLoadingMessages: ${isLoadingMessages}, hasMoreMessages: ${hasMoreMessages}, currentPage: ${currentPage}`);
    
    if (isLoadingMessages) {
        debugWarn(`[PAGINATION LOG] Aborting fetchMessages(${page}) because isLoadingMessages is true.`);
        return;
    }
    isLoadingMessages = true;
    debugLog(`[PAGINATION LOG] Set isLoadingMessages = true`);

    const chatWindow = document.getElementById("messages");
    const myId = localStorage.getItem("userId");

    const url = selectedUserId == null
        ? `/messages?page=${page}&size=${PAGE_SIZE}`
        : `/messages/private?senderId=${myId}&receiverId=${selectedUserId}&page=${page}&size=${PAGE_SIZE}`;

    const oldScrollHeight = chatWindow.scrollHeight;
    renderChatLoading(chatWindow, page);
    debugLog(`[PAGINATION LOG] Requesting URL: ${url}. Element measurements before fetch -> scrollHeight: ${oldScrollHeight}, clientHeight: ${chatWindow.clientHeight}, scrollTop: ${chatWindow.scrollTop}`);

    authenticatedFetch(url)
        .then(response => response.json())
        .then(data => {
            debugLog(`[PAGINATION LOG] 2. Server Response received for page ${page}:`, data);
            
            const messages = data.content || [];
            hasMoreMessages = data.hasNext !== undefined ? data.hasNext : !data.last;
            currentPage = page;

            debugLog(`[PAGINATION LOG] State updated -> messages returned count: ${messages.length}, data.hasNext: ${data.hasNext}, data.last: ${data.last}, updated hasMoreMessages: ${hasMoreMessages}, updated currentPage: ${currentPage}`);

            if (page === 0) {
                chatWindow.innerHTML = "";

                if (messages.length === 0) {
                    renderChatEmptyState(chatWindow);
                    return;
                }

                let lastSender = null;

                for (const msg of messages) {
                    if (document.querySelector(`[data-message-id="${msg.id}"]`)) continue;

                    const messageDiv = createMessageElement(msg);

                    if (lastSender !== msg.userName) {
                        messageDiv.style.marginTop = "20px";
                    }

                    lastSender = msg.userName;
                    chatWindow.appendChild(messageDiv);
                }

                chatWindow.scrollTop = chatWindow.scrollHeight;
                debugLog(`[PAGINATION LOG] Page 0 rendered. Post-render measurements -> scrollHeight: ${chatWindow.scrollHeight}, clientHeight: ${chatWindow.clientHeight}, scrollTop: ${chatWindow.scrollTop}, isOverflowing: ${chatWindow.scrollHeight > chatWindow.clientHeight}`);
            } else {
                removeChatStateElements(chatWindow);

                const fragment = document.createDocumentFragment();
                let lastSender = null;

                for (const msg of messages) {
                    if (document.querySelector(`[data-message-id="${msg.id}"]`)) continue;

                    const messageDiv = createMessageElement(msg);

                    if (lastSender !== msg.userName) {
                        messageDiv.style.marginTop = "20px";
                    }

                    lastSender = msg.userName;
                    fragment.appendChild(messageDiv);
                }

                chatWindow.insertBefore(fragment, chatWindow.firstChild);

                chatWindow.scrollTop = chatWindow.scrollHeight - oldScrollHeight;
                debugLog(`[PAGINATION LOG] Page ${page} prepended. Post-prepended measurements -> new scrollHeight: ${chatWindow.scrollHeight}, oldScrollHeight: ${oldScrollHeight}, new scrollTop: ${chatWindow.scrollTop}`);
            }

            if (selectedUserId != null && page === 0) {
                loadConversations();
            }
        })
        .catch(error => {
            console.error("Error loading messages:", error);

            if (page === 0) {
                renderChatErrorState(chatWindow);
                return;
            }

            removeChatStateElements(chatWindow);
        })
        .finally(() => {
            isLoadingMessages = false;
            removeChatLoadingElements(chatWindow);
            debugLog(`[PAGINATION LOG] Set isLoadingMessages = false`);
        });
}

function loadMessages() {
    debugLog("LOAD MESSAGES CALLED", new Date());
    currentPage = 0;
    hasMoreMessages = true;
    fetchMessages(0);
}

function findConversationPreview(partnerId) {
    return document.querySelector(
        `.conversation-preview[data-partner-id="${partnerId}"]`
    );
}

function showTypingPreview(senderId) {
    const previewDiv = findConversationPreview(senderId);

    if (previewDiv == null) {
        return;
    }

    if (!previewDiv.classList.contains("typing-preview")) {
        previewDiv.dataset.originalPreview = previewDiv.textContent;
    }

    previewDiv.textContent = "typing...";
    previewDiv.classList.add("typing-preview");
}

function hideTypingPreview(senderId) {
    const previewDiv = findConversationPreview(senderId);

    if (previewDiv == null) {
        return;
    }

    if (previewDiv.dataset.originalPreview != null) {
        previewDiv.textContent = previewDiv.dataset.originalPreview;
        delete previewDiv.dataset.originalPreview;
    }

    previewDiv.classList.remove("typing-preview");
}

function clearTypingPreviews() {
    const typingPreviews = document.querySelectorAll(".conversation-preview.typing-preview");

    typingPreviews.forEach(function (previewDiv) {
        if (previewDiv.dataset.originalPreview != null) {
            previewDiv.textContent = previewDiv.dataset.originalPreview;
            delete previewDiv.dataset.originalPreview;
        }

        previewDiv.classList.remove("typing-preview");
    });
}

function sendTypingStatus(typing) {
    if (selectedUserId == null || !client.connected) {
        return false;
    }

    client.publish({
        destination: "/app/typing",
        body: JSON.stringify({
            senderId: parseInt(localStorage.getItem("userId")),
            receiverId: selectedUserId,
            typing: typing
        })
    });

    return true;
}

function stopTyping() {
    if (typingTimeoutId != null) {
        clearTimeout(typingTimeoutId);
        typingTimeoutId = null;
    }

    if (!typingStarted) {
        return;
    }

    sendTypingStatus(false);
    typingStarted = false;
}

function handleTypingKeydown() {
    if (selectedUserId == null) {
        stopTyping();
        return;
    }

    if (!typingStarted) {
        typingStarted = sendTypingStatus(true);
    }

    if (typingTimeoutId != null) {
        clearTimeout(typingTimeoutId);
    }

    typingTimeoutId = setTimeout(function () {
        stopTyping();
    }, 1000);
}

client.onConnect = () => {

    debugLog("CONNECTED", new Date());

    loadMessages();
    loadConversations();

    client.subscribe('/topic/messages', function(message){

        const messageData = JSON.parse(message.body);

        const div = document.getElementById("messages");
        const existingMessage = document.querySelector(
            `[data-message-id="${messageData.id}"]`
        );

        if (existingMessage == null) {
            removeChatStateElements(div);
            const messageDiv = createMessageElement(messageData);

            div.appendChild(messageDiv);
            div.scrollTop = div.scrollHeight;
        }
        else {
            updateExistingMessageElement(existingMessage, messageData);
        }

        debugLog("RECEIVED!", new Date());
    });

    client.subscribe('/topic/user/'+localStorage.getItem("userId"), function (message){

        debugLog(JSON.parse(message.body));
        const messageData = JSON.parse(message.body);

        const myId = parseInt(localStorage.getItem("userId"));

        if(
            (messageData.senderId === selectedUserId &&
                messageData.receiverId === myId)

            ||

            (messageData.senderId === myId &&
                messageData.receiverId === selectedUserId)
        ){

            const div = document.getElementById("messages");

            if (messageData.senderId === selectedUserId) {
                hideTypingPreview(messageData.senderId);
            }

            const existingMessage = document.querySelector(
                `[data-message-id="${messageData.id}"]`
            );

            if(existingMessage == null){

                removeChatStateElements(div);
                const messageDiv = createMessageElement(messageData);

                div.appendChild(messageDiv);
                div.scrollTop = div.scrollHeight;

            }
            else {
                updateExistingMessageElement(existingMessage, messageData);
            }
        }
        loadConversations();

    });

    client.subscribe(
        '/topic/users',
        function(message){

            loadConversations();

        }
    );

    client.subscribe('/topic/typing/' + localStorage.getItem("userId"), function(message) {
        const typingData = JSON.parse(message.body);

        if (typingData.senderId !== selectedUserId) {
            return;
        }

        if (!typingData.typing) {
            hideTypingPreview(typingData.senderId);
            return;
        }

        showTypingPreview(typingData.senderId);
    });

    debugLog("Connected!");
};

client.onWebSocketClose = (event) => {
    stopTyping();
    debugLog("WEBSOCKET CLOSED", event);
};

client.onDisconnect = () => {
    stopTyping();
    debugLog("DISCONNECTED", new Date());
};

validateStoredSession()
    .then(function () {
        document.body.style.visibility = "visible";
        client.activate();
    })
    .catch(function (error) {
        console.error("Authentication validation failed:", error);
        redirectToLogin();
    });


const sendButton = document.getElementById("send");

let message = document.getElementById("message");
message.addEventListener("keydown", function (event){
    if(event.key === "Enter"){
        let sendButton = document.getElementById("send");
        sendButton.click();
    }
});

message.addEventListener("input", function () {
    if (message.value.trim() === "") {
        stopTyping();
        return;
    }

    handleTypingKeydown();
});

sendButton.addEventListener("click", function (){
    const messageBox = document.getElementById("message");
    if(messageBox.value.trim()===""){
        return;
    }
    const messageString={
        content: messageBox.value,
        senderId: parseInt(localStorage.getItem("userId")),
        receiverId: selectedUserId,
        userName: localStorage.getItem("userName")
    };
    const contentJson = JSON.stringify(messageString);
    client.publish({
        destination: '/app/send',
        body: contentJson
    });
    stopTyping();
    messageBox.value='';

})

debugLog(sendButton);


const logoutBtn = document.getElementById("logoutBtn");
logoutBtn.addEventListener("click", function (){
    if (client != null && client.connected) {
        client.deactivate();
    }

    localStorage.clear();
    window.location.href = "login.html";
})

const usersDiv = document.getElementById("users");

function setActiveConversation(partnerId) {
    document.querySelectorAll(".user.active-conversation").forEach(function (conversationDiv) {
        conversationDiv.classList.remove("active-conversation");
    });

    const activeConversation = document.querySelector(
        `.user[data-partner-id="${partnerId}"]`
    );

    if (activeConversation != null) {
        activeConversation.classList.add("active-conversation");
    }
}

function formatConversationTime(lastMessageTime) {
    if (lastMessageTime == null) {
        return "";
    }

    return new Date(lastMessageTime)
        .toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit"
        });
}

function openPrivateConversation(partnerId, partnerUsername) {
    stopTyping();
    selectedUserId = partnerId;
    selectedUserName = partnerUsername;
    currentPage = 0;
    hasMoreMessages = true;
    clearTypingPreviews();
    setActiveConversation(partnerId);

    fetchMessages(0);

    debugLog(selectedUserId);
}

function createPublicChatElement() {
    const publicDiv = document.createElement("div");
    publicDiv.classList.add("user");
    publicDiv.dataset.partnerId = "public";

    if (selectedUserId == null) {
        publicDiv.classList.add("active-conversation");
    }

    publicDiv.textContent = "Public Chat";
    publicDiv.addEventListener("click", function(){

        stopTyping();
        selectedUserId = null;
        selectedUserName = null;
        clearTypingPreviews();
        setActiveConversation("public");

        client.publish({
            destination: "/app/leaveChat"
        });

        loadMessages();

    });

    return publicDiv;
}

function createConversationElement(conversation) {
    const myId = parseInt(localStorage.getItem("userId"));
    const conversationDiv = document.createElement("div");
    const headerDiv = document.createElement("div");
    const nameSpan = document.createElement("span");
    const statusSpan = document.createElement("span");
    const timeSpan = document.createElement("span");
    const previewDiv = document.createElement("div");

    conversationDiv.classList.add("user", "conversation");
    conversationDiv.dataset.partnerId = conversation.partnerId;
    headerDiv.classList.add("conversation-header");
    nameSpan.classList.add("conversation-name");
    statusSpan.classList.add("status-dot");
    timeSpan.classList.add("conversation-time");
    previewDiv.classList.add("conversation-preview");
    previewDiv.dataset.partnerId = conversation.partnerId;

    if (conversation.partnerId === selectedUserId) {
        conversationDiv.classList.add("active-conversation");
    }

    statusSpan.classList.add(conversation.online ? "online" : "offline");

    nameSpan.textContent = conversation.partnerUsername;
    if (conversation.partnerId === myId) {
        nameSpan.textContent += " (You)";
    }

    timeSpan.textContent = formatConversationTime(conversation.lastMessageTime);

    headerDiv.appendChild(nameSpan);
    headerDiv.appendChild(statusSpan);
    headerDiv.appendChild(timeSpan);

    if (conversation.lastMessage == null) {
        previewDiv.textContent = "Start a conversation";
    }
    else if (conversation.lastSenderId === myId) {
        previewDiv.textContent = "You: " + conversation.lastMessage;
    }
    else {
        previewDiv.textContent = conversation.lastMessage;
    }

    if (conversation.lastMessageDeleted) {
        previewDiv.classList.add("deleted-message");
    } else {
        previewDiv.classList.remove("deleted-message");
    }

    if (conversation.unreadCount > 0) {
        const unreadSpan = document.createElement("span");
        unreadSpan.classList.add("unread-count");
        unreadSpan.textContent = conversation.unreadCount;
        previewDiv.appendChild(unreadSpan);
    }

    conversationDiv.appendChild(headerDiv);
    conversationDiv.appendChild(previewDiv);

    conversationDiv.addEventListener("click", function (){
        openPrivateConversation(conversation.partnerId, conversation.partnerUsername);
    });

    return conversationDiv;
}

function loadConversations(){

    usersDiv.innerHTML = "";
    authenticatedFetch('/conversations')
    .then(response => response.json())
    .then(conversations =>{
        usersDiv.innerHTML = "<h3>Conversations</h3>";
        usersDiv.appendChild(createPublicChatElement());

        for(const conversation of conversations){
            usersDiv.appendChild(createConversationElement(conversation));
        }
    })
    .catch(error => console.error("Error loading conversations:", error));
}


function startDeletingMessage(message) {

    const confirmed = window.confirm("Delete this message?");

    if (!confirmed) {
        return;
    }

    client.publish({
        destination: "/app/delete",
        body: JSON.stringify({
            messageId: message.id
        })
    });

}

const messagesContainer = document.getElementById("messages");
if (messagesContainer) {
    debugLog("[PAGINATION LOG] Attaching scroll event listener to #messages container");
    messagesContainer.addEventListener("scroll", function () {
        debugLog(`[PAGINATION LOG] Scroll event fired! scrollTop: ${messagesContainer.scrollTop}, hasMoreMessages: ${hasMoreMessages}, isLoadingMessages: ${isLoadingMessages}, condition (scrollTop <= 30 && hasMoreMessages && !isLoadingMessages): ${messagesContainer.scrollTop <= 30 && hasMoreMessages && !isLoadingMessages}`);
        
        if (messagesContainer.scrollTop <= 30 && hasMoreMessages && !isLoadingMessages) {
            debugLog(`[PAGINATION LOG] Triggering fetchMessages(page = ${currentPage + 1}) from scroll event!`);
            fetchMessages(currentPage + 1);
        }
    });
}


