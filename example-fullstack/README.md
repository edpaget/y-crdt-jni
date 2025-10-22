# YHocuspocus Full-Stack Collaborative Editor Example

A complete full-stack example demonstrating real-time collaborative editing using:

- **Backend**: Java YHocuspocus WebSocket server
- **Frontend**: React + TypeScript + Tiptap editor + Hocuspocus provider

This example shows how to build a collaborative editing application with the YHocuspocus Java server, demonstrating full compatibility with the JavaScript/TypeScript Hocuspocus ecosystem and official Tiptap collaboration extensions.

## Features

- **Real-time Collaboration** - Multiple users can edit the same document simultaneously
- **Rich Text Editing** - Full-featured editor with bold, italic, headings, lists, and more
- **Collaborative Cursors** - See where other users are typing in real-time
- **CRDT-based** - Conflict-free merge of concurrent edits
- **WebSocket Transport** - Low-latency bidirectional communication
- **Persistent Storage** - Documents are saved with debouncing (in-memory by default)
- **Modern Tech Stack** - React 18, TypeScript, Tiptap, Vite

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Browser (Client 1)                  │
│  ┌────────────────────────────────────────────────────┐ │
│  │  React App                                         │ │
│  │  ├─ Tiptap Editor (ProseMirror)                   │ │
│  │  ├─ Tiptap Collaboration Extensions               │ │
│  │  ├─ HocuspocusProvider (@hocuspocus/provider)     │ │
│  │  └─ Yjs (CRDT library)                            │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                          │
                          │ WebSocket (ws://localhost:1234)
                          ▼
┌─────────────────────────────────────────────────────────┐
│              Java YHocuspocus Server                    │
│  ┌────────────────────────────────────────────────────┐ │
│  │  WebSocketServer (Jetty 12)                       │ │
│  │  ├─ YHocuspocus (core server)                     │ │
│  │  ├─ Document lifecycle management                 │ │
│  │  ├─ Sync protocol (Yjs-compatible)                │ │
│  │  ├─ Awareness (user presence)                     │ │
│  │  └─ InMemoryDatabaseExtension (storage)           │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                          │
                          │ WebSocket (ws://localhost:1234)
                          ▼
┌─────────────────────────────────────────────────────────┐
│                     Browser (Client 2)                  │
│  ┌────────────────────────────────────────────────────┐ │
│  │  React App (same as Client 1)                     │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

## Prerequisites

- **Java 21** (for the backend server)
- **Node.js 18+** and **npm** (for the frontend)
- **Gradle** (included via wrapper)

## Quick Start

### 1. Start the Backend Server

From the project root directory:

```bash
# Build the backend
./gradlew :example-fullstack:backend:build

# Run the server
./gradlew :example-fullstack:backend:run
```

You should see:

```
═══════════════════════════════════════════════════════
  YHocuspocus Collaborative Editing Server
═══════════════════════════════════════════════════════

YHocuspocus server initialized
  - Storage: In-Memory
  - Debounce: 2s / Max: 10s

WebSocket server started
  - Address: ws://localhost:1234/
  - Max connections: Unlimited
  - Max message size: 10MB

═══════════════════════════════════════════════════════
Server is ready! Open the frontend to start editing.

Frontend: http://localhost:3000
Press Ctrl+C to stop the server...
═══════════════════════════════════════════════════════
```

The server is now running on **ws://localhost:1234**

### 2. Start the Frontend

In a new terminal, navigate to the frontend directory:

```bash
cd example-fullstack/frontend

# Install dependencies (first time only)
npm install

# Start the development server
npm run dev
```

The frontend will start on **http://localhost:3000**

### 3. Test Collaboration

1. Open **http://localhost:3000** in your browser
2. Enter your name and a document name (e.g., "demo-document")
3. Click **Connect**
4. Start typing in the editor

To see real-time collaboration:

1. Open **http://localhost:3000** in a **second browser window** (or incognito tab)
2. Use a different name but the **same document name**
3. Click **Connect**
4. Type in either window and watch the changes appear instantly in the other!

You'll see:
- **Real-time text updates** as you type
- **Collaborative cursors** showing where other users are typing
- **User names and colors** in the cursor labels

## Project Structure

```
example-fullstack/
├── backend/                           # Java WebSocket server
│   ├── build.gradle                  # Gradle build configuration
│   └── src/main/java/
│       └── net/carcdr/example/
│           └── CollaborativeServerMain.java  # Server entry point
│
├── frontend/                          # React TypeScript frontend
│   ├── package.json                  # NPM dependencies
│   ├── tsconfig.json                 # TypeScript config
│   ├── vite.config.ts                # Vite build config
│   ├── index.html                    # HTML entry point
│   └── src/
│       ├── main.tsx                  # React entry point
│       ├── App.tsx                   # Main app component
│       ├── Editor.tsx                # Collaborative editor component
│       ├── App.css                   # Styles
│       └── index.css                 # Global styles
│
└── README.md                          # This file
```

## How It Works

### Backend (Java)

The backend uses the YHocuspocus WebSocket server:

1. **YHocuspocus Core**: Manages document lifecycle, synchronization, and extensions
2. **WebSocket Transport**: Handles WebSocket connections using Jetty 12
3. **InMemoryDatabaseExtension**: Stores documents in memory (can be replaced with database storage)
4. **Debounced Persistence**: Saves documents after 2 seconds of inactivity, max 10 seconds

Key files:
- `CollaborativeServerMain.java`: Server entry point with configuration

### Frontend (React + TypeScript)

The frontend uses modern web technologies:

1. **React 18**: UI framework
2. **Tiptap**: Rich text editor built on ProseMirror
3. **@hocuspocus/provider**: Official Hocuspocus WebSocket provider
4. **@tiptap/extension-collaboration**: Tiptap's Yjs collaboration extension
5. **@tiptap/extension-collaboration-cursor**: Collaborative cursor extension
6. **Yjs**: CRDT library for conflict-free synchronization (used internally)
7. **Vite**: Fast build tool and dev server

Key files:
- `App.tsx`: Main application with connection UI
- `Editor.tsx`: Collaborative editor component with Tiptap
- `App.css`: Styling for the entire application

### Data Flow

1. **User types** in the Tiptap editor
2. **ProseMirror** generates document changes
3. **Tiptap Collaboration extension** converts changes to Yjs operations
4. **Yjs** encodes changes as CRDT operations
5. **HocuspocusProvider** sends binary updates to the Java server via WebSocket
6. **WebSocketServer** receives and forwards to YHocuspocus
7. **YHocuspocus** applies updates to the YDoc and broadcasts to other clients
8. **Other clients** receive updates via WebSocket through HocuspocusProvider
9. **Yjs** applies updates to their local documents
10. **Tiptap** updates the editor UI in real-time

All of this happens in **real-time** with automatic conflict resolution!

## Customization

### Backend Configuration

Edit `CollaborativeServerMain.java` to customize:

```java
YHocuspocus hocuspocus = YHocuspocus.builder()
    .extension(new InMemoryDatabaseExtension())  // Change storage backend
    .debounce(Duration.ofSeconds(2))             // Adjust save debounce
    .maxDebounce(Duration.ofSeconds(10))         // Adjust max debounce
    .build();

WebSocketServer server = WebSocketServer.builder()
    .server(hocuspocus)
    .port(1234)           // Change port
    .path("/")            // Change WebSocket path
    .build();
```

### Frontend Configuration

Edit `Editor.tsx` to customize:

```typescript
const hocuspocusProvider = new HocuspocusProvider({
  url: 'ws://localhost:1234',  // Change server URL
  name: documentName,          // Document name
  onStatus: ({ status }) => {
    console.log('Connection status:', status)
  },
  onSynced: ({ state }) => {
    console.log('Synced:', state)
  },
  // Add authentication token if needed
  // token: 'your-auth-token',
})
```

### Add Database Storage

Replace `InMemoryDatabaseExtension` with a database extension:

```java
public class PostgresDatabaseExtension extends DatabaseExtension {
    @Override
    protected byte[] loadFromDatabase(String documentName) {
        // Load from PostgreSQL
    }

    @Override
    protected void saveToDatabase(String documentName, byte[] state) {
        // Save to PostgreSQL
    }
}
```

## Troubleshooting

### Backend won't start

- **Check Java version**: `java -version` (need 21)
- **Check port 1234**: Make sure nothing else is using port 1234
- **Build first**: Run `./gradlew :example-fullstack:backend:build`

### Frontend won't start

- **Install dependencies**: `npm install` in `frontend/` directory
- **Check Node version**: `node -v` (need 18+)
- **Clear cache**: Delete `node_modules/` and run `npm install` again

### Can't connect to server

- **Check server is running**: Look for "WebSocket server started" message
- **Check URL**: Frontend should connect to `ws://localhost:1234`
- **Check browser console**: Press F12 to see WebSocket errors
- **Try different port**: If 1234 is blocked, change in both backend and frontend

### Changes not syncing

- **Check WebSocket status**: Look for green "Connected" indicator
- **Check browser console**: Look for Yjs sync errors
- **Try refresh**: Reload both browser windows
- **Check document names match**: Both clients must use the same document name

## Deployment

For production use:

### Backend

1. **Replace InMemoryDatabaseExtension** with persistent storage (PostgreSQL, MongoDB, etc.)
2. **Add authentication** via `onAuthenticate` hook
3. **Add rate limiting** and connection limits
4. **Use environment variables** for configuration
5. **Set up logging** with proper log levels
6. **Deploy behind reverse proxy** (nginx, Apache)
7. **Enable HTTPS** for secure WebSocket (wss://)

### Frontend

1. **Build for production**: `npm run build`
2. **Serve static files** with nginx, Apache, or CDN
3. **Update WebSocket URL** to production server
4. **Enable HTTPS** (required for wss://)
5. **Add error boundaries** for better UX
6. **Implement reconnection logic** for network failures

## Learn More

- **YHocuspocus Docs**: See `/yhocuspocus/README.md`
- **Hocuspocus Provider**: https://tiptap.dev/docs/hocuspocus/provider
- **Tiptap Collaboration**: https://tiptap.dev/docs/editor/api/extensions/collaboration
- **Tiptap Docs**: https://tiptap.dev/
- **Yjs Docs**: https://docs.yjs.dev/
- **WebSocket API**: https://developer.mozilla.org/en-US/docs/Web/API/WebSocket

## License

This example is part of the y-crdt-jni project and follows the same license.

## Support

If you encounter issues:

1. Check this README's troubleshooting section
2. Check browser console for errors (F12)
3. Check server logs for exceptions
4. Open an issue on GitHub with details
