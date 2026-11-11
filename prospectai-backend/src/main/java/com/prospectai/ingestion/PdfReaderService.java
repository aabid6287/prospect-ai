package com.prospectai.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for extracting page-aware text from PDF documents
 * using Spring AI PDF Document Reader.
 */
@Service
public class PdfReaderService {

    private static final Logger log = LoggerFactory.getLogger(PdfReaderService.class);

    /**
     * Reads a PDF file page-by-page, preserving page numbers in document metadata.
     *
     * @param pdfPath File path to the PDF on disk
     * @return List of Spring AI Document instances, one per physical page
     */
    public List<Document> readPdf(Path pdfPath) {
        if (!Files.exists(pdfPath)) {
            throw new IllegalArgumentException("PDF file does not exist at path: " + pdfPath);
        }

        log.info("Starting PDF extraction for file: {}", pdfPath.getFileName());

        try {
            FileSystemResource resource = new FileSystemResource(pdfPath.toFile());
            PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                    .withPagesPerDocument(1)
                    .build();

            PagePdfDocumentReader reader = new PagePdfDocumentReader(resource, config);
            List<Document> rawDocuments = reader.read();

            if (rawDocuments == null || rawDocuments.isEmpty()) {
                throw new IllegalArgumentException("The PDF contains no readable text or is an empty document.");
            }

            List<Document> cleanedDocuments = new ArrayList<>();
            int fallbackPage = 1;

            for (Document doc : rawDocuments) {
                String text = doc.getText();
                if (text == null) {
                    text = "";
                }

                // Preserve or ensure 1-based page number metadata
                Map<String, Object> metadata = doc.getMetadata();
                int pageNumber = fallbackPage;
                if (metadata.containsKey("page_number")) {
                    Object p = metadata.get("page_number");
                    if (p instanceof Number n) {
                        pageNumber = n.intValue();
                    }
                } else {
                    metadata.put("page_number", fallbackPage);
                }

                // Normalize text cautiously: preserve newlines for tables, strip control chars
                String cleanedText = normalizeText(text);
                Document cleanedDoc = new Document(cleanedText, metadata);
                cleanedDocuments.add(cleanedDoc);

                fallbackPage++;
            }

            log.info("Successfully extracted {} pages from PDF: {}", cleanedDocuments.size(), pdfPath.getFileName());
            return cleanedDocuments;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse PDF document at {}: {}", pdfPath, e.getMessage(), e);
            throw new IllegalStateException("Failed to extract content from PDF. The document may be corrupted or password-protected.", e);
        }
    }

    /**
     * Cautious text normalization that cleans irregular control characters
     * while preserving table alignments, lines, and financial numbers.
     */
    private String normalizeText(String raw) {
        if (raw == null) return "";
        // Replace null and non-printable control bytes except newline (\n), tab (\t), return (\r)
        String cleaned = raw.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        // Normalize multiple spaces/tabs within lines while preserving line breaks
        cleaned = cleaned.replaceAll("[ \\t]+", " ");
        return cleaned.trim();
    }
}
