package com.prospectai.document;

import com.prospectai.chat.ChatMessageRepository;
import com.prospectai.document.dto.DocumentResponse;
import com.prospectai.document.dto.UploadDocumentResponse;
import com.prospectai.exception.AccessDeniedException;
import com.prospectai.exception.ResourceNotFoundException;
import com.prospectai.ingestion.PdfIngestionService;
import com.prospectai.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final byte[] PDF_MAGIC_BYTES = new byte[]{0x25, 0x50, 0x44, 0x46}; // "%PDF"

    private final DocumentRepository documentRepository;
    private final PdfIngestionService pdfIngestionService;
    private final VectorStore vectorStore;
    private final ChatMessageRepository chatMessageRepository;
    private final Path uploadDir;

    @Value("${app.ingestion.max-file-size-mb:50}")
    private long maxFileSizeMb;

    public DocumentService(DocumentRepository documentRepository,
                           PdfIngestionService pdfIngestionService,
                           VectorStore vectorStore,
                           ChatMessageRepository chatMessageRepository,
                           @Value("${app.upload.dir:uploads/documents}") String uploadDirStr) {
        this.documentRepository = documentRepository;
        this.pdfIngestionService = pdfIngestionService;
        this.vectorStore = vectorStore;
        this.chatMessageRepository = chatMessageRepository;
        this.uploadDir = Paths.get(uploadDirStr).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            log.error("Failed to initialize upload directory: {}", this.uploadDir, e);
            throw new IllegalStateException("Could not initialize upload storage location", e);
        }
    }

    @Transactional
    public UploadDocumentResponse uploadDocument(User user, MultipartFile file, String requestedCompanyName) {
        validateFile(file);

        String documentId = UUID.randomUUID().toString();
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf";
        String cleanCompanyName = deriveCompanyName(originalFilename, requestedCompanyName);

        // Save physical file
        Path targetPath = this.uploadDir.resolve(documentId + ".pdf");
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to store uploaded document file: {}", originalFilename, e);
            throw new IllegalStateException("Failed to store uploaded file securely.", e);
        }

        // Persist database entity
        UploadedDocument doc = new UploadedDocument();
        doc.setId(documentId);
        doc.setOwnerId(user.getId());
        doc.setOriginalFilename(originalFilename);
        doc.setCompanyName(cleanCompanyName);
        doc.setStatus(DocumentStatus.UPLOADED);
        doc.setFileSize(file.getSize());
        doc.setContentType("application/pdf");
        doc.setStoragePath(targetPath.toString());

        UploadedDocument saved = documentRepository.save(doc);
        log.info("Document uploaded successfully: id={}, filename={}, ownerId={}", saved.getId(), originalFilename, user.getId());

        // Trigger async ingestion pipeline immediately
        pdfIngestionService.ingestDocumentAsync(saved.getId());

        return new UploadDocumentResponse(
                saved.getId(),
                saved.getOriginalFilename(),
                saved.getCompanyName(),
                saved.getStatus(),
                "Document uploaded successfully. Processing started."
        );
    }

    @Transactional
    public DocumentResponse processDocument(User user, String documentId) {
        findAndVerifyOwnership(user, documentId);
        // Process synchronously and return updated status
        UploadedDocument readyDoc = pdfIngestionService.ingestDocument(documentId);
        return DocumentResponse.fromEntity(readyDoc);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listDocuments(User user) {
        return documentRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(DocumentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(User user, String documentId) {
        UploadedDocument doc = findAndVerifyOwnership(user, documentId);
        return DocumentResponse.fromEntity(doc);
    }

    @Transactional
    public void deleteDocument(User user, String documentId) {
        UploadedDocument doc = findAndVerifyOwnership(user, documentId);

        // Step 1: Purge all vector chunks for this document from the vector store.
        // Retrieve chunk IDs by metadata filter on documentId, then delete them.
        try {
            FilterExpressionBuilder fb = new FilterExpressionBuilder();
            List<org.springframework.ai.document.Document> chunks = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query("*")
                            .topK(10000)
                            .filterExpression(fb.eq("documentId", documentId).build())
                            .build()
            );
            if (!chunks.isEmpty()) {
                List<String> chunkIds = chunks.stream()
                        .map(org.springframework.ai.document.Document::getId)
                        .toList();
                vectorStore.delete(chunkIds);
                log.info("Purged {} vector chunks for documentId={}", chunkIds.size(), documentId);
            } else {
                log.debug("No vector chunks found for documentId={} (already purged or not yet indexed)", documentId);
            }
        } catch (Exception e) {
            // Non-fatal: log warning and continue — file and DB record should still be cleaned up
            log.warn("Failed to purge vectors for documentId={}: {}", documentId, e.getMessage());
        }

        // Step 2: Delete all chat messages associated with this document
        try {
            chatMessageRepository.deleteByDocumentId(documentId);
            log.info("Purged chat history for documentId={}", documentId);
        } catch (Exception e) {
            log.warn("Failed to purge chat history for documentId={}: {}", documentId, e.getMessage());
        }

        // Step 3: Delete physical file from disk
        if (doc.getStoragePath() != null) {
            try {
                Path filePath = Paths.get(doc.getStoragePath());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.warn("Could not delete stored file on disk: {}", doc.getStoragePath(), e);
            }
        }

        // Step 4: Delete DB record
        documentRepository.delete(doc);
        log.info("Document deleted successfully: id={}, ownerId={}", documentId, user.getId());
    }

    public UploadedDocument findAndVerifyOwnership(User user, String documentId) {
        UploadedDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        if (!doc.getOwnerId().equals(user.getId())) {
            log.warn("Access denied: user {} attempted to access document {} owned by {}",
                    user.getId(), documentId, doc.getOwnerId());
            throw new AccessDeniedException("You do not have permission to access this document.");
        }

        return doc;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be empty.");
        }

        long maxBytes = maxFileSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("File size (" + (file.getSize() / 1024 / 1024) + "MB) exceeds the maximum allowed limit of " + maxFileSizeMb + "MB.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Invalid file type. Only PDF documents (.pdf) are supported.");
        }

        // Validate PDF magic bytes (%PDF)
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[4];
            int read = is.read(header);
            if (read < 4 || header[0] != PDF_MAGIC_BYTES[0] || header[1] != PDF_MAGIC_BYTES[1] ||
                    header[2] != PDF_MAGIC_BYTES[2] || header[3] != PDF_MAGIC_BYTES[3]) {
                throw new IllegalArgumentException("Invalid PDF format. The file is corrupt or not an authentic PDF document.");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read uploaded file to verify format.");
        }
    }

    private String deriveCompanyName(String originalFilename, String requestedCompanyName) {
        if (requestedCompanyName != null && !requestedCompanyName.trim().isEmpty()) {
            return requestedCompanyName.trim();
        }
        String clean = originalFilename.replaceAll("(?i)\\.pdf$", "");
        clean = clean.replace('_', ' ').replace('-', ' ').trim();
        return clean.isEmpty() ? "Unknown Company" : clean;
    }
}
