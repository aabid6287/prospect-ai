package com.prospectai.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "Question cannot be blank")
        @Size(max = 2000, message = "Question cannot exceed 2000 characters")
        String question,

        String conversationId
) {
    public ChatRequest(String question) {
        this(question, null);
    }
}
