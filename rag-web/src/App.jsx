import { useEffect, useState } from 'react'
import { addDocuments, ask, ingestionNotices } from './api.js'
import { GlassCard } from './components/GlassCard.jsx'
import { PrismLights } from './components/PrismLights.jsx'

const ACCEPT = '.pdf,.png,.jpeg,.jpg,.txt,.doc,.docx,.xls,.xlsx,.pptx,.ppt,.csv'

export function App() {
  const [files, setFiles] = useState([])
  const [stored, setStored] = useState([])
  const [uploadError, setUploadError] = useState('')
  const [uploading, setUploading] = useState(false)
  const [notices, setNotices] = useState([])
  const [message, setMessage] = useState('')
  const [answer, setAnswer] = useState('')
  const [chatError, setChatError] = useState('')
  const [streaming, setStreaming] = useState(false)

  useEffect(() => {
    let stopped = false
    async function refresh() {
      try {
        const next = await ingestionNotices()
        if (!stopped) {
          setNotices(next)
        }
      } catch {
        // Keep the last notice when the API is briefly unavailable.
      }
    }
    refresh()
    const timer = setInterval(refresh, 1000)
    return () => {
      stopped = true
      clearInterval(timer)
    }
  }, [])

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
    <>
      <PrismLights />
      <div className="scrim" aria-hidden="true" />
      <main>
        <header>
          <p className="eyebrow">SpringRAG</p>
          <h1 className="shiny">Documents and questions</h1>
          <p className="lede">
            Upload a file, then ask a question. The page calls the Spring API and shows the reply.
          </p>
          <ul className="pills">
            <li>Streamed answers</li>
            <li>Local model</li>
            <li>Document upload</li>
          </ul>
        </header>

        <div className="grid">
          <GlassCard title="Upload">
            <form onSubmit={onUpload}>
              <label className="drop">
                Files
                <input
                  type="file"
                  name="file"
                  accept={ACCEPT}
                  multiple
                  required
                  onChange={(event) => setFiles(Array.from(event.target.files ?? []))}
                />
                <span className="file-action">Choose files</span>
                <span className="hint">
                  {files.length === 0 ? 'pdf, text, office, or image' : files.map((file) => file.name).join(', ')}
                </span>
              </label>
              <button type="submit" disabled={uploading || files.length === 0}>
                {uploading ? 'Scanning' : 'Upload'}
              </button>
            </form>
            {uploadError ? <p className="error">{uploadError}</p> : null}
            <ul className="notices" aria-live="polite">
              {notices.map((notice) => (
                <li key={notice.fileName} className={notice.state}>
                  {notice.message}
                </li>
              ))}
            </ul>
            <ul className="files">
              {stored.map((document) => (
                <li key={document.fileName}>
                  <strong>{document.fileName}</strong>
                  <span>{document.mediaType}</span>
                  <span>{document.size} bytes</span>
                </li>
              ))}
            </ul>
          </GlassCard>

          <GlassCard title="Question">
            <form onSubmit={onAsk}>
              <label>
                Message
                <textarea
                  name="message"
                  rows="5"
                  required
                  value={message}
                  onChange={(event) => setMessage(event.target.value)}
                  placeholder="Ask about the uploaded documents"
                />
              </label>
              <button type="submit" disabled={streaming || message.trim() === ''}>
                {streaming ? 'Answering' : 'Ask'}
              </button>
            </form>
          </GlassCard>
        </div>

        <GlassCard title="Reply">
          {chatError ? <p className="error">{chatError}</p> : null}
          <article aria-live="polite">
            {answer ? answer : <span className="placeholder">The reply streams here.</span>}
            {streaming ? <span className="caret" /> : null}
          </article>
        </GlassCard>
      </main>
    </>
  )
}
