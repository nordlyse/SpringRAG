# SpringRAG

SpringRAG answers user questions from stored documents and from MCP servers that are explicitly allowed. The stack is Ollama, pgvector, and Spring AI.

## Stack

- **Ollama 0.34.4** runs the local language model and embeddings.
- **pgvector 0.8.6** on PostgreSQL 17 stores document embeddings.
- **Spring AI 2.0.1** on **Spring Boot 4.1.1** and **Java 25** ties the model, the vector store, and the question flow together. That application lives in `spring-rag/`.

## Run

Ollama and the `spring-rag` application are defined in `compose.yaml`. The Spring AI project is not kept at the repository root.

```bash
docker compose up --build
```

The application listens on port `8080`. It talks to Ollama at `http://ollama:11434` and to pgvector at `jdbc:postgresql://pgvector:5432/springrag`. Uploaded files are written to `spring-rag/data`. The chat model name defaults to `llama3.2`. The embedding model name defaults to `nomic-embed-text`. Pull both once Ollama is up:

```bash
docker compose exec ollama ollama pull llama3.2
docker compose exec ollama ollama pull nomic-embed-text
```

The page listens on port `5173`. `docker compose up --build` starts it. Open [http://localhost:5173](http://localhost:5173). While a file is scanned the page says so, and when the scan finishes it says that document is ready to use.

Upload a document:

```bash
curl -s -F "file=@notes.txt" http://localhost:8080/documents
```

Allowed types are PDF, PNG, JPEG, TXT, DOC, DOCX, XLS, XLSX, PPTX, PPT, and CSV.

Send a question. The reply is a stream, so words arrive while the model is still writing:

```bash
curl -N http://localhost:8080/chat \
  -H 'Content-Type: application/json' \
  -H 'Accept: text/event-stream' \
  -d '{"message":"What is in the documents?"}'
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
