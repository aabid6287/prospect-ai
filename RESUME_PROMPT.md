# ProspectAI — Session Resume Notes & Handover

## 1. Project Purpose & Current Phase
- **Project Name:** **ProspectAI**
- **Purpose:** Institutional-grade Red Herring Prospectus (RHP) financial research platform. Allows users to upload 400+ page IPO prospectuses (PDFs), track processing status, parse and chunk documents, index vectors in Pinecone, ask grounded questions with verifiable citations, and explore source-backed financial & risk factor analysis panels.
- **Current Phase:** **Phase 5 Completed & Verified**. Next is **Phase 6 — UX, Reliability, and Security Hardening**.

---

## 2. Architecture & Important Technical Decisions
- **Backend:** Java 21, Spring Boot 3.4.5, Maven, Spring Security with stateless JWT (JJWT 0.12.6), Spring Data JPA, Spring AI 1.0.0.
- **AI Integrations & Fallbacks:**
  - `AiConfig.java` uses `OpenAiApi.builder().apiKey(...).build()` and `OpenAiChatModel.builder()`.
  - Offline testing/dev uses `LocalFallbackEmbeddingModel` (token-frequency hash with L2 normalization) and `SimpleVectorStore`, allowing full end-to-end RAG testing without live external API keys.
  - Production uses `OpenAiEmbeddingModel`, `OpenAiChatModel`, and `PineconeVectorStore`.
- **RAG & Citations:**
  - Vector search is strictly document-scoped using Spring AI's `FilterExpressionBuilder.eq("documentId", documentId)`.
  - Citations are synthesized directly from chunk metadata (`pageNumber`, `section`, text excerpt) and not hallucinated by the LLM.
  - If no relevant chunks match, the assistant explicitly states that the prospectus does not contain information to answer the question.
- **Security & Ownership:**
  - Resource authorization validates that the authenticated principal owns the document (`doc.getOwnerId().equals(user.getId())`) on every request.

---

## 3. Features Completed & Verified
- [x] **Phase 0:** Project bootstrap, Maven `pom.xml`, React+Vite, Tailwind CSS, `.env.example`, README, global exceptions.
- [x] **Phase 1:** User registration, login, BCrypt password hashing, JWT generation & validation, security filters, frontend auth context.
- [x] **Phase 2:** PDF upload, validation, `UploadedDocument` entity, file size checks, processing statuses (`UPLOADED`, `PROCESSING`, `READY`, `FAILED`), document deletion.
- [x] **Phase 3:** Spring AI `PagePdfDocumentReader` PDF extraction, `DocumentChunkingService` with metadata preservation, vector ingestion, and test suites.
- [x] **Phase 4:** Core RAG Chat:
  - `ChatMessage` entity & `ChatMessageRepository` with MySQL/H2 index.
  - `ChatService` with document-scoped vector retrieval, prompt grounding, and citation extraction.
  - `ChatController` with `POST /api/chat/{documentId}` and `GET /api/chat/{documentId}/history`.
  - Frontend `chatService.js` and live Q&A in `DocumentWorkspacePage.jsx` with clickable citation pills.
  - Full suite of integration tests in `ChatControllerTest.java`.
- [x] **Phase 5:** RHP Analysis Views:
  - DTOs: `RhpOverviewResponse`, `RiskAnalysisResponse`, `FinancialAnalysisResponse`, `FinancialMetricDto`.
  - `RhpAnalysisService` with document-filtered semantic retrieval, financial unit preservation (INR Crore), and disclaimers.
  - `AnalysisController` with `GET /api/analysis/{documentId}/overview`, `/risks`, `/financials`.
  - Comprehensive integration test suite in `AnalysisControllerTest.java`.
  - Frontend `analysisService.js`, `OverviewPanel.jsx`, `RisksPanel.jsx`, and `FinancialsPanel.jsx`.
  - Tab switcher in `DocumentWorkspacePage.jsx` with interactive citation click-through to document pages.

---

## 4. Key Endpoints & Database Changes
- `POST /api/auth/register` — User registration
- `POST /api/auth/login` — JWT generation
- `POST /api/documents/upload` — Multipart PDF upload
- `GET /api/documents` — User's documents
- `GET /api/documents/{documentId}` — Document metadata
- `DELETE /api/documents/{documentId}` — Delete document
- `POST /api/chat/{documentId}` — Document-grounded RAG query
- `GET /api/chat/{documentId}/history` — Conversation history
- `GET /api/analysis/{documentId}/overview` — Company business model & objects of issue
- `GET /api/analysis/{documentId}/risks` — Internal, external, and litigation risk factor breakdown
- `GET /api/analysis/{documentId}/financials` — Restated financial metrics, EBITDA, and debt structure
- **Tables:** `users`, `uploaded_documents`, `chat_messages`.

---

## 5. Verification Commands & Results
- **Backend Build & Tests:**
  ```bash
  cd prospectai-backend
  mvn clean test
  ```
  *Result:* **BUILD SUCCESS** (Tests run: 33, Failures: 0, Errors: 0, Skipped: 0).
- **Frontend Build & Lint:**
  ```bash
  cd prospectai-frontend
  npm run build
  npm run lint
  ```
  *Result:* **Vite build clean** (`dist/` generated, 0 build errors), **0 lint errors**.

---

## 6. Current Errors & Unresolved Issues
- None. All 33 backend tests and frontend builds pass cleanly.

---

## 7. The Exact Next Task
- **Implement Phase 6 — UX, Reliability, and Security Hardening:**
  1. Polling or WebSocket for real-time document indexing status progression (`UPLOADED` -> `PROCESSING` -> `READY`/`FAILED`).
  2. Rate limiting filter / bucket on `/api/chat` and `/api/analysis`.
  3. Vector store cleanup on document deletion (ensure deleting a document purges corresponding vectors from the vector store).
  4. Curated RAG retrieval quality evaluation test suite (evaluating expected source pages for standard RHP queries).
  5. Accessibility checks (ARIA labels, keyboard focus, high contrast states).

---

## 8. Invariable Constraints
- Product name must remain **ProspectAI**.
- Spring AI is the sole vector/model integration layer; do not add external Python scripts or unvetted libraries.
- Grounding: Never allow ungrounded responses or hallucinated page numbers.
- Security: Always verify document ownership from the security context (`@AuthenticationPrincipal User user`).
