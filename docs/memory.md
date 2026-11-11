# ProspectAI — Project Memory

This file is the concise source of truth for future development sessions. Update it when an agreed architecture decision changes.

## Product
- Name: **ProspectAI**
- Purpose: Upload an RHP (Red Herring Prospectus) PDF and ask document-grounded questions with source-page citations.
- Core capability: RAG-based document chat plus source-backed RHP analysis.
- MVP priority: reliable upload → extraction → chunking → Pinecone indexing → document-scoped chat → citations.

## Agreed Stack
### Backend
- Java
- Spring Boot
- Spring AI
- OpenAI API for chat and embeddings
- Spring AI PDF Document Reader for PDF extraction
- Pinecone as vector database
- MySQL for users, document records, chat history, and processing state
- Spring Security + JWT
- Maven

### Frontend
- React
- Vite
- Tailwind CSS
- React Router recommended
- API calls centralized under `src/services/`

### Optional Infrastructure
- Docker / Docker Compose
- Production hosting to be decided later

## Naming and Packages
- Root directory: `ProspectAI/`
- Backend directory: `prospectai-backend/`
- Frontend directory: `prospectai-frontend/`
- Java package root: `com.prospectai`
- Main class: `ProspectAiApplication.java`
- Pinecone index name in example configuration: `prospectai-index` (actual index configuration must match the embedding model).

## Backend Modules
- `config/`: Spring Security, JWT, CORS, AI, Pinecone.
- `security/`: JWT filter/service and user-details service.
- `auth/`: registration/login and DTOs.
- `user/`: user model, roles, repository.
- `document/`: upload/list/details/delete, status, metadata.
- `ingestion/`: PDF reader, chunking, metadata, vector ingestion.
- `chat/`: retrieval, answer generation, citations, history.
- `analysis/`: overview, financial and risk analysis.
- `exception/`: global exception handling.
- `common/`: shared response and constants.

## Frontend Modules
- `components/common/`
- `components/auth/`
- `components/documents/`
- `components/chat/`
- `components/analysis/`
- `pages/`
- `services/`
- `context/`
- `hooks/`
- `layouts/`
- `utils/`

## Key Design Decisions
1. All provider credentials stay in the backend.
2. Spring AI owns embedding/vector-store/chat integration.
3. Spring AI PDF Document Reader performs PDF extraction; choose and test the appropriate reader implementation.
4. Chunk metadata must preserve `documentId`, `pageNumber` where available, `chunkIndex`, and section where detectable.
5. Pinecone retrieval is always filtered to the requested document.
6. MySQL tracks document ownership and processing status.
7. JWT authentication is not enough by itself: every resource access must also verify owner permissions.
8. Citations are created from retrieved metadata, not invented by the model.
9. If context is insufficient, the assistant must say the answer is not found in the document.
10. Long PDF ingestion should use explicit status tracking and preferably asynchronous processing.
11. Document deletion must clean up vectors; partial cleanup must be recoverable.
12. The application provides research assistance, not guaranteed investment advice.

## Suggested API Endpoints
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/users/me`
- `POST /api/documents/upload`
- `GET /api/documents`
- `GET /api/documents/{documentId}`
- `DELETE /api/documents/{documentId}`
- `POST /api/chat/{documentId}`
- `GET /api/chat/{conversationId}/history`
- `GET /api/analysis/{documentId}/overview`
- `GET /api/analysis/{documentId}/risks`

## Status Values
Suggested document states:
- `UPLOADED`
- `PROCESSING`
- `READY`
- `FAILED`
- `DELETING` (optional cleanup state)

## Current Implementation Status
- **Phases 0–6 Completed & Verified:**
  - Phase 0: Scaffolding, project structure, builds, documentation.
  - Phase 1: MySQL schema, BCrypt, JJWT 0.12.6, authentication endpoints, frontend auth context, protected routes.
  - Phase 2: PDF file validation, `UploadedDocument` entity, upload/list/get/delete APIs, status badges, frontend dashboard.
  - Phase 3: Spring AI `PagePdfDocumentReader`, `DocumentChunkingService`, vector ingestion, fallback local models for test suites.
  - Phase 4: `ChatMessage` entity & repository, `ChatService`, `ChatController` (`POST /api/chat/{documentId}`, `GET /api/chat/{documentId}/history`), citation synthesis from chunk metadata, document-scoped filtering, frontend `chatService.js`, interactive citation highlighting in `DocumentWorkspacePage.jsx`.
  - Phase 5: `RhpAnalysisService`, `AnalysisController` (`/api/analysis/{documentId}/overview`, `/risks`, `/financials`), DTOs (`RhpOverviewResponse`, `RiskAnalysisResponse`, `FinancialAnalysisResponse`, `FinancialMetricDto`), frontend `analysisService.js`, and dedicated UI panels (`OverviewPanel.jsx`, `RisksPanel.jsx`, `FinancialsPanel.jsx`).
  - Phase 6: `CorrelationIdFilter` (MDC correlation ID in all logs + `X-Correlation-ID` response header), `RateLimitingFilter` (sliding-window 60 req/60s on `/api/chat/**` and `/api/analysis/**`, HTTP 429 + `Retry-After`), `DocumentService.deleteDocument()` purges VectorStore chunks and chat history on delete, `DashboardPage.jsx` auto-polls at 3s intervals while documents are `UPLOADED`/`PROCESSING`, `RagQualityAndSecurityHardeningTest.java` (12 new tests: citation accuracy, out-of-domain honesty, cross-tenant isolation, correlation ID headers).
- **Test Suite Status:** 45 backend unit/integration tests passing (`mvn test`). Frontend builds cleanly with zero errors (`npm run build`).
- **Next Step:** Implement **Phase 7 — Deployment** (production config, HTTPS/CORS, MySQL, Pinecone, secrets in env, Docker Compose, deployment notes).

## Open Decisions
- Exact Spring Boot and Spring AI versions.
- Exact Spring AI PDF reader class/module based on chosen release.
- JWT access-token lifetime and whether refresh tokens are required.
- Browser token strategy (memory/session storage versus secure HttpOnly cookies).
- File size/page limits.
- Chunk size, overlap, top-k, and retrieval threshold after evaluation.
- Whether OCR is required for scanned RHPs.
- Deployment provider and production storage/retention policy.
