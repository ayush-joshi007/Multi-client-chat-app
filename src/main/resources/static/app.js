const userId = localStorage.getItem("userId");
if(userId==null){
    window.location.href = 'login.html';
}

const client = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/ws'
});

console.log("app.js loaded");

function loadMessages(){

    console.log("LOAD MESSAGES CALLED", new Date());

    const div = document.getElementById("messages");
    div.innerHTML = "";

    fetch('http://localhost:8080/messages')
        .then(response => response.json())
        .then(data =>{
            for(const msg of data){

                const msgDiv = document.createElement("div");
                msgDiv.classList.add("message");
                msgDiv.textContent =
                    msg.userName + ": " + msg.content;

                div.appendChild(msgDiv);
            }

            div.scrollTop = div.scrollHeight;
        })
        .catch(error => console.error('Error:', error));
}

client.onConnect = () => {

    console.log("CONNECTED", new Date());

    loadMessages();

    client.subscribe('/topic/messages', function(message){

        const messageData = JSON.parse(message.body);

        const div = document.getElementById("messages");
        const messageDiv = document.createElement("div");

        messageDiv.classList.add("message");
        messageDiv.textContent =
            messageData.userName + ": " + messageData.content;

        div.appendChild(messageDiv);
        div.scrollTop = div.scrollHeight;

        console.log("RECEIVED!", new Date());
    });

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
    const messageString={
        content: messageBox.value,
        senderId: parseInt(localStorage.getItem("userId")),
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