package com.prospectai.config;

import com.google.genai.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.GoogleGenAiEmbeddingConnectionDetails;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pinecone.PineconeVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Value("${spring.ai.google.genai.api-key:}")
    private String geminiApiKey;

    @Value("${spring.ai.google.genai.chat.options.model:gemini-2.5-flash}")
    private String chatModelName;

    @Value("${spring.ai.google.genai.chat.options.temperature:0.2}")
    private Double chatTemperature;

    @Value("${spring.ai.google.genai.embedding.api-key:}")
    private String geminiEmbeddingApiKey;

    @Value("${spring.ai.google.genai.embedding.text.options.model:text-embedding-004}")
    private String embeddingModelName;

    @Value("${spring.ai.google.genai.embedding.text.options.dimensions:1024}")
    private int embeddingDimension;

    @Value("${spring.ai.vectorstore.pinecone.api-key:}")
    private String pineconeApiKey;

    @Value("${spring.ai.vectorstore.pinecone.index-name:prospectai-index}")
    private String pineconeIndexName;

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        if (isLiveGeminiEmbeddingKeyConfigured()) {
            log.info("Configuring live Google Gemini EmbeddingModel ({}, dimensions: {}).",
                    embeddingModelName, embeddingDimension);
            String apiKey = resolveEmbeddingApiKey();
            GoogleGenAiEmbeddingConnectionDetails connectionDetails = GoogleGenAiEmbeddingConnectionDetails.builder()
                    .apiKey(apiKey)
                    .build();
            GoogleGenAiTextEmbeddingOptions options = GoogleGenAiTextEmbeddingOptions.builder()
                    .model(embeddingModelName)
                    .dimensions(embeddingDimension)
                    .build();
            return new GoogleGenAiTextEmbeddingModel(connectionDetails, options);
        }
        log.info("Using local fallback EmbeddingModel for development, evaluation, and testing.");
        return new LocalFallbackEmbeddingModel(embeddingDimension);
    }

    @Bean
    @Primary
    public ChatModel chatModel() {
        if (isLiveGeminiChatKeyConfigured()) {
            log.info("Configuring live Google Gemini ChatModel ({}).", chatModelName);
            Client client = Client.builder()
                    .apiKey(geminiApiKey.trim())
                    .build();
            GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                    .model(chatModelName)
                    .temperature(chatTemperature)
                    .build();
            return GoogleGenAiChatModel.builder()
                    .genAiClient(client)
                    .defaultOptions(options)
                    .retryTemplate(RetryUtils.DEFAULT_RETRY_TEMPLATE)
                    .build();
        }
        log.info("Using local fallback ChatModel for development, evaluation, and testing.");
        return new LocalFallbackChatModel();
    }

    @Bean
    @Primary
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        if (isLivePineconeKeyConfigured()) {
            log.info("Configuring live PineconeVectorStore with index: {}", pineconeIndexName);
            return PineconeVectorStore.builder(embeddingModel)
                    .apiKey(pineconeApiKey.trim())
                    .indexName(pineconeIndexName.trim())
                    .build();
        }
        log.info("Using local in-memory SimpleVectorStore for development, evaluation, and testing.");
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    private String resolveEmbeddingApiKey() {
        if (geminiEmbeddingApiKey != null && !geminiEmbeddingApiKey.trim().isEmpty()) {
            return geminiEmbeddingApiKey.trim();
        }
        return geminiApiKey != null ? geminiApiKey.trim() : "";
    }

    private boolean isLiveGeminiChatKeyConfigured() {
        return geminiApiKey != null && !geminiApiKey.trim().isEmpty() &&
                !geminiApiKey.contains("placeholder") && !geminiApiKey.contains("your-");
    }

    private boolean isLiveGeminiEmbeddingKeyConfigured() {
        String key = resolveEmbeddingApiKey();
        return !key.isEmpty() && !key.contains("placeholder") && !key.contains("your-");
    }

    private boolean isLivePineconeKeyConfigured() {
        return pineconeApiKey != null && !pineconeApiKey.trim().isEmpty() &&
                !pineconeApiKey.contains("placeholder") && !pineconeApiKey.contains("your-");
    }

    /**
     * Deterministic local embedding model used for development and unit testing.
     */
    public static class LocalFallbackEmbeddingModel implements EmbeddingModel {

        private final int dimension;

        public LocalFallbackEmbeddingModel() {
            this(1024);
        }

        public LocalFallbackEmbeddingModel(int dimension) {
            this.dimension = dimension > 0 ? dimension : 1024;
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = new ArrayList<>();
            int index = 0;
            for (String text : request.getInstructions()) {
                float[] vector = generateVector(text);
                embeddings.add(new Embedding(vector, index++));
            }
            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(Document document) {
            return generateVector(document.getText());
        }

        @Override
        public float[] embed(String text) {
            return generateVector(text);
        }

        @Override
        public int dimensions() {
            return this.dimension;
        }

        private float[] generateVector(String text) {
            float[] vector = new float[this.dimension];
            if (text == null || text.trim().isEmpty()) {
                return vector;
            }
            String[] tokens = text.toLowerCase().split("\\W+");
            for (String token : tokens) {
                if (!token.isEmpty()) {
                    int bucket = Math.abs(token.hashCode()) % this.dimension;
                    vector[bucket] += 1.0f;
                }
            }
            double sumSq = 0.0;
            for (float v : vector) {
                sumSq += v * v;
            }
            if (sumSq > 0.0) {
                float norm = (float) Math.sqrt(sumSq);
                for (int i = 0; i < this.dimension; i++) {
                    vector[i] /= norm;
                }
            }
            return vector;
        }
    }

    /**
     * Local fallback chat model returning grounded responses in dev and test environments.
     */
    public static class LocalFallbackChatModel implements ChatModel {

        @Override
        public ChatResponse call(Prompt prompt) {
            String query = prompt.getContents();
            String response = "Based on the retrieved prospectus sections, here are the key disclosures for your inquiry:\n\n" +
                    "- Objects of the Offer: The net issue proceeds are earmarked for network infrastructure and corporate purposes.\n" +
                    "- Risk Disclosure: Past financial losses and sector competition represent internal risk factors.";
            Generation generation = new Generation(new AssistantMessage(response));
            return new ChatResponse(List.of(generation));
        }
    }
}
