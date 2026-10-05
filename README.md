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

The chat model name defaults to `llama3.2`. The embedding model name defaults to `nomic-embed-text`. Both stay in memory until the application stops.

### Start the web application

The page lives in `rag-web/` and listens on port `5173`. It is a Vite development server. A source change reloads the page in the browser; it does not wait for a production bundle. React, React DOM, Vite, and `@vitejs/plugin-react` are MIT. The glass cards, shiny heading, and WebGL prism background are original source in `rag-web` and add no further runtime dependency.

`docker compose up --build` already starts this page. Open [http://localhost:5173](http://localhost:5173). While a file uploaded from the page is scanned the page says so, and when the scan finishes it says that document is ready to use. That notice clears on the next page load.

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

Send a question. The reply is a server-sent event stream. Matching passages from pgvector are added to the prompt when the search finds any. A question with no matching passages stays as written, and earlier turns in the same conversation stay available. `StreamingChatModel` then writes the answer in chunks. A blank message returns `400`.

Repeat `conversationId` to continue the same conversation. The application keeps the last 20 messages for that id and sends them with the next question. A missing id is used for that request only. The page stores one id in the browser tab. The window is Spring AI in-memory chat memory, which is Apache-2.0, so the stack does not add another store. Restarting the API clears that window.

```bash
curl -N http://localhost:8080/chat \
  -H 'Content-Type: application/json' \
  -H 'Accept: text/event-stream' \
  -d '{"message":"What is in the documents?","conversationId":"demo-1"}'
```

## Chat tools

`/chat` can call two Spring AI tools on `UserDirectory`. Both are marked with `@Tool` and registered on the chat client, so the model calls them when a question is about a user or a role. The answer must come from the tool result. The model is told not to invent a user or a role.

| Tool | What it does |
| --- | --- |
| `findUser` | Finds one test user by user id or by username. Either value is enough. Pass an empty string for the key you do not have. When both are present they must belong to the same user. |
| `rolesForUser` | Returns the roles of that user, again by user id or by username. |

The rows live in the same PostgreSQL database as pgvector. `JdbcUserStore` adds the tables when they are missing:

- `users` stores `id` as the primary key and `username` as a unique key. Each value identifies at most one user.
- `roles` stores `user_id` and `role_name`. `user_id` references `users`. The primary key is `(user_id, role_name)`, so the same role is not stored twice for one user.

On startup the application inserts these test users when they are not already there. Set `spring-rag.seed-test-users` to `false` to skip that step.

| Id | Username | Roles |
| --- | --- | --- |
| `1001` | `ada` | `ADMIN` |
| `1002` | `nora` | `EDITOR`, `VIEWER` |
| `1003` | `milo` | `VIEWER` |

Ask for one of them by id or by username:

```bash
curl -N http://localhost:8080/chat \
  -H 'Content-Type: application/json' \
  -H 'Accept: text/event-stream' \
  -d '{"message":"Which roles does nora have?","conversationId":"demo-1"}'
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

Text is taken from each allowed file, split into token passages, and written to pgvector. A directory listener watches `spring-rag/data`, so a file saved there is scanned the same way as a web upload. PNG and JPEG files are scanned for any text they already contain.

A question searches pgvector first. When a passage matches, the answer uses that passage. When nothing matches, the model answers from its own knowledge.

## Allowed MCP servers

The system may also call MCP servers, but only servers that have been explicitly allowed. Any other MCP server is left unused.

A reply is built from both sources when the question needs them:

1. Passages retrieved from the stored documents.
2. Results returned by the allowed MCP servers.

Document content stays in the answer path even when an allowed MCP server is called, so the reply is grounded in the user's files and in those permitted tools.
