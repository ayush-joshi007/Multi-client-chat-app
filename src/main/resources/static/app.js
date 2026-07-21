let selectedUserId = null;
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

function loadMessages(){

    console.log("LOAD MESSAGES CALLED", new Date());

    const div = document.getElementById("messages");
    div.innerHTML = "";

    fetch('http://localhost:8080/messages', {
        headers: {
            Authorization: `Bearer ${token}`
        }
    })
        .then(response => response.json())
        .then(data =>{
            let lastSender = null;

            for(const msg of data){

                const messageDiv = createMessageElement(msg);

                if(lastSender !== msg.userName){
                    messageDiv.style.marginTop = "20px";
                }

                lastSender = msg.userName;

                div.appendChild(messageDiv);
            }

            div.scrollTop = div.scrollHeight;
        })
        .catch(error => console.error('Error:', error));
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

    const chatWindow = document.getElementById("messages");
    const myId = localStorage.getItem("userId");
    chatWindow.innerHTML = "";

    fetch(
        `http://localhost:8080/messages/private?senderId=${myId}&receiverId=${selectedUserId}`,
        {
            headers:{
                Authorization: `Bearer ${token}`
            }
        }
    )
    .then(response => response.json())
    .then(data => {

        let lastSender = null;

        data.forEach(message => {

            const messageDiv = createMessageElement(message);

            if(lastSender !== message.userName){
                messageDiv.style.marginTop = "20px";
            }

            lastSender = message.userName;

            chatWindow.appendChild(messageDiv);

        });

        chatWindow.scrollTop = chatWindow.scrollHeight;

        loadConversations();
    });

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

