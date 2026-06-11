# Multi-client-chat-app
This is a basic multiclient chat application, we are building for a mini-project for college submission.
Built as a learning project to understand Spring Boot, WebSockets, REST APIs, frontend-backend communication, and database persistence.


# Chat App - Version 1
A real-time public chat application built using Spring Boot, WebSockets, JavaScript, and a relational database.

## Features

### Real-Time Messaging

* Messages are delivered instantly to all connected clients using WebSockets.
* No page refresh is required to receive new messages.
* Multiple users can participate in the chat simultaneously.

### Persistent Message Storage

* Messages are stored in the database.
* Chat history is preserved even after restarting the application or refreshing the page.
* Previously sent messages are loaded automatically when the application starts.

### Live Sender Identification

* Users can enter a temporary display name when joining the chat.
* Sender names are displayed in the live chat feed so participants can identify who sent each message.
* No authentication system is required in Version 1.

### User Experience Improvements

* Send messages using either the Send button or the Enter key.
* Input field is cleared automatically after sending a message.
* Chat area automatically scrolls to the latest message.
* Long messages are supported and displayed correctly.
* Messages are visually separated for improved readability.

## Technologies Used

### Backend

* Java
* Spring Boot
* Spring WebSocket (STOMP)
* Spring Data JPA
* Hibernate

### Frontend

* HTML
* CSS
* JavaScript

### Database

* Relational database managed through JPA/Hibernate

## Architecture

The application uses a publish-subscribe messaging model:

1. A user sends a message from the browser.
2. The message is transmitted to the Spring Boot backend using WebSockets.
3. The backend persists the message in the database.
4. The message is broadcast to all connected clients.
5. Connected clients receive and display the message in real time.

## Version 1 Scope

Version 1 focuses on building a functional real-time chat room with message persistence and live communication.

The following features are intentionally deferred to future versions:

* User registration
* User login
* JWT authentication
* Private messaging
* Online user tracking
* Typing indicators
* Read receipts
* File sharing
* Group chats

