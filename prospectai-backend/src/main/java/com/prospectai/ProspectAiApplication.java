package com.prospectai;

import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration;
import org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiEmbeddingConnectionAutoConfiguration;
import org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiTextEmbeddingAutoConfiguration;
import org.springframework.ai.vectorstore.pinecone.autoconfigure.PineconeVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {
    GoogleGenAiChatAutoConfiguration.class,
    GoogleGenAiTextEmbeddingAutoConfiguration.class,
    GoogleGenAiEmbeddingConnectionAutoConfiguration.class,
    PineconeVectorStoreAutoConfiguration.class
})
@EnableAsync
public class ProspectAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProspectAiApplication.class, args);
    }
}
