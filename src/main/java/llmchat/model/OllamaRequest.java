package llmchat.model;

import java.util.List;

public record OllamaRequest(String model, List<ChatMessage> messages, boolean stream) {

}