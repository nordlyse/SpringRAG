# SpringRAG

SpringRAG answers user questions from stored documents and from MCP servers that are explicitly allowed. The stack is Ollama, pgvector, and Spring AI.

## Stack

- **Ollama** runs the local language model and embeddings.
- **pgvector** stores document embeddings in PostgreSQL.
- **Spring AI** ties the model, the vector store, and the question flow together.

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
