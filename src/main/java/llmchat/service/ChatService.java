package llmchat.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import llmchat.client.LlmClient;
import llmchat.model.ChatMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {
    private final LlmClient llmClient;
    private final String systemPrompt;

    private final List<ChatMessage> history = new ArrayList<>();

    public ChatService(LlmClient llmClient) {
        this.llmClient = llmClient;
        this.systemPrompt = loadSystemPrompt();
    }

    public String sendMessage(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("Сообщение не может быть пустым");
        }

        ChatMessage userMessageObject = new ChatMessage("user", userMessage);
        history.add(userMessageObject);
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", systemPrompt));
        messages.addAll(history);

        try {
            ChatMessage assistantMessage = llmClient.send(messages);
            history.add(assistantMessage);
            return assistantMessage.content();
        } catch (RuntimeException e) {
            history.removeLast();
            throw e;
        }
    }

    public void clearHistory() {
        history.clear();
    }

    private String loadSystemPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource("system-prompt.txt");
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось загрузить system-prompt.txt", e);
        }
    }

    public List<ChatMessage> getHistory() {
        return List.copyOf(history);
    }
}