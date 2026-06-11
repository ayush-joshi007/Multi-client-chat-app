const userName= prompt("Enter username: ");
const client = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/ws'
});

console.log("app.js loaded");

function loadMessages(){
    fetch('http://localhost:8080/messages')
        .then(response => response.json())
        .then(data =>{
            for(const msg of data){
                const div = document.getElementById("messages");
                const msgDiv = document.createElement("div");
                msgDiv.classList.add("message");
                msgDiv.textContent=msg.content;
                div.appendChild(msgDiv);
                div.scrollTop=div.scrollHeight;
            }
        })
        .catch(error => console.error('Error:', error));
}
loadMessages();

client.onConnect = () => {
    client.subscribe('/topic/messages', (message) => {
         const messageData = JSON.parse(message.body);
         const Div = document.getElementById("messages");
         const messageDiv = document.createElement("div");
         messageDiv.classList.add("message");
         // Display userName + ": " + content for WebSocket messages
         const displayText = messageData.userName ? `${messageData.userName}: ${messageData.content}` : messageData.content;
         messageDiv.textContent = displayText;
         Div.appendChild(messageDiv);
         Div.scrollTop = Div.scrollHeight;
         console.log("received!");
     })
     console.log("Connected!");
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
        'content': messageBox.value,


        'userName': userName //TEMPORARY SENDER NAME



    };
    const contentJson = JSON.stringify(messageString);
    client.publish({
        destination: '/app/send',
        body: contentJson
    });
    messageBox.value='';

})


console.log(sendButton);