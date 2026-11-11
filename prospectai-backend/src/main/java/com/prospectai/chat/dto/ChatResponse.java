package com.prospectai.chat.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ChatResponse(
        String id,
        String documentId,
        String conversationId,
        String question,
        String answer,
        List<CitationDto> citations,
        LocalDateTime createdAt
) {
}
