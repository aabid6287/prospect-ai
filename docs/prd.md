# ProspectAI — Product Requirements Document

## 1. Product Overview
**Product name:** ProspectAI  
**Product type:** AI-powered RHP (Red Herring Prospectus) analysis and document-chat platform.

ProspectAI allows an authenticated user to upload an RHP PDF, process it into searchable chunks, and ask questions answered from the selected document using Retrieval-Augmented Generation (RAG).

## 2. Problem Statement
RHP documents are lengthy and often contain dense legal language, financial tables, promoter details, risk disclosures, and IPO-use-of-proceeds information. Manually finding relevant information can be slow. ProspectAI provides a document-grounded interface for exploring an RHP and locating supporting pages.

## 3. Goals
- Let users register, log in, and access their own documents securely.
- Upload and process PDF RHPs.
- Extract PDF content with Spring AI PDF Document Reader.
- Split content into chunks and preserve useful metadata, especially page references.
- Generate embeddings through the configured OpenAI embedding model and store vectors in Pinecone.
- Answer questions using retrieved chunks from the selected RHP.
- Display source-page citations and relevant excerpts.
- Offer structured RHP analysis views for risk factors, financial information, promoter details, and use of proceeds when supported by the document.
- Keep processing state, document metadata, and chat history in MySQL.

## 4. Non-Goals for the MVP
- Investment recommendations, buy/sell calls, or guaranteed IPO outcomes.
- Replacing professional legal, accounting, or investment advice.
- Automatically validating every financial statement against external filings.
- Multi-document cross-company comparison unless added in a later phase.
- OCR support for scanned PDFs unless explicitly included in the implementation scope.

## 5. Target Users
- Retail investors researching IPOs.
- Analysts and finance learners reviewing RHPs.
- Researchers who need to locate disclosures and source pages quickly.

## 6. Core User Journeys
### Journey A — Authentication
1. User registers with required details.
2. Backend validates input and stores a securely hashed password.
3. User logs in and receives an authentication response.
4. Authenticated requests are protected by JWT-based Spring Security.

### Journey B — Upload RHP
1. User selects a PDF file.
2. Frontend validates basic file constraints and submits multipart form data.
3. Backend validates file type, size, readability, and ownership.
4. Backend creates a document record with a processing status.
5. PDF reader extracts content while preserving page metadata where supported.
6. Chunking service creates overlapping chunks.
7. Spring AI generates embeddings and stores vectors in Pinecone with document and page metadata.
8. Document status becomes `READY`, or `FAILED` with a safe error message.

### Journey C — Ask Questions
1. User opens a document in their own library.
2. User submits a question.
3. Backend validates ownership and filters vector retrieval by the selected `documentId`.
4. Retrieved chunks are passed to the LLM with grounding instructions.
5. Backend returns an answer plus source citations derived from retrieved metadata.
6. If context is insufficient, the assistant clearly says the answer was not found in the uploaded document.

### Journey D — Delete Document
1. User requests deletion of their document.
2. Backend verifies ownership.
3. Backend deletes associated Pinecone vectors and relevant application records according to the deletion policy.
4. UI confirms deletion only after the backend reports success.

## 7. Functional Requirements
### Authentication
- Register and log in.
- Hash passwords with a suitable password encoder such as BCrypt.
- Validate JWT signatures, expiry, and claims.
- Protect document, chat, and analysis APIs.
- Enforce authorization at the resource level; a valid JWT must not grant access to another user's document.

### Document Management
- Upload PDF RHPs.
- List only documents owned by the authenticated user.
- View document processing status and metadata.
- Delete owned documents and their vectors.
- Reject unsupported, empty, oversized, or unreadable files.
- Avoid processing duplicate submissions unintentionally.

### Ingestion and Retrieval
- Use Spring AI PDF Document Reader.
- Chunk text with configurable chunk size and overlap.
- Preserve `documentId`, `ownerId` where appropriate, `pageNumber`, `chunkIndex`, and section metadata when available.
- Store embeddings in Pinecone using a compatible index dimension and metric.
- Query Pinecone with a mandatory document filter.
- Tune retrieval count and chunk parameters using evaluation questions.

### RAG Chat
- Answer only from retrieved context for document-specific factual questions.
- Include source page numbers and excerpts when available.
- Do not invent citations, financial figures, or missing facts.
- Support chat history per conversation.
- Handle empty retrieval and model/provider failures gracefully.

### RHP Analysis
- Provide document-grounded summaries of risk factors, business overview, financials, promoter/shareholding details, litigation, and use of proceeds when the relevant information is present.
- Distinguish facts extracted from the RHP from calculations or interpretation.
- Include page citations where available.
- Display a disclaimer that the output is informational, not investment advice.

## 8. Non-Functional Requirements
- Security: secrets stay server-side; authorization checks on every resource.
- Reliability: status tracking, safe retries, and clear failure states.
- Performance: long PDF processing should not block a request indefinitely; use asynchronous processing when practical.
- Maintainability: layered modules, DTOs, validation, centralized exception handling.
- Observability: structured logs without passwords, tokens, API keys, or unnecessary sensitive document content.
- Accessibility: keyboard-operable controls, visible focus, readable contrast, labels, and responsive layout.
- Data integrity: prevent orphaned vectors and clearly handle partial failures.
- Privacy: define retention and deletion behavior for files, vectors, and chat history.

## 9. Suggested API Surface
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

All endpoints except registration/login should require authentication unless explicitly documented otherwise.

## 10. Acceptance Criteria
- A user can register and log in.
- Unauthenticated requests to protected endpoints are rejected.
- User A cannot list, open, chat with, analyze, or delete User B's documents.
- A valid PDF can be uploaded and progresses to `READY`.
- Extracted chunks stored in Pinecone include a stable document identifier and page metadata when available.
- Asking a question retrieves only chunks for the selected RHP.
- Responses include source references grounded in retrieved chunks.
- Unsupported questions receive a clear “not found in this document” response rather than invented information.
- Failed processing is visible and does not falsely report success.
- Deleting a document removes its vectors or records a recoverable cleanup failure.

## 11. Risks and Mitigations
- **PDF tables extract poorly:** test representative RHPs; consider table-aware extraction/OCR in a later phase.
- **Hallucinated answers:** use strict prompts, citations, retrieval thresholds, and evaluation tests.
- **Cross-document leakage:** mandatory vector filters and backend ownership checks.
- **Large PDFs:** file limits, asynchronous ingestion, and progress states.
- **Financial interpretation errors:** preserve units and periods; avoid presenting unsupported calculations as facts.

## 12. Success Metrics
- Upload-to-ready success rate.
- Percentage of evaluated questions with correct supporting citations.
- Retrieval relevance on a curated RHP question set.
- Median time to process an RHP and answer a question.
- Rate of authorization and deletion tests passing.
