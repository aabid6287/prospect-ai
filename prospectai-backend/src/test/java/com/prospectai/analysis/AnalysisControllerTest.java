package com.prospectai.analysis;

import com.prospectai.auth.AuthService;
import com.prospectai.auth.dto.AuthResponse;
import com.prospectai.auth.dto.RegisterRequest;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

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
        documentRepository.deleteAll();
        userRepository.deleteAll();

        // Primary User
        AuthResponse auth1 = authService.register(new RegisterRequest("Lead Analyst", "lead@prospectai.com", "Password123!"));
        primaryToken = auth1.token();
        primaryUser = userRepository.findById(auth1.id()).orElseThrow();

        // Secondary User
        AuthResponse auth2 = authService.register(new RegisterRequest("Other Analyst", "other@prospectai.com", "Password123!"));
        secondaryToken = auth2.token();
        secondaryUser = userRepository.findById(auth2.id()).orElseThrow();

        // Ready Doc
        readyDoc = new UploadedDocument();
        readyDoc.setId(UUID.randomUUID().toString());
        readyDoc.setOwnerId(primaryUser.getId());
        readyDoc.setCompanyName("Swiggy Limited");
        readyDoc.setOriginalFilename("Swiggy_RHP.pdf");
        readyDoc.setStoragePath("uploads/test/swiggy.pdf");
        readyDoc.setFileSize(2048000L);
        readyDoc.setContentType("application/pdf");
        readyDoc.setPageCount(524);
        readyDoc.setStatus(DocumentStatus.READY);
        readyDoc.setCreatedAt(Instant.now());
        readyDoc.setUpdatedAt(Instant.now());
        documentRepository.save(readyDoc);

        // Processing Doc
        processingDoc = new UploadedDocument();
        processingDoc.setId(UUID.randomUUID().toString());
        processingDoc.setOwnerId(primaryUser.getId());
        processingDoc.setCompanyName("Zomato Limited");
        processingDoc.setOriginalFilename("Zomato_RHP.pdf");
        processingDoc.setStoragePath("uploads/test/zomato.pdf");
        processingDoc.setStatus(DocumentStatus.PROCESSING);
        processingDoc.setCreatedAt(Instant.now());
        processingDoc.setUpdatedAt(Instant.now());
        documentRepository.save(processingDoc);

        // Seed vector store
        Document chunk1 = new Document(
                "Section I: Business Overview. Swiggy operates a consumer digital commerce platform across food delivery and quick-commerce.",
                Map.of("documentId", readyDoc.getId(), "ownerId", primaryUser.getId(), "pageNumber", 5, "section", "Business Overview")
        );
        Document chunk2 = new Document(
                "Section II: Risk Factors. Historical restated net loss was INR 2,350 Crore in FY24 due to scale-up investments.",
                Map.of("documentId", readyDoc.getId(), "ownerId", primaryUser.getId(), "pageNumber", 18, "section", "Risk Factors")
        );
        Document chunk3 = new Document(
                "Section VI: Financial Statements. Revenue from operations reached INR 11,247 Crore with short-term debt of INR 450 Crore.",
                Map.of("documentId", readyDoc.getId(), "ownerId", primaryUser.getId(), "pageNumber", 215, "section", "Financials")
        );
        vectorStore.add(List.of(chunk1, chunk2, chunk3));
    }

    @Test
    void shouldReturnOverviewWhenDocumentReady() throws Exception {
        mockMvc.perform(get("/api/analysis/" + readyDoc.getId() + "/overview")
                        .header("Authorization", "Bearer " + primaryToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentId").value(readyDoc.getId()))
                .andExpect(jsonPath("$.data.companyName").value("Swiggy Limited"))
                .andExpect(jsonPath("$.data.businessModel").isNotEmpty())
                .andExpect(jsonPath("$.data.objectsOfIssue").isNotEmpty())
                .andExpect(jsonPath("$.data.disclaimer").isNotEmpty())
                .andExpect(jsonPath("$.data.citations").isArray());
    }

    @Test
    void shouldReturnRisksWhenDocumentReady() throws Exception {
        mockMvc.perform(get("/api/analysis/" + readyDoc.getId() + "/risks")
                        .header("Authorization", "Bearer " + primaryToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentId").value(readyDoc.getId()))
                .andExpect(jsonPath("$.data.internalRisksSummary").isNotEmpty())
                .andExpect(jsonPath("$.data.keyInternalRisks", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data.keyExternalRisks", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data.litigationSummary").isNotEmpty())
                .andExpect(jsonPath("$.data.disclaimer").isNotEmpty());
    }

    @Test
    void shouldReturnFinancialsWhenDocumentReady() throws Exception {
        mockMvc.perform(get("/api/analysis/" + readyDoc.getId() + "/financials")
                        .header("Authorization", "Bearer " + primaryToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentId").value(readyDoc.getId()))
                .andExpect(jsonPath("$.data.revenueTrend").isNotEmpty())
                .andExpect(jsonPath("$.data.keyMetrics", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.data.keyMetrics[0].unit").value("INR Crore"))
                .andExpect(jsonPath("$.data.disclaimer").isNotEmpty());
    }

    @Test
    void shouldDenyAccessToDocumentOwnedByAnotherUser() throws Exception {
        mockMvc.perform(get("/api/analysis/" + readyDoc.getId() + "/overview")
                        .header("Authorization", "Bearer " + secondaryToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectAnalysisWhenDocumentNotReady() throws Exception {
        mockMvc.perform(get("/api/analysis/" + processingDoc.getId() + "/overview")
                        .header("Authorization", "Bearer " + primaryToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("not ready for analysis")));
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/analysis/" + readyDoc.getId() + "/overview"))
                .andExpect(status().isUnauthorized());
    }
}
