import { useState, useMemo, useEffect } from 'react'
import { HocuspocusProvider } from '@hocuspocus/provider'
import * as Y from 'yjs'
import CollaborativeEditor from './Editor'
import './App.css'

type ConnectionStatus = 'disconnected' | 'connecting' | 'connected'

function App() {
  const [documentName, setDocumentName] = useState('demo-document')
  const [userName, setUserName] = useState(`User-${Math.floor(Math.random() * 1000)}`)
  const [shouldConnect, setShouldConnect] = useState(false)
  const [status, setStatus] = useState<ConnectionStatus>('disconnected')

  // Create Y.Doc - persists across connection attempts
  const ydoc = useMemo(() => new Y.Doc(), [])

  // Create provider only when user clicks connect
  const provider = useMemo(() => {
    if (!shouldConnect) return null
    return new HocuspocusProvider({
      url: 'ws://localhost:1234',
      name: documentName,
      document: ydoc,
      onStatus: ({ status }) => {
        setStatus(status as ConnectionStatus)
      },
    })
  }, [shouldConnect, documentName, ydoc])

  // Cleanup provider on disconnect or unmount
  useEffect(() => {
    return () => {
      provider?.destroy()
    }
  }, [provider])

  const handleDisconnect = () => {
    provider?.destroy()
    setShouldConnect(false)
    setStatus('disconnected')
  }

  return (
    <div className="app">
      <header className="header">
        <h1>YHocuspocus Collaborative Editor</h1>
        <p className="subtitle">
          React + Tiptap + Hocuspocus Provider + Java WebSocket Server
        </p>
      </header>

      {!shouldConnect ? (
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
            onClick={() => setShouldConnect(true)}
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
              onClick={handleDisconnect}
            >
              Disconnect
            </button>
          </div>
          <CollaborativeEditor
            ydoc={ydoc}
            provider={provider!}
            status={status}
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
