package com.prospectai.chat;

import com.prospectai.common.ApiResponse;
import com.prospectai.chat.dto.ChatRequest;
import com.prospectai.chat.dto.ChatResponse;
import com.prospectai.user.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/{documentId}")
    public ResponseEntity<ApiResponse<ChatResponse>> askQuestion(
            @AuthenticationPrincipal User user,
            @PathVariable String documentId,
            @Valid @RequestBody ChatRequest request
    ) {
        ChatResponse response = chatService.askQuestion(user, documentId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{documentId}/history")
    public ResponseEntity<ApiResponse<List<ChatResponse>>> getChatHistory(
            @AuthenticationPrincipal User user,
            @PathVariable String documentId
    ) {
        List<ChatResponse> history = chatService.getChatHistory(user, documentId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
