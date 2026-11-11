package com.prospectai.document;

import com.prospectai.common.ApiResponse;
import com.prospectai.document.dto.DocumentResponse;
import com.prospectai.document.dto.UploadDocumentResponse;
import com.prospectai.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadDocumentResponse>> upload(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "companyName", required = false) String companyName
    ) {
        UploadDocumentResponse response = documentService.uploadDocument(user, file, companyName);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document uploaded successfully.", response));
    }

    @PostMapping("/{documentId}/process")
    public ResponseEntity<ApiResponse<DocumentResponse>> process(
            @AuthenticationPrincipal User user,
            @PathVariable String documentId
    ) {
        DocumentResponse response = documentService.processDocument(user, documentId);
        return ResponseEntity.ok(ApiResponse.success("Document processed successfully.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> list(@AuthenticationPrincipal User user) {
        List<DocumentResponse> documents = documentService.listDocuments(user);
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<ApiResponse<DocumentResponse>> get(
            @AuthenticationPrincipal User user,
            @PathVariable String documentId
    ) {
        DocumentResponse document = documentService.getDocument(user, documentId);
        return ResponseEntity.ok(ApiResponse.success(document));
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal User user,
            @PathVariable String documentId
    ) {
        documentService.deleteDocument(user, documentId);
        return ResponseEntity.ok(ApiResponse.success("Document deleted successfully.", null));
    }
}
