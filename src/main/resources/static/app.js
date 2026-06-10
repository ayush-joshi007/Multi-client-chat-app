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
                msgDiv.textContent=msg.content;
                div.appendChild(msgDiv);
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
        messageDiv.textContent = JSON.stringify(messageData.content);
        Div.appendChild(messageDiv);
    })
    console.log("Connected!");
};

client.activate();


const sendButton = document.getElementById("send");

sendButton.addEventListener("click", function (){
    const message = document.getElementById("message");
    const messageString={
        'content': message.value
    };
    const contentJson = JSON.stringify(messageString);
    client.publish({
        destination: '/app/send',
        body: contentJson
    });

})

console.log(sendButton);