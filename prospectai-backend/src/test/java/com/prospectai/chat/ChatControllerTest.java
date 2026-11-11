package com.prospectai.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospectai.auth.AuthService;
import com.prospectai.auth.dto.AuthResponse;
import com.prospectai.auth.dto.RegisterRequest;
import com.prospectai.chat.dto.ChatRequest;
import com.prospectai.document.DocumentRepository;
import com.prospectai.document.DocumentStatus;
import com.prospectai.document.UploadedDocument;
import com.prospectai.user.User;
import com.prospectai.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private VectorStore vectorStore;

    private User primaryUser;
    private String primaryToken;

    private User secondaryUser;
    private String secondaryToken;

    private UploadedDocument readyDoc;
    private UploadedDocument processingDoc;

    @BeforeEach
    void setUp() {
        chatMessageRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();

        // Register Primary User
        AuthResponse auth1 = authService.register(new RegisterRequest("Primary Analyst", "primary@prospectai.com", "Password123!"));
        primaryToken = auth1.token();
        primaryUser = userRepository.findById(auth1.id()).orElseThrow();

        // Register Secondary User
        AuthResponse auth2 = authService.register(new RegisterRequest("Secondary Analyst", "secondary@prospectai.com", "Password123!"));
        secondaryToken = auth2.token();
        secondaryUser = userRepository.findById(auth2.id()).orElseThrow();

        // Create Ready Document for Primary User
        readyDoc = new UploadedDocument();
        readyDoc.setId(UUID.randomUUID().toString());
        readyDoc.setOwnerId(primaryUser.getId());
        readyDoc.setCompanyName("Swiggy Limited");
        readyDoc.setOriginalFilename("Swiggy_RHP.pdf");
        readyDoc.setStoragePath("uploads/test/swiggy.pdf");
        readyDoc.setFileSize(1024000L);
        readyDoc.setContentType("application/pdf");
        readyDoc.setPageCount(524);
        readyDoc.setStatus(DocumentStatus.READY);
        readyDoc.setCreatedAt(Instant.now());
        readyDoc.setUpdatedAt(Instant.now());
        documentRepository.save(readyDoc);

        // Create Processing Document for Primary User
        processingDoc = new UploadedDocument();
        processingDoc.setId(UUID.randomUUID().toString());
        processingDoc.setOwnerId(primaryUser.getId());
        processingDoc.setCompanyName("Zomato Limited");
        processingDoc.setOriginalFilename("Zomato_RHP.pdf");
        processingDoc.setStoragePath("uploads/test/zomato.pdf");
        processingDoc.setFileSize(2048000L);
        processingDoc.setContentType("application/pdf");
        processingDoc.setStatus(DocumentStatus.PROCESSING);
        processingDoc.setCreatedAt(Instant.now());
        processingDoc.setUpdatedAt(Instant.now());
        documentRepository.save(processingDoc);

        // Seed chunks into VectorStore for readyDoc
        Document chunk1 = new Document(
                "Section II: Risk Factors. We have incurred restated losses of INR 2,350 Crore in FY24.",
                Map.of(
                        "documentId", readyDoc.getId(),
                        "ownerId", primaryUser.getId(),
                        "pageNumber", 14,
                        "section", "Risk Factors"
                )
        );
        Document chunk2 = new Document(
                "Objects of the Offer: Net issue proceeds will be used for expansion of Dark Stores across Tier 1 cities.",
                Map.of(
                        "documentId", readyDoc.getId(),
                        "ownerId", primaryUser.getId(),
                        "pageNumber", 88,
                        "section", "Objects of the Offer"
                )
        );
        vectorStore.add(List.of(chunk1, chunk2));
    }

    @Test
    void shouldAnswerQuestionSuccessfullyWithCitationsWhenReady() throws Exception {
        ChatRequest request = new ChatRequest("What are the restated losses in risk factors?");

        mockMvc.perform(post("/api/chat/" + readyDoc.getId())
                        .header("Authorization", "Bearer " + primaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentId").value(readyDoc.getId()))
                .andExpect(jsonPath("$.data.question").value(request.question()))
                .andExpect(jsonPath("$.data.answer").isNotEmpty())
                .andExpect(jsonPath("$.data.citations", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.citations[0].pageNumber").value(14))
                .andExpect(jsonPath("$.data.citations[0].section").value("Risk Factors"));
    }

    @Test
    void shouldDenyAccessToDocumentOwnedByAnotherUser() throws Exception {
        ChatRequest request = new ChatRequest("What are the objects of the offer?");

        // Secondary user tries to query Primary user's document
        mockMvc.perform(post("/api/chat/" + readyDoc.getId())
                        .header("Authorization", "Bearer " + secondaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectQuestionWhenDocumentNotReady() throws Exception {
        ChatRequest request = new ChatRequest("What is the valuation?");

        mockMvc.perform(post("/api/chat/" + processingDoc.getId())
                        .header("Authorization", "Bearer " + primaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("not ready for chat")));
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        ChatRequest request = new ChatRequest("Summarize risks.");

        mockMvc.perform(post("/api/chat/" + readyDoc.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnValidationErrorForEmptyQuestion() throws Exception {
        ChatRequest request = new ChatRequest("   ");

        mockMvc.perform(post("/api/chat/" + readyDoc.getId())
                        .header("Authorization", "Bearer " + primaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFetchChatHistoryForAuthorizedOwner() throws Exception {
        // Send a question first
        ChatRequest request = new ChatRequest("How will proceeds be deployed?");
        mockMvc.perform(post("/api/chat/" + readyDoc.getId())
                        .header("Authorization", "Bearer " + primaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Now query history
        mockMvc.perform(get("/api/chat/" + readyDoc.getId() + "/history")
                        .header("Authorization", "Bearer " + primaryToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].question").value(request.question()))
                .andExpect(jsonPath("$.data[0].answer").isNotEmpty())
                .andExpect(jsonPath("$.data[0].citations").isArray());
    }
}
