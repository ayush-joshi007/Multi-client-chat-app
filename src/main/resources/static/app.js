const client = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/ws'
});

console.log("app.js loaded");
client.onConnect = () => {
    client.subscribe('/topic/messages', (message) => {
        console.log(message);
    })
    console.log("Connected!");
};

client.activate();

const sendButton = document.getElementById("send");
console.log(sendButton);