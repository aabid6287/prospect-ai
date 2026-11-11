# ProspectAI — Implementation Phases

## Delivery Strategy
Build a working vertical slice early: authenticate, upload one PDF, index its chunks, ask a question, and show source pages. Add polish and specialized analysis after the core RAG flow works.

## Phase 0 — Project Bootstrap (Completed)
**Deliverables**
- [x] Create `prospectai-backend` using Spring Boot and Maven.
- [x] Create `prospectai-frontend` using Vite, React, and Tailwind CSS.
- [x] Add the agreed folder structure.
- [x] Configure formatting, `.gitignore`, environment examples, and README.
- [x] Confirm compatible Spring Boot and Spring AI versions and official dependency coordinates.

**Exit criteria**
- [x] Backend starts successfully.
- [x] Frontend starts successfully.
- [x] No real credentials are committed.

## Phase 1 — Database and JWT Authentication (Completed)
**Deliverables**
- [x] MySQL connection and schema.
- [x] User entity, role, repository, registration, login.
- [x] Password hashing and JWT generation/validation.
- [x] Spring Security filter/configuration.
- [x] Frontend login/register forms and auth state.
- [x] Protected routes and authenticated API client.

**Exit criteria**
- [x] Registration/login work.
- [x] Invalid credentials and invalid/expired tokens are rejected.
- [x] Protected endpoints reject unauthenticated requests.
- [x] User identity is derived from the authenticated principal.

## Phase 2 — Document Management (Completed)
**Deliverables**
- [x] `UploadedDocument` entity and repository.
- [x] Upload/list/details/delete APIs.
- [x] File validation and configurable upload limits.
- [x] Processing statuses and safe error reporting.
- [x] React upload component, document list, and status display.

**Exit criteria**
- [x] Only PDFs meeting configured validation are accepted.
- [x] A user sees only their own documents.
- [x] Invalid uploads produce clear errors.
- [x] Status is never shown as ready before indexing succeeds.

## Phase 3 — PDF Reader and Vector Ingestion (Completed)
**Deliverables**
- [x] Integrate Spring AI PDF Document Reader (`PagePdfDocumentReader`).
- [x] Extract content with page metadata.
- [x] Chunking service with configurable chunk size and overlap (`DocumentChunkingService`).
- [x] Metadata attachment and stable vector IDs (`documentId_p{page}_c{index}`).
- [x] OpenAI embedding configuration & deterministic fallback embedding model for tests.
- [x] Pinecone vector store integration & SimpleVectorStore fallback.
- [x] Ingestion failure/retry handling.

**Exit criteria**
- [x] A sample RHP is extracted and chunked.
- [x] Stored vectors are traceable to the document and page.
- [x] Reprocessing does not unintentionally duplicate vectors.
- [x] The document becomes `READY` only after successful ingestion.

## Phase 4 — Core RAG Chat (Completed)
**Deliverables**
- [x] Chat request/response DTOs (`ChatRequest`, `ChatResponse`, `CitationDto`).
- [x] Retrieval filtered strictly by selected document (`documentId` filter expression).
- [x] Grounded system instructions enforcing strict evidence bounds and no hallucinated facts.
- [x] ChatModel integration with local fallback.
- [x] Source citation construction from retrieved metadata (`pageNumber`, `section`, `excerpt`).
- [x] Chat history persistence (`ChatMessage` entity & `ChatMessageRepository`).
- [x] Chat UI with suggested questions, live Q&A, and interactive citation links.

**Exit criteria**
- [x] Questions retrieve only the selected RHP's chunks.
- [x] Answers include real source references from chunk metadata.
- [x] Missing context produces an honest response.
- [x] Conversations are isolated by owner and document.

## Phase 5 — RHP Analysis Views (Completed)
**Deliverables**
- [x] Overview summary (`GET /api/analysis/{documentId}/overview`).
- [x] Risk-factor analysis (`GET /api/analysis/{documentId}/risks`).
- [x] Business model and use-of-proceeds summaries.
- [x] Promoter/shareholding and litigation extraction where available.
- [x] Financial summary with units and fiscal-year context (`GET /api/analysis/{documentId}/financials`).
- [x] Source-backed analysis panels (`OverviewPanel`, `RisksPanel`, `FinancialsPanel`).

**Exit criteria**
- [x] Each supported analysis is grounded in retrieved evidence.
- [x] Missing sections are clearly identified.
- [x] Financial values preserve source units and periods.
- [x] Informational disclaimer is visible.

## Phase 6 — UX, Reliability, and Security Hardening (Completed)
**Deliverables**
- [x] Vector deletion cleanup on document delete (`DocumentService.deleteDocument()` purges VectorStore chunks via metadata filter + deletes chat history via `ChatMessageRepository.deleteByDocumentId()`).
- [x] Correlation ID filter (`CorrelationIdFilter.java`) — `X-Correlation-ID` header sourced or generated, stored in SLF4J MDC, echoed in response. Log pattern updated to include `[%X{correlationId}]`.
- [x] Rate limiting filter (`RateLimitingFilter.java`) — sliding-window (60 req/60s) per client IP on `/api/chat/**` and `/api/analysis/**`, returns HTTP 429 + `ApiResponse.error` + `Retry-After` header.
- [x] Frontend real-time processing status polling (`DashboardPage.jsx`) — 3-second `setInterval` activates while any document is `UPLOADED`/`PROCESSING`, clears automatically on terminal state; pulsing "Indexing…" badge on pending rows.
- [x] Curated RAG quality & security hardening test suite (`RagQualityAndSecurityHardeningTest.java`) — 12 tests covering citation accuracy (Risks p14, Proceeds p88, Financials p210), out-of-domain honesty, cross-tenant vector isolation, correlation ID echo.

**Exit criteria**
- [x] Main user flows work on desktop and mobile.
- [x] Cross-user access tests pass.
- [x] Failure states are understandable and recoverable.
- [x] Citation accuracy is evaluated, not assumed.


## Phase 7 — Deployment
**Deliverables**
- Production frontend and backend configuration.
- HTTPS and explicit CORS origins.
- MySQL deployment and backups.
- Pinecone index configuration.
- Secrets configured in hosting environment.
- Optional Docker images and Compose for local development.
- Deployment and rollback notes.

**Exit criteria**
- Production smoke tests pass.
- No secrets are bundled in frontend assets.
- Health checks, logs, and database backup approach are documented.

## MVP Scope
For the first demonstrable release, complete Phases 0–4. Implement Phase 5 only after document ingestion, retrieval, answer grounding, and citations work reliably.
