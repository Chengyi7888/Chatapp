# Chatapp

Chatapp is a lightweight self-hosted messaging application with a native Android client and a Node.js server.

It is designed for private networks, local development, and small personal deployments. The client communicates with the server over a line-delimited JSON protocol carried by a TCP connection. A small HTTP service provides avatars, uploaded files, and APK update metadata.

## Features

- Account registration and login
- Password hashing with per-user salts
- Direct messaging with conversation history
- Friend requests, friend lists, blocking, and unread counts
- Group chats with membership management
- Text, image, file, reply, and forwarded-message workflows
- Avatars and file attachments
- Favorites, drafts, pinned messages, and local chat tools
- Profile editing and account deletion
- Activity feed and built-in mini-game entry
- Chinese, Traditional Chinese, and English interface strings
- APK update checking through the server HTTP endpoint

## Project Structure

```text
android/    Native Android client source and manifest
server/     Node.js TCP/HTTP server and protocol test scripts
assets/     Project assets
docs/       Internal UI and architecture notes
```

Runtime data, logs, APKs, and compiled build output are intentionally not tracked in this repository.

## Requirements

- Android SDK with API level 34 available for client builds
- Android minimum API level 24
- Node.js 18 or newer
- A device or emulator that can reach the server over the local network

## Running the Server

From the repository root:

```powershell
$env:PORT = "8899"
$env:UPDATE_PORT = "8900"
node server/server.js
```

The server listens on:

- TCP `8899` for the Chatapp protocol
- HTTP `8900` for avatars, files, and update metadata

The Android login screen should use the server machine's reachable IP address and TCP port, for example:

```text
192.168.1.10:8899
```

The server creates its runtime data beside `server/server.js`:

```text
server/users.json
server/messages.json
server/groups.json
server/groupMessages.json
server/posts.json
server/activities.json
server/server.log
server/avatars/
server/files/
```

These files contain private application data and should not be committed or shared.

## Android Client

The Android source is intentionally kept close to the platform APIs and does not require a third-party UI framework. The application package is `com.chatapp.app`.

The manifest declares:

- Minimum SDK: 24
- Target SDK: 34
- Application version: `1.0.2`

The repository currently does not include a Gradle wrapper or a checked-in APK. Use Android Studio or your preferred Android build setup to import the `android/` source tree, configure the Android SDK, and build the application.

Before installing the client, make sure the server is reachable from the device. Android cleartext traffic is enabled because the current development server uses plain TCP/HTTP on a trusted local network.

## Protocol

The TCP protocol sends one JSON object per line. A client can send messages such as:

```json
{"type":"ping"}
```

The server responds with:

```json
{"type":"pong"}
```

Authentication, contacts, conversations, messages, groups, profiles, and update-related events use the same line-delimited JSON format.

## Testing

The server includes protocol-level scripts under `server/`:

```powershell
node server/test-v210.js
node server/test-v212.js
node server/test-profile.js
```

These scripts create temporary test accounts and data. Run them only against a disposable development data directory or clean the generated runtime files afterward.

For a basic syntax check:

```powershell
node --check server/server.js
node --check server/test-v210.js
node --check server/test-v212.js
node --check server/test-profile.js
```

## Security Notes

This project is intended for development and trusted private-network use.

- Do not expose the server directly to the public internet without adding TLS, authentication hardening, rate limiting, and a proper deployment boundary.
- Do not commit `users.json`, message databases, uploaded files, avatars, logs, APKs, or compiled classes.
- Change the default ports if they conflict with another service.
- Treat the server data directory as sensitive because it may contain account records, profile fields, messages, and uploaded media.

## License

No license has been declared yet. Until a license is added, all rights are reserved by the project owner.
