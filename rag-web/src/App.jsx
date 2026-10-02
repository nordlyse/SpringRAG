import { useState } from 'react'
import { addDocuments, ask } from './api.js'

const ACCEPT = '.pdf,.png,.jpeg,.jpg,.txt,.doc,.docx,.xls,.xlsx,.pptx,.ppt,.csv'

export function App() {
  const [files, setFiles] = useState([])
  const [stored, setStored] = useState([])
  const [uploadError, setUploadError] = useState('')
  const [uploading, setUploading] = useState(false)
  const [message, setMessage] = useState('')
  const [answer, setAnswer] = useState('')
  const [chatError, setChatError] = useState('')
  const [streaming, setStreaming] = useState(false)

  async function onUpload(event) {
    event.preventDefault()
    setUploadError('')
    setUploading(true)
    try {
      const added = await addDocuments(files)
      setStored((current) => [...added, ...current])
      setFiles([])
      event.target.reset()
    } catch (error) {
      setUploadError(error.message)
    } finally {
      setUploading(false)
    }
  }

  async function onAsk(event) {
    event.preventDefault()
    setChatError('')
    setAnswer('')
    setStreaming(true)
    try {
      await ask(message, (chunk) => {
        setAnswer((current) => current + chunk)
      })
    } catch (error) {
      setChatError(error.message)
    } finally {
      setStreaming(false)
    }
  }

  return (
    <main>
      <header>
        <p className="mark">SpringRAG</p>
        <h1>Documents and questions</h1>
        <p className="lede">
          Upload a file, then ask a question. The page calls the Spring API and shows the streamed reply.
        </p>
      </header>

      <section>
        <h2>Upload</h2>
        <form onSubmit={onUpload}>
          <label>
            Files
            <input
              type="file"
              name="file"
              accept={ACCEPT}
              multiple
              required
              onChange={(event) => setFiles(Array.from(event.target.files ?? []))}
            />
          </label>
          <button type="submit" disabled={uploading || files.length === 0}>
            {uploading ? 'Uploading' : 'Upload'}
          </button>
        </form>
        {uploadError ? <p className="error">{uploadError}</p> : null}
        <ul>
          {stored.map((document) => (
            <li key={document.fileName}>
              <strong>{document.fileName}</strong>
              <span>{document.mediaType}</span>
              <span>{document.size} bytes</span>
            </li>
          ))}
        </ul>
      </section>

      <section>
        <h2>Question</h2>
        <form onSubmit={onAsk}>
          <label>
            Message
            <textarea
              name="message"
              rows="4"
              required
              value={message}
              onChange={(event) => setMessage(event.target.value)}
            />
          </label>
          <button type="submit" disabled={streaming || message.trim() === ''}>
            {streaming ? 'Answering' : 'Ask'}
          </button>
        </form>
        {chatError ? <p className="error">{chatError}</p> : null}
        <article aria-live="polite">{answer}</article>
      </section>
    </main>
  )
}
