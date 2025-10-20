import { useEffect, useState, useMemo } from 'react'
import { useEditor, EditorContent } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import Collaboration from '@tiptap/extension-collaboration'
import CollaborationCursor from '@tiptap/extension-collaboration-cursor'
import { HocuspocusProvider } from '@hocuspocus/provider'

interface EditorProps {
  documentName: string
  userName: string
  userColor: string
}

const CollaborativeEditor = ({ documentName, userName, userColor }: EditorProps) => {
  const [status, setStatus] = useState<'connecting' | 'connected' | 'disconnected'>('connecting')

  // Create provider first (memoized so it doesn't recreate on every render)
  const provider = useMemo(() => {
    return new HocuspocusProvider({
      url: 'ws://localhost:1234',
      name: documentName,
      onStatus: ({ status }) => {
        console.log('Hocuspocus status changed:', status)
        setStatus(status as 'connecting' | 'connected' | 'disconnected')
      },
      onSynced: ({ state }) => {
        console.log('Document synced, state:', state)
      },
      onConnect: () => {
        console.log('WebSocket connected!')
      },
      onDisconnect: ({ event }) => {
        console.log('WebSocket disconnected:', event)
      },
      onClose: ({ event }) => {
        console.log('Connection closed:', event)
      },
      onOpen: () => {
        console.log('WebSocket opened!')
      },
    })
  }, [documentName])

  // Create editor with the provider's document
  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        history: false, // Disable default history, use Yjs history
      }),
      Collaboration.configure({
        document: provider.document,
      }),
      CollaborationCursor.configure({
        provider: provider,
        user: {
          name: userName,
          color: userColor,
        },
      }),
    ],
  }, [provider])

  if (!editor) {
    return <div className="editor-loading">Loading editor...</div>
  }

  return (
    <div className="editor-wrapper">
      <div className="editor-toolbar">
        <button
          onClick={() => editor.chain().focus().toggleBold().run()}
          className={editor.isActive('bold') ? 'is-active' : ''}
          title="Bold"
        >
          <strong>B</strong>
        </button>
        <button
          onClick={() => editor.chain().focus().toggleItalic().run()}
          className={editor.isActive('italic') ? 'is-active' : ''}
          title="Italic"
        >
          <em>I</em>
        </button>
        <button
          onClick={() => editor.chain().focus().toggleHeading({ level: 1 }).run()}
          className={editor.isActive('heading', { level: 1 }) ? 'is-active' : ''}
          title="Heading 1"
        >
          H1
        </button>
        <button
          onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}
          className={editor.isActive('heading', { level: 2 }) ? 'is-active' : ''}
          title="Heading 2"
        >
          H2
        </button>
        <button
          onClick={() => editor.chain().focus().toggleBulletList().run()}
          className={editor.isActive('bulletList') ? 'is-active' : ''}
          title="Bullet List"
        >
          • List
        </button>
        <button
          onClick={() => editor.chain().focus().toggleOrderedList().run()}
          className={editor.isActive('orderedList') ? 'is-active' : ''}
          title="Ordered List"
        >
          1. List
        </button>
        <div className="toolbar-separator"></div>
        <div className={`status-indicator status-${status}`}>
          {status === 'connected' && '🟢 Connected'}
          {status === 'connecting' && '🟡 Connecting...'}
          {status === 'disconnected' && '🔴 Disconnected'}
        </div>
      </div>
      <EditorContent editor={editor} className="editor-content" />
    </div>
  )
}

export default CollaborativeEditor
