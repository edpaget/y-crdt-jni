# Examples

This directory contains example applications demonstrating YHocuspocus collaborative editing.

## Structure

```
examples/
├── frontend/           # Shared React + Tiptap frontend
├── fullstack/          # Standalone Jetty WebSocket backend
│   ├── backend/        # Java server using yhocuspocus-websocket
│   └── e2e/            # Playwright end-to-end tests
└── spring-boot/        # Spring Boot backend
    └── backend/        # Spring Boot server using yhocuspocus-spring-websocket
```

## Quick Start

### Option 1: Standalone Server (Jetty)

```bash
# Terminal 1: Start backend
./gradlew :examples:fullstack:backend:run

# Terminal 2: Start frontend
cd examples/frontend
npm install
npm run dev
```

Open http://localhost:3000

### Option 2: Spring Boot Server

```bash
# Terminal 1: Start backend
./gradlew :examples:spring-boot:backend:bootRun

# Terminal 2: Start frontend
cd examples/frontend
npm install
npm run dev
```

Open http://localhost:3000

## Comparison

| Feature | fullstack | spring-boot |
|---------|-----------|-------------|
| Server | Standalone Jetty | Spring Boot embedded |
| Module | yhocuspocus-websocket | yhocuspocus-spring-websocket |
| Config | Java builder pattern | application.yml |
| Port | 1234 | 1234 |

Both backends are compatible with the same frontend since they implement the same Y.js WebSocket protocol.

## Frontend

The shared frontend is a React application using:
- Tiptap for rich text editing
- Y.js for CRDT-based collaboration
- @hocuspocus/provider for WebSocket connection

See [frontend/](frontend/) for details.
