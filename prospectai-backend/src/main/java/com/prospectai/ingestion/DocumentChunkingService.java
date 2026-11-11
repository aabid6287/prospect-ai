package com.prospectai.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service that splits extracted PDF page documents into semantically coherent
 * chunks and attaches stable metadata for Pinecone vector storage.
 */
@Service
public class DocumentChunkingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentChunkingService.class);

    @Value("${app.ingestion.chunk-size:1000}")
    private int chunkSize;

    @Value("${app.ingestion.min-chunk-size-chars:100}")
    private int minChunkSizeChars;

    private static final Pattern SECTION_PATTERN = Pattern.compile(
            "(?i)(SECTION\\s+[IVXLCDM0-9]+|RISK\\s+FACTORS|OBJECTS\\s+OF\\s+THE\\s+OFFER|USE\\s+OF\\s+PROCEEDS|" +
            "FINANCIAL\\s+STATEMENTS|FINANCIAL\\s+INFORMATION|OUR\\s+BUSINESS|INDUSTRY\\s+OVERVIEW|" +
            "PROMOTERS?\\s+AND\\s+MANAGEMENT|OUTSTANDING\\s+LITIGATION|CAPITAL\\s+STRUCTURE)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Splits page-level documents into chunks while strictly preserving page citations
     * and attaching stable vector IDs for idempotent vector storage.
     *
     * @param pageDocuments Extracted page documents
     * @param documentId    Prospectus document UUID
     * @param ownerId       User owner UUID
     * @return List of chunked Document instances ready for VectorStore insertion
     */
    public List<Document> chunkDocuments(List<Document> pageDocuments, String documentId, String ownerId) {
        log.info("Starting chunking for documentId={} with chunkSize={}, minChunkSizeChars={}",
                documentId, chunkSize, minChunkSizeChars);

        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(chunkSize)
                .withMinChunkSizeChars(minChunkSizeChars)
                .build();

        List<Document> finalChunks = new ArrayList<>();
        int globalChunkIndex = 0;
        String currentDetectedSection = "General Information";

        for (Document pageDoc : pageDocuments) {
            String pageText = pageDoc.getText();
            if (pageText == null || pageText.trim().isEmpty()) {
                continue;
            }

            int pageNumber = extractPageNumber(pageDoc);

            // Check if this page introduces a new major section heading
            String newSection = detectSection(pageText);
            if (newSection != null) {
                currentDetectedSection = newSection;
            }

            // Split the single page document
            List<Document> rawPageChunks = splitter.apply(List.of(pageDoc));

            for (Document rawChunk : rawPageChunks) {
                String chunkContent = rawChunk.getText();
                if (chunkContent == null || chunkContent.trim().isEmpty()) {
                    continue;
                }

                // Construct stable vector ID for idempotent upserts: {docId}_p{page}_c{index}
                String vectorId = String.format("%s_p%d_c%d", documentId, pageNumber, globalChunkIndex);

                // Build enriched metadata map
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("documentId", documentId);
                metadata.put("ownerId", ownerId);
                metadata.put("pageNumber", pageNumber);
                metadata.put("chunkIndex", globalChunkIndex);
                metadata.put("section", currentDetectedSection);
                metadata.put("charLength", chunkContent.length());

                Document enrichedDoc = new Document(vectorId, chunkContent, metadata);
                finalChunks.add(enrichedDoc);
                globalChunkIndex++;
            }
        }

        log.info("Completed chunking: produced {} chunks across {} pages for documentId={}",
                finalChunks.size(), pageDocuments.size(), documentId);

        return finalChunks;
    }

    private int extractPageNumber(Document doc) {
        Map<String, Object> metadata = doc.getMetadata();
        if (metadata != null && metadata.containsKey("page_number")) {
            Object p = metadata.get("page_number");
            if (p instanceof Number n) {
                return n.intValue();
            }
        }
        return 1;
    }

    private String detectSection(String text) {
        if (text == null) return null;
        Matcher matcher = SECTION_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group().toUpperCase().trim();
        }
        return null;
    }
}
