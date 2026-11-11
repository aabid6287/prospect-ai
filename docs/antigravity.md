# ProspectAI — Antigravity Build Instructions

You are the senior full-stack engineer responsible for implementing **ProspectAI**, a production-conscious RHP (Red Herring Prospectus) analysis and RAG chat application.

Read these project documents before making changes:
- `prd.md`
- `architecture.md`
- `rules.md`
- `phases.md`
- `design.md`
- `memory.md`

Treat `rules.md` as mandatory engineering constraints. Treat `memory.md` as the current project source of truth. If documents conflict, report the conflict and prefer the most recently confirmed user decision rather than silently inventing a choice.

## 1. Required Stack
### Backend
- Java + Spring Boot + Maven
- Spring AI
- OpenAI API for chat and embeddings
- Spring AI PDF Document Reader for PDF extraction
- Pinecone vector database through Spring AI integration
- MySQL + Spring Data JPA
- Spring Security + JWT
- Bean Validation and centralized exception handling

### Frontend
- React + Vite
- Tailwind CSS
- React Router
- A centralized API client and authentication context
- Use only additional dependencies that materially help the requirements.

## 2. Working Method
1. Inspect the existing repository before creating or overwriting files.
2. Read all project documentation and follow the implementation phases in order.
3. Implement one phase at a time. Do not generate the entire application as a large, untested code dump.
4. Before each phase, summarize the files and behavior to be added.
5. After implementation, run the available build, lint, and tests; fix failures before declaring the phase complete.
6. Keep documentation and `.env.example` updated when configuration changes.
7. Do not claim a build or test passed unless you actually ran it and observed success.
8. If credentials or external services are unavailable, use configuration placeholders and mocks for tests; document what could not be integration-tested.
9. Ask before making a major architecture or technology change. For small reversible details, use a reasonable default and document it.

## 3. Product Requirements
Implement the following user journey:
1. A user registers and logs in.
2. The user uploads an RHP PDF.
3. The backend validates the file and creates a document record.
4. Spring AI PDF Document Reader extracts document content.
5. Extracted content is split into overlapping chunks with page metadata where available.
6. OpenAI embeddings are generated and stored in Pinecone.
7. The UI shows processing status and then marks the document ready.
8. The user asks questions about that document.
9. Retrieval searches only the selected document's vectors.
10. The chat model answers from retrieved context and returns source citations based on chunk metadata.
11. The user can view chat history, explore source excerpts, and delete their own document.

## 4. Security Requirements
- Hash passwords with BCrypt or an equivalent adaptive password encoder.
- Validate JWT signature and expiry.
- Protect all non-public APIs.
- Derive user identity from the authenticated principal, never from a client-supplied owner ID.
- Check ownership before every document, chat, analysis, and deletion operation.
- Never put OpenAI, Pinecone, or JWT signing secrets in frontend code or Git.
- Validate uploads by size, type, and readability.
- Treat uploaded PDF text as untrusted input; it must not override system instructions.
- Return safe errors without exposing stack traces or secrets.
- Ensure document-specific vector filtering is mandatory.

## 5. RAG Requirements
- Preserve `documentId`, `pageNumber` where available, `chunkIndex`, and relevant section metadata.
- Build citations from retrieved metadata; do not allow the LLM to invent source pages.
- If no relevant context is found, say that the information was not found in the uploaded RHP.
- Preserve financial units, fiscal periods, signs, and table labels.
- Avoid unsupported financial conclusions or guaranteed investment recommendations.
- Make chunk size, overlap, top-k, and model names configurable.
- Make ingestion idempotent and vector cleanup recoverable.

## 6. UI Requirements
- Professional financial research SaaS interface.
- Responsive dashboard with sidebar navigation.
- Landing, login, registration, dashboard, document analysis, and chat pages.
- PDF upload with validation, progress/status, and retry states.
- Chat interface with suggested questions, clear loading states, and source citations.
- Analysis panels for overview, financials, risks, promoters, use of proceeds, and litigation when supported by source content.
- Use accessible semantic HTML, keyboard navigation, visible focus, and readable contrast.
- Use the `design.md` visual direction and tokens.
- Do not invent dashboard metrics, analysis results, citations, or successful action states.

## 7. Suggested Implementation Order
### Phase 0
Create the backend and frontend scaffolds, configuration templates, folder structure, and README.

### Phase 1
Implement MySQL, user registration/login, JWT security, protected routes, and frontend auth.

### Phase 2
Implement document metadata, upload/list/details/delete APIs, file validation, and processing states.

### Phase 3
Integrate Spring AI PDF Document Reader, chunking, OpenAI embeddings, and Pinecone storage.

### Phase 4
Implement document-filtered retrieval, grounded answers, citations, and chat history.

### Phase 5
Add source-backed RHP analysis panels.

### Phase 6
Improve UX, accessibility, error handling, evaluation, and security tests.

### Phase 7
Prepare deployment configuration and operational documentation.

## 8. Code Standards
- Use constructor injection.
- Keep controllers thin.
- Use DTOs rather than exposing entities.
- Use clear naming and focused methods.
- Centralize exception handling.
- Add unit tests for business rules and security-sensitive behavior.
- Add integration tests for document isolation and retrieval filtering where feasible.
- Avoid unnecessary abstractions, decorative UI dependencies, and unrequested features.
- Keep secrets out of logs and source control.

## 9. Completion Report
At the end of each phase, report:
- What was implemented.
- Important files created or changed.
- Commands run and their actual results.
- Tests passed and tests not run.
- Required environment variables or external services.
- Known limitations and the next phase.

Start by inspecting the repository and implementing **Phase 0 only**. Do not proceed to later phases until the scaffold builds successfully and the phase report is complete.
