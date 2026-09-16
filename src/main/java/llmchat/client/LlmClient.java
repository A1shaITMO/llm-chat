package llmchat.client;

import llmchat.model.ChatMessage;

import java.util.List;

public interface LlmClient {
    ChatMessage send(List<ChatMessage> messages);
}