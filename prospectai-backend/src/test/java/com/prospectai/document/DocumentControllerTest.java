package com.prospectai.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospectai.auth.AuthService;
import com.prospectai.auth.dto.AuthResponse;
import com.prospectai.auth.dto.RegisterRequest;
import com.prospectai.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    private String userAToken;
    private String userBToken;

    // Standard valid PDF header
    private static final byte[] VALID_PDF_BYTES = "%PDF-1.7\n1 0 obj\n<<\n/Type /Catalog\n>>\nendobj\ntrailer\n<<\n>>\n%%EOF".getBytes();

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        userRepository.deleteAll();

        // Register User A
        AuthResponse userA = authService.register(new RegisterRequest("User A", "userA@fintech.com", "Password123!"));
        userAToken = userA.token();

        // Register User B
        AuthResponse userB = authService.register(new RegisterRequest("User B", "userB@fintech.com", "Password123!"));
        userBToken = userB.token();
    }

    @Test
    void shouldUploadValidPdfSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Swiggy_Limited_RHP.pdf",
                "application/pdf",
                VALID_PDF_BYTES
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .param("companyName", "Swiggy Limited")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.originalFilename").value("Swiggy_Limited_RHP.pdf"))
                .andExpect(jsonPath("$.data.companyName").value("Swiggy Limited"))
                .andExpect(jsonPath("$.data.status").value("UPLOADED"));
    }

    @Test
    void shouldRejectNonPdfUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "financials.txt",
                "text/plain",
                "Revenue: 1000".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Only PDF documents")));
    }

    @Test
    void shouldRejectCorruptPdfWithoutMagicBytes() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malformed.pdf",
                "application/pdf",
                "THIS IS NOT A REAL PDF FILE CONTENT".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("corrupt or not an authentic PDF")));
    }

    @Test
    void shouldRejectEmptyPdfUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("cannot be empty")));
    }

    @Test
    void shouldEnforceUserDocumentIsolationWhenListing() throws Exception {
        // Upload document for User A
        MockMultipartFile fileA = new MockMultipartFile("file", "UserA_Doc.pdf", "application/pdf", VALID_PDF_BYTES);
        mockMvc.perform(multipart("/api/documents/upload")
                        .file(fileA)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isCreated());

        // Upload document for User B
        MockMultipartFile fileB = new MockMultipartFile("file", "UserB_Doc.pdf", "application/pdf", VALID_PDF_BYTES);
        mockMvc.perform(multipart("/api/documents/upload")
                        .file(fileB)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isCreated());

        // Verify User A only sees User A's document
        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].originalFilename").value("UserA_Doc.pdf"));

        // Verify User B only sees User B's document
        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].originalFilename").value("UserB_Doc.pdf"));
    }

    @Test
    void shouldGetDocumentByIdForOwner() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "Company_RHP.pdf", "application/pdf", VALID_PDF_BYTES);
        MvcResult uploadResult = mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isCreated())
                .andReturn();

        String docId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(get("/api/documents/" + docId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(docId))
                .andExpect(jsonPath("$.data.originalFilename").value("Company_RHP.pdf"));
    }

    @Test
    void shouldForbidAccessToOtherUsersDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "UserA_Confidential.pdf", "application/pdf", VALID_PDF_BYTES);
        MvcResult uploadResult = mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isCreated())
                .andReturn();

        String docId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // User B attempts to view User A's document
        mockMvc.perform(get("/api/documents/" + docId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("do not have permission")));
    }

    @Test
    void shouldDeleteDocumentByOwner() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "To_Delete.pdf", "application/pdf", VALID_PDF_BYTES);
        MvcResult uploadResult = mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isCreated())
                .andReturn();

        String docId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Delete document as owner
        mockMvc.perform(delete("/api/documents/" + docId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify document no longer exists in list
        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void shouldForbidDeletingOtherUsersDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "UserA_Important.pdf", "application/pdf", VALID_PDF_BYTES);
        MvcResult uploadResult = mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isCreated())
                .andReturn();

        String docId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // User B attempts to delete User A's document
        mockMvc.perform(delete("/api/documents/" + docId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        // Verify document still exists for User A
        mockMvc.perform(get("/api/documents/" + docId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk());
    }
}
