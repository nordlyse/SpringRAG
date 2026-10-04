export async function addDocuments(files) {
  const body = new FormData()
  for (const file of files) {
    body.append('file', file)
  }
  const response = await fetch('/documents', {
    method: 'POST',
    body,
  })
  if (!response.ok) {
    throw new Error(await failureText(response))
  }
  return response.json()
}

const CONVERSATION_KEY = 'springrag.conversationId'

export async function ingestionNotices() {
  const response = await fetch('/documents/ingestion')
  if (!response.ok) {
    throw new Error(await failureText(response))
  }
  return response.json()
}

export async function ask(message, onChunk) {
  const response = await fetch('/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
    },
    body: JSON.stringify({ message, conversationId: conversationId() }),
  })
  if (!response.ok) {
    throw new Error(await failureText(response))
  }
  await readEventStream(response, onChunk)
}

async function failureText(response) {
  const body = await response.text()
  return body || response.statusText || 'Request failed.'
}

async function readEventStream(response, onChunk) {
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  while (true) {
    const { value, done } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })
    const events = buffer.split('\n\n')
    buffer = events.pop() ?? ''
    for (const event of events) {
      const text = eventText(event)
      if (text) {
        onChunk(text)
      }
    }
  }
  const tail = eventText(buffer)
  if (tail) {
    onChunk(tail)
  }
}

function conversationId() {
  const existing = sessionStorage.getItem(CONVERSATION_KEY)
  if (existing) {
    return existing
  }
  const id = crypto.randomUUID()
  sessionStorage.setItem(CONVERSATION_KEY, id)
  return id
}

function eventText(event) {
  return event
    .split('\n')
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice(5))
    .join('\n')
}
