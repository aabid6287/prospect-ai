package com.prospectai.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospectai.chat.dto.ChatRequest;
import com.prospectai.chat.dto.ChatResponse;
import com.prospectai.chat.dto.CitationDto;
import com.prospectai.document.DocumentRepository;
import com.prospectai.document.DocumentStatus;
import com.prospectai.document.UploadedDocument;
import com.prospectai.exception.AccessDeniedException;
import com.prospectai.exception.ResourceNotFoundException;
import com.prospectai.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final int TOP_K = 4;
    private static final double SIMILARITY_THRESHOLD = 0.2;

    private final DocumentRepository documentRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final VectorStore vectorStore;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public ChatService(DocumentRepository documentRepository,
                       ChatMessageRepository chatMessageRepository,
                       VectorStore vectorStore,
                       ChatModel chatModel,
                       ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ChatResponse askQuestion(User user, String documentId, ChatRequest request) {
        UploadedDocument doc = verifyDocumentAccess(user, documentId);

        if (doc.getStatus() != DocumentStatus.READY) {
            throw new IllegalArgumentException(
                    "Document is not ready for chat. Current status: " + doc.getStatus() +
                    ". Please wait until indexing finishes."
            );
        }

        String question = request.question().trim();
        log.info("Processing chat query for documentId: {}, user: {}", documentId, user.getEmail());

        // 1. Perform document-filtered vector retrieval
        List<Document> matchedChunks = retrieveRelevantChunks(documentId, question);

        // 2. Extract citations strictly from retrieved metadata
        List<CitationDto> citations = extractCitations(matchedChunks);

        // 3. Construct grounded system and user prompt
        String answer;
        if (matchedChunks.isEmpty()) {
            answer = "The uploaded sections of this prospectus (" + doc.getCompanyName() +
                    ") do not contain information related to your question: \"" + question + "\".";
        } else {
            answer = generateGroundedAnswer(doc, question, matchedChunks);
        }

        // 4. Persist conversation history
        String conversationId = request.conversationId() != null && !request.conversationId().isBlank()
                ? request.conversationId().trim()
                : "conv-" + documentId;

        String citationsJson = serializeCitations(citations);

        ChatMessage chatMessage = new ChatMessage(
                UUID.randomUUID().toString(),
                documentId,
                user.getId(),
                conversationId,
                question,
                answer,
                citationsJson,
                LocalDateTime.now()
        );
        chatMessageRepository.save(chatMessage);

        return new ChatResponse(
                chatMessage.getId(),
                chatMessage.getDocumentId(),
                chatMessage.getConversationId(),
                chatMessage.getQuestion(),
                chatMessage.getAnswer(),
                citations,
                chatMessage.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ChatResponse> getChatHistory(User user, String documentId) {
        verifyDocumentAccess(user, documentId);

        List<ChatMessage> messages = chatMessageRepository
                .findByDocumentIdAndOwnerIdOrderByCreatedAtAsc(documentId, user.getId());

        return messages.stream().map(msg -> new ChatResponse(
                msg.getId(),
                msg.getDocumentId(),
                msg.getConversationId(),
                msg.getQuestion(),
                msg.getAnswer(),
                deserializeCitations(msg.getCitationsJson()),
                msg.getCreatedAt()
        )).toList();
    }

    private List<Document> retrieveRelevantChunks(String documentId, String question) {
        try {
            FilterExpressionBuilder filter = new FilterExpressionBuilder();
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(question)
                    .topK(TOP_K)
                    .similarityThreshold(SIMILARITY_THRESHOLD)
                    .filterExpression(filter.eq("documentId", documentId).build())
                    .build();

            return vectorStore.similaritySearch(searchRequest);
        } catch (Exception e) {
            log.warn("Filtered similaritySearch encountered exception, falling back to query-only search with post-filtering: {}", e.getMessage());
            // Fallback for vector stores that might not support certain expression trees in tests
            List<Document> allMatches = vectorStore.similaritySearch(
                    SearchRequest.builder().query(question).topK(TOP_K * 2).build()
            );
            return allMatches.stream()
                    .filter(d -> documentId.equals(d.getMetadata().get("documentId")))
                    .limit(TOP_K)
                    .toList();
        }
    }

    private List<CitationDto> extractCitations(List<Document> matchedChunks) {
        List<CitationDto> citations = new ArrayList<>();
        Set<String> seenCitations = new HashSet<>();

        for (Document chunk : matchedChunks) {
            Map<String, Object> meta = chunk.getMetadata();
            Integer pageNum = null;
            if (meta.containsKey("pageNumber")) {
                pageNum = ((Number) meta.get("pageNumber")).intValue();
            } else if (meta.containsKey("page_number")) {
                pageNum = ((Number) meta.get("page_number")).intValue();
            }

            String section = (String) meta.getOrDefault("section", "Prospectus Disclosure");

            String rawText = chunk.getText() != null ? chunk.getText().trim() : "";
            String excerpt = rawText.length() > 250 ? rawText.substring(0, 247) + "..." : rawText;

            String key = pageNum + ":" + section;
            if (!seenCitations.contains(key)) {
                seenCitations.add(key);
                citations.add(new CitationDto(pageNum, section, excerpt));
            }
        }
        return citations;
    }

    private String generateGroundedAnswer(UploadedDocument doc, String question, List<Document> chunks) {
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("DOCUMENT: ").append(doc.getCompanyName()).append(" (RHP)\n\n");

        for (Document chunk : chunks) {
            Map<String, Object> meta = chunk.getMetadata();
            Object page = meta.getOrDefault("pageNumber", meta.getOrDefault("page_number", "Unknown"));
            Object section = meta.getOrDefault("section", "General");
            contextBuilder.append(String.format("--- [Page: %s | Section: %s] ---\n", page, section));
            contextBuilder.append(chunk.getText()).append("\n\n");
        }

        String systemPromptText = """
                You are ProspectAI, an expert financial research analyst analyzing an Indian Red Herring Prospectus (RHP).
                Answer the user's inquiry accurately and factually using ONLY the provided RHP context.

                Mandatory Guidelines:
                1. Grounding: Answer strictly using facts, disclosures, and figures present in the provided context excerpts.
                2. Missing Evidence: If the provided text does not contain sufficient details to answer, state: "The provided sections of the prospectus do not contain information to answer this question."
                3. Precision: Preserve exact currency units (e.g., INR, INR Crore, Thousands, Lakhs), fiscal periods (e.g., FY23, FY24, FY25), negative signs, and table values without truncation.
                4. Tone: Objective, factual equity research tone. Clearly differentiate between internal risks, external risks, financial performance, and proposed object of issue expenditures.
                5. Disclaimer: Provide informational research assistance; do not provide guaranteed investment recommendations.
                """;

        String userPromptText = String.format("Context:\n%s\n\nQuestion: %s", contextBuilder, question);

        List<Message> messages = List.of(
                new SystemMessage(systemPromptText),
                new UserMessage(userPromptText)
        );

        try {
            org.springframework.ai.chat.model.ChatResponse response = chatModel.call(new Prompt(messages));
            if (response != null && response.getResult() != null && response.getResult().getOutput() != null) {
                return response.getResult().getOutput().getText();
            }
        } catch (Exception e) {
            log.error("Error executing ChatModel call: {}", e.getMessage(), e);
        }

        return "Unable to generate an AI response at this moment. Please review the extracted source citations.";
    }

    private UploadedDocument verifyDocumentAccess(User user, String documentId) {
        UploadedDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        if (!doc.getOwnerId().equals(user.getId())) {
            log.warn("Access denied: user {} attempted to access document {} owned by {}",
                    user.getId(), documentId, doc.getOwnerId());
            throw new AccessDeniedException("You do not have permission to access this document.");
        }
        return doc;
    }

    private String serializeCitations(List<CitationDto> citations) {
        try {
            return objectMapper.writeValueAsString(citations);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize citations to JSON", e);
            return "[]";
        }
    }

    private List<CitationDto> deserializeCitations(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<CitationDto>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize citations from JSON", e);
            return Collections.emptyList();
        }
    }
}
