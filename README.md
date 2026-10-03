# SpringRAG

SpringRAG answers user questions from stored documents and from MCP servers that are explicitly allowed. The stack is Ollama, pgvector, and Spring AI.

## Stack

- **Ollama 0.34.4** runs the local language model and embeddings.
- **pgvector 0.8.6** on PostgreSQL 17 stores document embeddings.
- **Spring AI 2.0.1** on **Spring Boot 4.1.1** and **Java 25** ties the model, the vector store, and the question flow together. That application lives in `spring-rag/`.

## Run

Ollama, pgvector, `spring-rag`, and the web app are defined in `compose.yaml`. The Spring AI project is not kept at the repository root.

Start the stack:

```bash
docker compose up --build
```

The API listens on port `8080`. Inside Compose it talks to Ollama at `http://ollama:11434` and to pgvector at `jdbc:postgresql://pgvector:5432/springrag`. Uploaded files are written to `spring-rag/data`. Pull the chat and embedding models once Ollama is up:

```bash
docker compose exec ollama ollama pull llama3.2
docker compose exec ollama ollama pull nomic-embed-text
```

The chat model name defaults to `llama3.2`. The embedding model name defaults to `nomic-embed-text`.

### Start the web application

The page lives in `rag-web/` and listens on port `5173`. It is a Vite development server. A source change reloads the page in the browser; it does not wait for a production bundle. React, React DOM, Vite, and `@vitejs/plugin-react` are MIT. The glass cards, shiny heading, and WebGL prism background are original source in `rag-web` and add no further runtime dependency.

`docker compose up --build` already starts this page. Open [http://localhost:5173](http://localhost:5173).

To start only the page on the host, leave the API listening on port `8080`, then run:

```bash
cd rag-web
npm install
npm run dev
```

Node.js 20 or newer is enough for this Vite version. The dev server prints a local URL, normally [http://localhost:5173](http://localhost:5173). Vite forwards `POST /documents` and `POST /chat` to `http://localhost:8080`, or to `SPRING_RAG_API` when that variable is set. In Compose that variable is `http://spring-rag:8080`.

On the page, choose a file and upload it, then type a question. The reply streams into the page.

Upload a document:

```bash
curl -s -F "file=@notes.txt" http://localhost:8080/documents
```

Allowed types are PDF, PNG, JPEG, TXT, DOC, DOCX, XLS, XLSX, PPTX, PPT, and CSV.

Send a question. The reply is a server-sent event stream. `QuestionAnswerAdvisor` reads matching passages from pgvector and adds them to the prompt. `StreamingChatModel` then writes the answer in chunks. A blank message returns `400`.

Repeat `conversationId` to continue the same conversation. The application keeps the last 20 messages for that id and sends them with the next question. A missing id is used for that request only. The page stores one id in the browser tab. The window is Spring AI in-memory chat memory, which is Apache-2.0, so the stack does not add another store.

```bash
curl -N http://localhost:8080/chat \
  -H 'Content-Type: application/json' \
  -H 'Accept: text/event-stream' \
  -d '{"message":"What is in the documents?","conversationId":"demo-1"}'
```

## Documents

The system reads the following file types and uses their content when answering:

- PDF
- PNG
- JPEG
- TXT
- DOC
- DOCX
- XLS
- XLSX
- PPTX
- PPT
- CSV

Text is taken from each file, split into passages, embedded, and inserted into pgvector. PNG and JPEG files are read as images so their visible content can be used as well. A question is answered from the passages retrieved for that question.

## Allowed MCP servers

The system may also call MCP servers, but only servers that have been explicitly allowed. Any other MCP server is left unused.

A reply is built from both sources when the question needs them:

1. Passages retrieved from the stored documents.
2. Results returned by the allowed MCP servers.

Document content stays in the answer path even when an allowed MCP server is called, so the reply is grounded in the user's files and in those permitted tools.
