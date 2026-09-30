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

The application listens on port `8080`. It talks to Ollama at `http://ollama:11434` and to pgvector at `jdbc:postgresql://pgvector:5432/springrag`. Uploaded files are written to `spring-rag/data`. The chat model name defaults to `llama3.2`. Pull that model once the Ollama service is up:

```bash
docker compose exec ollama ollama pull llama3.2
```

Upload a document:

```bash
curl -s -F "file=@notes.txt" http://localhost:8080/documents
```

Allowed types are PDF, PNG, JPEG, TXT, DOC, DOCX, XLS, XLSX, PPTX, PPT, and CSV.

Send a question:

```bash
curl -s http://localhost:8080/chat \
  -H 'Content-Type: application/json' \
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

Text is taken from each file, split into passages, embedded, and inserted into pgvector. PNG and JPEG files are read as images so their visible content can be used as well. A question is answered from the passages retrieved for that question.

## Allowed MCP servers

The system may also call MCP servers, but only servers that have been explicitly allowed. Any other MCP server is left unused.

A reply is built from both sources when the question needs them:

1. Passages retrieved from the stored documents.
2. Results returned by the allowed MCP servers.

Document content stays in the answer path even when an allowed MCP server is called, so the reply is grounded in the user's files and in those permitted tools.
