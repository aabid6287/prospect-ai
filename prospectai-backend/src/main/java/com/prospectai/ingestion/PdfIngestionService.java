package com.prospectai.ingestion;

import com.prospectai.document.DocumentRepository;
import com.prospectai.document.DocumentStatus;
import com.prospectai.document.UploadedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Orchestrator service managing the complete PDF ingestion pipeline:
 * extraction -> chunking -> vector storage -> status transitions.
 */
@Service
public class PdfIngestionService {

    private static final Logger log = LoggerFactory.getLogger(PdfIngestionService.class);

    private final DocumentRepository documentRepository;
    private final PdfReaderService pdfReaderService;
    private final DocumentChunkingService documentChunkingService;
    private final VectorStore vectorStore;

    public PdfIngestionService(DocumentRepository documentRepository,
                               PdfReaderService pdfReaderService,
                               DocumentChunkingService documentChunkingService,
                               VectorStore vectorStore) {
        this.documentRepository = documentRepository;
        this.pdfReaderService = pdfReaderService;
        this.documentChunkingService = documentChunkingService;
        this.vectorStore = vectorStore;
    }

    /**
     * Executes the ingestion pipeline asynchronously so that large RHP uploads
     * do not block HTTP request threads.
     *
     * @param documentId UUID of the document to process
     */
    @Async
    public void ingestDocumentAsync(String documentId) {
        log.info("Starting async ingestion pipeline for documentId={}", documentId);
        ingestDocument(documentId);
    }

    /**
     * Synchronous ingestion pipeline implementation.
     *
     * @param documentId UUID of the document to process
     * @return Processed UploadedDocument instance in READY or FAILED status
     */
    @Transactional
    public UploadedDocument ingestDocument(String documentId) {
        UploadedDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));

        // Step 1: Transition status to PROCESSING
        doc.setStatus(DocumentStatus.PROCESSING);
        doc.setFailureReason(null);
        doc = documentRepository.save(doc);
        log.info("Document status set to PROCESSING for documentId={}", documentId);

        try {
            if (doc.getStoragePath() == null) {
                throw new IllegalStateException("Document storage path is missing or invalid.");
            }

            Path pdfPath = Paths.get(doc.getStoragePath());

            // Step 2: Read PDF with Spring AI PDF Document Reader
            List<Document> pageDocuments = pdfReaderService.readPdf(pdfPath);
            int pageCount = pageDocuments.size();
            doc.setPageCount(pageCount);

            // Step 3: Chunk pages with TokenTextSplitter and attach metadata
            List<Document> chunks = documentChunkingService.chunkDocuments(pageDocuments, doc.getId(), doc.getOwnerId());

            if (chunks.isEmpty()) {
                throw new IllegalStateException("Extraction produced no indexable text chunks.");
            }

            // Step 4: Index chunks into VectorStore (Pinecone / Fallback)
            log.info("Indexing {} chunks into VectorStore for documentId={}", chunks.size(), documentId);
            vectorStore.add(chunks);

            // Step 5: Transition to READY only after successful vector indexing
            doc.setStatus(DocumentStatus.READY);
            doc.setFailureReason(null);
            UploadedDocument readyDoc = documentRepository.save(doc);

            log.info("Document successfully indexed into VectorStore: id={}, pages={}, chunks={}, status=READY",
                    documentId, pageCount, chunks.size());

            return readyDoc;

        } catch (Exception e) {
            log.error("Ingestion failed for documentId={}: {}", documentId, e.getMessage(), e);
            doc.setStatus(DocumentStatus.FAILED);
            String safeError = e.getMessage() != null ? e.getMessage() : "Document extraction or vector indexing failed.";
            doc.setFailureReason(safeError.length() > 500 ? safeError.substring(0, 500) : safeError);
            return documentRepository.save(doc);
        }
    }
}
