# customer-support-ai-agent

An AI-powered Customer Support Agent that provides accurate and contextual responses to customer queries by leveraging a company knowledge base and FAQ repository.

React chat UI → Spring Boot + Spring AI → OpenAI, with FAQ and knowledge-base embeddings stored in PostgreSQL + pgvector.

## How it works

1. The chat UI sends the customer message to `POST /api/chat`.
2. A semantic search (pgvector, cosine similarity) finds the top-K knowledge chunks. The best similarity is the **confidence score**.
3. The last N messages of the conversation are loaded so follow-up questions keep their context.
4. If confidence is at or above `support.confidence-threshold`, the LLM answers using only the retrieved context (RAG).
5. If confidence is below the threshold, or the LLM replies `NO_ANSWER`, the query is **escalated**: it is stored in the `escalation` table and the customer is told a human agent will follow up.

## Prerequisites

- Java 21 and Maven
- Node.js 18+
- Docker (for PostgreSQL with pgvector)
- An OpenAI API key

## Run

```bash
# 1. Database
docker compose up -d

# 2. Backend (http://localhost:8080) - seeds sample FAQs on first start
export OPENAI_API_KEY=sk-...          # PowerShell: $env:OPENAI_API_KEY="sk-..."
cd backend
mvn spring-boot:run

# 3. Frontend (http://localhost:5173)
cd frontend
npm install
npm run dev
```

## API

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/chat` | `{"conversationId": "...", "message": "..."}` → answer, confidence, escalated flag, sources. Omit `conversationId` to start a new conversation. |
| POST | `/api/knowledge` | `{"title": "...", "content": "..."}` → chunks, embeds and stores an article. |
| GET | `/api/escalations` | Lists escalated queries. |

## Configuration

Settings live in `backend/src/main/resources/application.yml`:

| Property | Default | Meaning |
| --- | --- | --- |
| `support.top-k` | 4 | Knowledge chunks retrieved per query |
| `support.confidence-threshold` | 0.55 | Below this similarity the query is escalated |
| `support.history-size` | 10 | Previous messages sent to the LLM |
| `spring.ai.openai.chat.options.model` | gpt-4o-mini | Chat model |
| `spring.ai.openai.embedding.options.model` | text-embedding-3-small | Embedding model (1536 dimensions) |

Sample FAQs are in `backend/src/main/resources/faqs.json`.

## Project layout

```
backend/    Spring Boot app (chat, knowledge, escalation packages)
frontend/   React + Vite chat interface
docker-compose.yml   PostgreSQL with pgvector
```
