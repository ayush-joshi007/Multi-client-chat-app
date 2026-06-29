let selectedUserId = null;
const token = localStorage.getItem("token");

if (!token) {
    window.location.href = "/login.html";
}

const client = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/ws'
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

function createMessageElement(message) {

    const messageDiv = document.createElement("div");
    const timeStampDiv = document.createElement("div");

    messageDiv.dataset.messageId = message.id;

    messageDiv.classList.add("message");
    timeStampDiv.classList.add("timeStamp");

    const myName = localStorage.getItem("userName");

    if (message.userName === myName) {
        messageDiv.classList.add("my-message");
        messageDiv.textContent =
            message.userName + " (You): " + message.content;
    }
    else {
        messageDiv.classList.add("other-message");
        messageDiv.textContent =
            message.userName + ": " + message.content;
    }

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
    loadUsers();
    client.publish({
        destination: '/app/online',
        body: localStorage.getItem("userId")
    })

    client.subscribe('/topic/messages', function(message){

        const messageData = JSON.parse(message.body);

        const div = document.getElementById("messages");
        const messageDiv = createMessageElement(messageData);

        div.appendChild(messageDiv);
        div.scrollTop = div.scrollHeight;

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
            else{
                const timeStampDiv = existingMessage.querySelector(".timeStamp");

                const formattedTime = new Date(messageData.createdAt)
                    .toLocaleTimeString([], {
                        hour: "2-digit",
                        minute: "2-digit"
                    });

                renderMessageStatus(timeStampDiv, formattedTime, messageData);

            }
        }

    });

    client.subscribe(
        '/topic/users',
        function(message){

            loadUsers();

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
function loadUsers(){
    usersDiv.innerHTML = "";
    fetch('users', {
        headers: {
            Authorization: `Bearer ${token}`
        }
    })
    .then(response => response.json())
    .then(users =>{
        usersDiv.innerHTML = "<h3>Users List</h3>";
        const publicDiv = document.createElement("div");
        publicDiv.classList.add("user");

        publicDiv.textContent = "🌐 Public Chat";
        publicDiv.addEventListener("click", function(){

            selectedUserId = null;

            client.publish({
                destination: "/app/leaveChat",
                body: localStorage.getItem("userId")
            });

            loadMessages();

        });

        usersDiv.appendChild(publicDiv);
        for(const user of users){

            const userDiv = document.createElement("div");
            userDiv.classList.add("user");
            const myId = parseInt(localStorage.getItem("userId"));

            let status = user.online ? " 🟢" : " ⚪";

            if(user.userId === myId){
                userDiv.textContent =
                    user.userName + " (You)" + status;
            }
            else{
                userDiv.textContent =
                    user.userName + status;
            }

            userDiv.addEventListener("click", function (){
                selectedUserId=user.userId;
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

                    });
                console.log(selectedUserId);
            })

            usersDiv.appendChild(userDiv);
        }
    })
    .catch(error => console.error('Error:', error));

}


