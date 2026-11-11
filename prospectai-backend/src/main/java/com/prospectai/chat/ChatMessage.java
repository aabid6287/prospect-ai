package com.prospectai.chat;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_doc_owner", columnList = "document_id, owner_id")
})
public class ChatMessage {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "document_id", length = 36, nullable = false)
    private String documentId;

    @Column(name = "owner_id", length = 36, nullable = false)
    private String ownerId;

    @Column(name = "conversation_id", length = 64)
    private String conversationId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "citations_json", columnDefinition = "TEXT")
    private String citationsJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public ChatMessage() {
    }

    public ChatMessage(String id, String documentId, String ownerId, String conversationId,
                       String question, String answer, String citationsJson, LocalDateTime createdAt) {
        this.id = id;
        this.documentId = documentId;
        this.ownerId = ownerId;
        this.conversationId = conversationId;
        this.question = question;
        this.answer = answer;
        this.citationsJson = citationsJson;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getCitationsJson() {
        return citationsJson;
    }

    public void setCitationsJson(String citationsJson) {
        this.citationsJson = citationsJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
