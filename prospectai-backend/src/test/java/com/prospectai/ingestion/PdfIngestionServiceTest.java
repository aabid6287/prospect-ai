package com.prospectai.ingestion;

import com.prospectai.auth.AuthService;
import com.prospectai.auth.dto.AuthResponse;
import com.prospectai.auth.dto.RegisterRequest;
import com.prospectai.document.DocumentRepository;
import com.prospectai.document.DocumentService;
import com.prospectai.document.DocumentStatus;
import com.prospectai.document.UploadedDocument;
import com.prospectai.document.dto.UploadDocumentResponse;
import com.prospectai.user.User;
import com.prospectai.user.UserRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
class PdfIngestionServiceTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private PdfReaderService pdfReaderService;

    @Autowired
    private DocumentChunkingService documentChunkingService;

    @Autowired
    private PdfIngestionService pdfIngestionService;

    @Autowired
    private VectorStore vectorStore;

    private User testUser;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        userRepository.deleteAll();

        AuthResponse auth = authService.register(new RegisterRequest("Analyst User", "analyst@test.com", "Password123!"));
        testUser = userRepository.findById(auth.id()).orElseThrow();
    }

    private byte[] createSampleRhpPdf() throws IOException {
        try (PDDocument document = new PDDocument()) {
            // Page 1: Introduction
            PDPage page1 = new PDPage();
            document.addPage(page1);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page1)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("SECTION I - GENERAL INFORMATION AND DEFINITIONS");
                contentStream.newLineAtOffset(0, -20);
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                contentStream.showText("This Red Herring Prospectus is dated October 2026 for Swiggy Limited.");
                contentStream.endText();
            }

            // Page 2: Risk Factors
            PDPage page2 = new PDPage();
            document.addPage(page2);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page2)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("SECTION II - RISK FACTORS");
                contentStream.newLineAtOffset(0, -20);
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                contentStream.showText("Internal Risk 1: We have incurred restated losses in each fiscal year since inception.");
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("Internal Risk 2: Intense competition from other quick-commerce and food delivery networks.");
                contentStream.endText();
            }

            // Page 3: Objects of the Offer
            PDPage page3 = new PDPage();
            document.addPage(page3);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page3)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("SECTION IV - OBJECTS OF THE OFFER");
                contentStream.newLineAtOffset(0, -20);
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                contentStream.showText("The net proceeds of INR 3,750 Crore are proposed to be utilized for dark stores expansion.");
                contentStream.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    @Test
    void shouldExtractPdfPagesWithMetadata() throws IOException {
        byte[] pdfBytes = createSampleRhpPdf();
        Path tempFile = Files.createTempFile("sample_rhp", ".pdf");
        Files.write(tempFile, pdfBytes);

        try {
            List<Document> pages = pdfReaderService.readPdf(tempFile);

            assertThat(pages).hasSize(3);
            assertThat(pages.get(0).getText()).contains("SECTION I - GENERAL INFORMATION");
            assertThat(pages.get(1).getText()).contains("SECTION II - RISK FACTORS");
            assertThat(pages.get(2).getText()).contains("OBJECTS OF THE OFFER");

            // Verify page metadata
            assertThat(pages.get(0).getMetadata().get("page_number")).isEqualTo(1);
            assertThat(pages.get(1).getMetadata().get("page_number")).isEqualTo(2);
            assertThat(pages.get(2).getMetadata().get("page_number")).isEqualTo(3);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void shouldChunkPagesAndPreserveMetadata() throws IOException {
        byte[] pdfBytes = createSampleRhpPdf();
        Path tempFile = Files.createTempFile("sample_rhp", ".pdf");
        Files.write(tempFile, pdfBytes);

        try {
            List<Document> pages = pdfReaderService.readPdf(tempFile);
            String docId = "doc-test-123";
            String ownerId = testUser.getId();

            List<Document> chunks = documentChunkingService.chunkDocuments(pages, docId, ownerId);

            assertThat(chunks).isNotEmpty();
            for (Document chunk : chunks) {
                assertThat(chunk.getMetadata()).containsKey("documentId");
                assertThat(chunk.getMetadata().get("documentId")).isEqualTo(docId);
                assertThat(chunk.getMetadata()).containsKey("ownerId");
                assertThat(chunk.getMetadata().get("ownerId")).isEqualTo(ownerId);
                assertThat(chunk.getMetadata()).containsKey("pageNumber");
                assertThat(chunk.getMetadata()).containsKey("chunkIndex");
                assertThat(chunk.getId()).startsWith(docId + "_p");
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void shouldIngestPdfAndTransitionToReady() throws IOException {
        byte[] pdfBytes = createSampleRhpPdf();
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "Swiggy_RHP.pdf",
                "application/pdf",
                pdfBytes
        );

        UploadDocumentResponse uploadResp = documentService.uploadDocument(testUser, multipartFile, "Swiggy Limited");
        String docId = uploadResp.id();

        // Ingest synchronously
        UploadedDocument readyDoc = pdfIngestionService.ingestDocument(docId);

        assertThat(readyDoc.getStatus()).isEqualTo(DocumentStatus.READY);
        assertThat(readyDoc.getPageCount()).isEqualTo(3);
        assertThat(readyDoc.getFailureReason()).isNull();

        // Verify vectors were added to VectorStore and can be retrieved
        List<Document> searchResults = vectorStore.similaritySearch(
                SearchRequest.builder().query("risk factors incurred restated losses").topK(3).build()
        );
        assertThat(searchResults).isNotEmpty();
    }

    @Test
    void shouldHandleIngestionFailureSafely() {
        UploadedDocument doc = new UploadedDocument();
        doc.setId("non-existent-file-doc");
        doc.setOwnerId(testUser.getId());
        doc.setOriginalFilename("broken.pdf");
        doc.setStoragePath("uploads/documents/does_not_exist_9999.pdf");
        doc.setStatus(DocumentStatus.UPLOADED);
        documentRepository.save(doc);

        UploadedDocument failedDoc = pdfIngestionService.ingestDocument("non-existent-file-doc");

        assertThat(failedDoc.getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(failedDoc.getFailureReason()).isNotEmpty();
    }
}
