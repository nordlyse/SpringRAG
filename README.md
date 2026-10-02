# SpringRAG

SpringRAG is a retrieval-augmented question system built with Ollama, pgvector, and Spring AI. Stored documents and MCP servers that are explicitly allowed are the intended sources for an answer. The application lives in `spring-rag/`.

## Stack

- **Ollama 0.34.4** runs the local chat model and embeddings.
- **pgvector 0.8.6** on PostgreSQL 17 holds the vector store and the document records.
- **Spring AI 2.0.1** on **Spring Boot 4.1.1** and **Java 25** connects the model, the vector store, and the HTTP API.
- Chat model: `llama3.2`. Embedding model: `nomic-embed-text` (768 dimensions).

## Run

Ollama, pgvector, and the application are defined in `compose.yaml`.

```bash
docker compose up --build
```

The application listens on port `8080`. It talks to Ollama at `http://ollama:11434` and to PostgreSQL at `jdbc:postgresql://pgvector:5432/springrag`. Uploaded files are written to `spring-rag/data`. Pull the models once Ollama is up:

```bash
docker compose exec ollama ollama pull llama3.2
docker compose exec ollama ollama pull nomic-embed-text
```

## Documents

Allowed types are PDF, PNG, JPEG (`.jpeg` and `.jpg`), TXT, DOC, DOCX, XLS, XLSX, PPTX, PPT, and CSV. An empty file or any other extension is rejected with `400`. Each file may be up to 50 MB.

`POST` stores the bytes under `spring-rag/data` and inserts a row in the `documents` table (`file_name`, `media_type`, `size_bytes`). The stored name is the safe base name, a unique id, and the original extension, for example `notes-3f1c2a0e-....txt`. Later calls use that stored name.

| Method | Path | Result |
| --- | --- | --- |
| `GET` | `/documents` | Lists the stored document records. |
| `GET` | `/documents/{fileName}` | Returns one record, or `404` when it is missing. |
| `POST` | `/documents` | Stores one or more files and returns `201`. The multipart part name is `file`. |
| `PUT` | `/documents/{fileName}` | Replaces the file bytes and updates the media type and size. The new file must use the same extension. Missing records return `404`. |
| `DELETE` | `/documents/{fileName}` | Removes the database row and then the file. Success is `204`. A missing record returns `404`. |

Upload a document:

```bash
curl -s -F "file=@notes.txt" http://localhost:8080/documents
```

List stored documents:

```bash
curl -s http://localhost:8080/documents
```

Replace one document. `{fileName}` is the name returned by the upload:

```bash
curl -s -X PUT -F "file=@notes.txt" http://localhost:8080/documents/{fileName}
```

Remove one document:

```bash
curl -s -X DELETE -o /dev/null -w "%{http_code}\n" http://localhost:8080/documents/{fileName}
```

Each record is JSON with `fileName`, `size`, and `mediaType`.

## Questions

Send a question. The reply is a server-sent event stream. `QuestionAnswerAdvisor` reads matching passages from pgvector and adds them to the prompt. `StreamingChatModel` then writes the answer in chunks. A blank message returns `400`.

```bash
curl -N http://localhost:8080/chat \
  -H 'Content-Type: application/json' \
  -H 'Accept: text/event-stream' \
  -d '{"message":"What is in the documents?"}'
```

The intended answer path uses two sources when the question needs them:

1. Passages retrieved from the stored documents.
2. Results returned by MCP servers that have been explicitly allowed. Any other MCP server stays unused.

Document content stays in that path even when an allowed MCP server is called, so a reply is grounded in the user's files and in those permitted tools. MCP calls are not wired into `/chat` yet. Uploaded files are stored on disk and in the `documents` table; they are not embedded into pgvector until a later step.

## Tests

```bash
mvn -f spring-rag/pom.xml test
```

JaCoCo writes the coverage report to `spring-rag/target/site/jacoco/index.html`.
