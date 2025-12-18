import { useEditor, EditorContent } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import Collaboration from '@tiptap/extension-collaboration'
import CollaborationCaret from '@tiptap/extension-collaboration-caret'
import { HocuspocusProvider } from '@hocuspocus/provider'
import * as Y from 'yjs'

interface EditorProps {
  ydoc: Y.Doc
  provider: HocuspocusProvider
  status: 'connecting' | 'connected' | 'disconnected'
  userName: string
  userColor: string
}

const CollaborativeEditor = ({ ydoc, provider, status, userName, userColor }: EditorProps) => {
  // Create editor only after provider is connected
  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        undoRedo: false, // Disable default history, use Yjs history
      }),
      Collaboration.configure({
        document: ydoc,
      }),
      CollaborationCaret.configure({
        provider: provider,
        user: {
          name: userName,
          color: userColor,
        },
      }),
    ],
  }, [ydoc, provider, status === 'connected'])

  const statusIndicator = (
    <div className={`status-indicator status-${status}`}>
      {status === 'connected' && 'Connected'}
      {status === 'connecting' && 'Connecting...'}
      {status === 'disconnected' && 'Disconnected'}
    </div>
  )

  if (status !== 'connected' || !editor) {
    return (
      <div className="editor-wrapper">
        <div className="editor-toolbar">
          {statusIndicator}
        </div>
        <div className="editor-loading">
          {status === 'connecting' && 'Connecting to server...'}
          {status === 'disconnected' && 'Disconnected. Reconnecting...'}
          {status === 'connected' && 'Loading editor...'}
        </div>
      </div>
    )
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
        {statusIndicator}
      </div>
      <EditorContent editor={editor} className="editor-content" />
    </div>
  )
}

export default CollaborativeEditor
