package llmchat.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import llmchat.model.ChatMessage;
import llmchat.model.OllamaRequest;
import llmchat.model.OllamaResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Component
public class OllamaClient implements LlmClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${LLM_BASE_URL}")
    private String baseUrl;

    @Value("${LLM_MODEL}")
    private String model;

    public OllamaClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public ChatMessage send(List<ChatMessage> messages) {
        OllamaRequest requestBody = new OllamaRequest(model, messages, false);

        final String json;
        try {
            json = objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException e) {
            throw new LlmClientException("Не удалось сформировать запрос к LLM", e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/chat"))
                .timeout(Duration.ofMinutes(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        final HttpResponse<String> response;

        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new LlmClientException("""
                Не удалось подключиться к LLM.
                Убедитесь, что Ollama запущена.
                """, e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new LlmClientException("Запрос к LLM был прерван", e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new LlmClientException("""
                Не удалось получить ответ от модели.
                Проверьте, что модель %s установлена.
                HTTP status: %d
                """.formatted(model, response.statusCode())
            );
        }

        try {
            OllamaResponse ollamaResponse = objectMapper.readValue(response.body(), OllamaResponse.class);
            if (ollamaResponse.message() == null || ollamaResponse.message().content() == null) {
                throw new LlmClientException("LLM вернула некорректный ответ");
            }

            return ollamaResponse.message();

        } catch (JsonProcessingException e) {
            throw new LlmClientException("Не удалось обработать ответ от LLM", e);
        }
    }
}