let selectedUserId = null;
let currentPage = 0;
let isLoadingMessages = false;
let hasMoreMessages = true;
const PAGE_SIZE = 20;

const token = localStorage.getItem("token");

if (!token) {
    window.location.href = "/login.html";
}

const client = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/ws',
    connectHeaders: {
        Authorization: `Bearer ${token}`
    }
});

console.log("app.js loaded");

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

function fetchMessages(page = 0) {
    console.log(`[PAGINATION LOG] 1. fetchMessages() called for page = ${page}. Current state -> isLoadingMessages: ${isLoadingMessages}, hasMoreMessages: ${hasMoreMessages}, currentPage: ${currentPage}`);
    
    if (isLoadingMessages) {
        console.warn(`[PAGINATION LOG] Aborting fetchMessages(${page}) because isLoadingMessages is true.`);
        return;
    }
    isLoadingMessages = true;
    console.log(`[PAGINATION LOG] Set isLoadingMessages = true`);

    const chatWindow = document.getElementById("messages");
    const myId = localStorage.getItem("userId");

    const url = selectedUserId == null
        ? `http://localhost:8080/messages?page=${page}&size=${PAGE_SIZE}`
        : `http://localhost:8080/messages/private?senderId=${myId}&receiverId=${selectedUserId}&page=${page}&size=${PAGE_SIZE}`;

    const oldScrollHeight = chatWindow.scrollHeight;
    console.log(`[PAGINATION LOG] Requesting URL: ${url}. Element measurements before fetch -> scrollHeight: ${oldScrollHeight}, clientHeight: ${chatWindow.clientHeight}, scrollTop: ${chatWindow.scrollTop}`);

    fetch(url, {
        headers: {
            Authorization: `Bearer ${token}`
        }
    })
        .then(response => response.json())
        .then(data => {
            console.log(`[PAGINATION LOG] 2. Server Response received for page ${page}:`, data);
            
            const messages = data.content || [];
            hasMoreMessages = data.hasNext !== undefined ? data.hasNext : !data.last;
            currentPage = page;

            console.log(`[PAGINATION LOG] State updated -> messages returned count: ${messages.length}, data.hasNext: ${data.hasNext}, data.last: ${data.last}, updated hasMoreMessages: ${hasMoreMessages}, updated currentPage: ${currentPage}`);

            if (page === 0) {
                chatWindow.innerHTML = "";
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
                console.log(`[PAGINATION LOG] Page 0 rendered. Post-render measurements -> scrollHeight: ${chatWindow.scrollHeight}, clientHeight: ${chatWindow.clientHeight}, scrollTop: ${chatWindow.scrollTop}, isOverflowing: ${chatWindow.scrollHeight > chatWindow.clientHeight}`);
            } else {
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
                console.log(`[PAGINATION LOG] Page ${page} prepended. Post-prepended measurements -> new scrollHeight: ${chatWindow.scrollHeight}, oldScrollHeight: ${oldScrollHeight}, new scrollTop: ${chatWindow.scrollTop}`);
            }

            if (selectedUserId != null && page === 0) {
                loadConversations();
            }
        })
        .catch(error => console.error("[PAGINATION LOG] Error loading messages:", error))
        .finally(() => {
            isLoadingMessages = false;
            console.log(`[PAGINATION LOG] Set isLoadingMessages = false`);
        });
}

function loadMessages() {
    console.log("LOAD MESSAGES CALLED", new Date());
    currentPage = 0;
    hasMoreMessages = true;
    fetchMessages(0);
}

client.onConnect = () => {

    console.log("CONNECTED", new Date());

    loadMessages();
    loadConversations();

    client.subscribe('/topic/messages', function(message){

        const messageData = JSON.parse(message.body);

        const div = document.getElementById("messages");
        const existingMessage = document.querySelector(
            `[data-message-id="${messageData.id}"]`
        );

        if (existingMessage == null) {
            const messageDiv = createMessageElement(messageData);

            div.appendChild(messageDiv);
            div.scrollTop = div.scrollHeight;
        }
        else {
            updateExistingMessageElement(existingMessage, messageData);
        }

        console.log("RECEIVED!", new Date());
    });

    client.subscribe('/topic/user/'+localStorage.getItem("userId"), function (message){

        console.log(JSON.parse(message.body));
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

            const existingMessage = document.querySelector(
                `[data-message-id="${messageData.id}"]`
            );

            if(existingMessage == null){

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

    console.log("Connected!");
};

client.onWebSocketClose = () => {
    console.log("WEBSOCKET CLOSED", new Date());
};

client.onDisconnect = () => {
    console.log("DISCONNECTED", new Date());
};

client.activate();


const sendButton = document.getElementById("send");

let message = document.getElementById("message");
message.addEventListener("keydown", function (event){
    if(event.key === "Enter"){
        let sendButton = document.getElementById("send");
        sendButton.click();
    }
})

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
    messageBox.value='';

})

console.log(sendButton);


const logoutBtn = document.getElementById("logoutBtn");
logoutBtn.addEventListener("click", function (){
    localStorage.clear();
    window.location.href = "login.html";
})

const usersDiv = document.getElementById("users");
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

function openPrivateConversation(partnerId) {
    selectedUserId = partnerId;
    currentPage = 0;
    hasMoreMessages = true;

    fetchMessages(0);

    console.log(selectedUserId);
}

function createPublicChatElement() {
    const publicDiv = document.createElement("div");
    publicDiv.classList.add("user");

    publicDiv.textContent = "Public Chat";
    publicDiv.addEventListener("click", function(){

        selectedUserId = null;

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
    headerDiv.classList.add("conversation-header");
    nameSpan.classList.add("conversation-name");
    statusSpan.classList.add("status-dot");
    timeSpan.classList.add("conversation-time");
    previewDiv.classList.add("conversation-preview");

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

    if (conversation.unreadCount > 0) {
        const unreadSpan = document.createElement("span");
        unreadSpan.classList.add("unread-count");
        unreadSpan.textContent = conversation.unreadCount;
        previewDiv.appendChild(unreadSpan);
    }

    conversationDiv.appendChild(headerDiv);
    conversationDiv.appendChild(previewDiv);

    conversationDiv.addEventListener("click", function (){
        openPrivateConversation(conversation.partnerId);
    });

    return conversationDiv;
}

function loadConversations(){

    usersDiv.innerHTML = "";
    fetch('http://localhost:8080/conversations', {
        headers: {
            Authorization: `Bearer ${token}`
        }
    })
    .then(response => response.json())
    .then(conversations =>{
        usersDiv.innerHTML = "<h3>Conversations</h3>";
        usersDiv.appendChild(createPublicChatElement());

        for(const conversation of conversations){
            usersDiv.appendChild(createConversationElement(conversation));
        }
    })
    .catch(error => console.error('Error:', error));
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
    console.log("[PAGINATION LOG] Attaching scroll event listener to #messages container");
    messagesContainer.addEventListener("scroll", function () {
        console.log(`[PAGINATION LOG] Scroll event fired! scrollTop: ${messagesContainer.scrollTop}, hasMoreMessages: ${hasMoreMessages}, isLoadingMessages: ${isLoadingMessages}, condition (scrollTop <= 30 && hasMoreMessages && !isLoadingMessages): ${messagesContainer.scrollTop <= 30 && hasMoreMessages && !isLoadingMessages}`);
        
        if (messagesContainer.scrollTop <= 30 && hasMoreMessages && !isLoadingMessages) {
            console.log(`[PAGINATION LOG] Triggering fetchMessages(page = ${currentPage + 1}) from scroll event!`);
            fetchMessages(currentPage + 1);
        }
    });
}


