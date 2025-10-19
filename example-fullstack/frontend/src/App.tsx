import { useState } from 'react'
import CollaborativeEditor from './Editor'
import './App.css'

function App() {
  const [documentName, setDocumentName] = useState('demo-document')
  const [userName, setUserName] = useState(`User-${Math.floor(Math.random() * 1000)}`)
  const [connected, setConnected] = useState(false)

  return (
    <div className="app">
      <header className="header">
        <h1>YHocuspocus Collaborative Editor</h1>
        <p className="subtitle">
          React + Tiptap + Hocuspocus Provider + Java WebSocket Server
        </p>
      </header>

      {!connected ? (
        <div className="connect-form">
          <h2>Connect to Server</h2>
          <div className="form-group">
            <label htmlFor="userName">Your Name:</label>
            <input
              id="userName"
              type="text"
              value={userName}
              onChange={(e) => setUserName(e.target.value)}
              placeholder="Enter your name"
            />
          </div>
          <div className="form-group">
            <label htmlFor="documentName">Document Name:</label>
            <input
              id="documentName"
              type="text"
              value={documentName}
              onChange={(e) => setDocumentName(e.target.value)}
              placeholder="Enter document name"
            />
          </div>
          <button
            className="connect-button"
            onClick={() => setConnected(true)}
            disabled={!userName.trim() || !documentName.trim()}
          >
            Connect
          </button>
        </div>
      ) : (
        <div className="editor-container">
          <div className="editor-header">
            <span className="user-info">Editing as: <strong>{userName}</strong></span>
            <span className="document-info">Document: <strong>{documentName}</strong></span>
            <button
              className="disconnect-button"
              onClick={() => setConnected(false)}
            >
              Disconnect
            </button>
          </div>
          <CollaborativeEditor
            documentName={documentName}
            userName={userName}
            userColor={getUserColor(userName)}
          />
        </div>
      )}

      <footer className="footer">
        <p>
          Open this page in multiple browser windows to see real-time collaboration!
        </p>
        <p className="tech-stack">
          Built with: React • TypeScript • Tiptap • Hocuspocus Provider • Java YHocuspocus Server
        </p>
      </footer>
    </div>
  )
}

// Generate consistent color for user based on their name
function getUserColor(name: string): string {
  const colors = [
    '#958DF1', '#F98181', '#FBBC88', '#FAF594', '#70CFF8',
    '#94FADB', '#B9F18D', '#C3E2C2', '#EAECCC', '#AFC8AD',
  ]
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return colors[Math.abs(hash) % colors.length]
}

export default App
