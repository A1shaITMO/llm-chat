package llmchat;

import llmchat.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import llmchat.client.LlmClient;
import llmchat.model.ChatMessage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatServiceTest {

    private LlmClient llmClient;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        llmClient = mock(LlmClient.class);
        chatService = new ChatService(llmClient);
    }

    @Test
    void shouldSaveAssistantResponseToHistory() {
        when(llmClient.send(anyList()))
                .thenReturn(new ChatMessage("assistant", "Привет!"));

        String response = chatService.sendMessage("Привет");

        assertEquals("Привет!", response);
        List<ChatMessage> history = chatService.getHistory();
        assertEquals(2, history.size());
        assertEquals("user", history.get(0).role());
        assertEquals("Привет", history.get(0).content());
        assertEquals("assistant", history.get(1).role());
        assertEquals("Привет!", history.get(1).content());
    }

    @Test
    void shouldPassPreviousMessagesToLlm() {

        when(llmClient.send(anyList()))
                .thenReturn(new ChatMessage("assistant", "Ответ"));

        chatService.sendMessage("Первое сообщение");
        chatService.sendMessage("Второе сообщение");

        verify(llmClient, times(2)).send(anyList());
        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(llmClient, times(2)).send(captor.capture());

        List<ChatMessage> secondRequest = captor.getAllValues().get(1);

        assertEquals(4, secondRequest.size());
        assertEquals("system", secondRequest.get(0).role());
        assertEquals("Первое сообщение", secondRequest.get(1).content());
        assertEquals("Ответ", secondRequest.get(2).content());
        assertEquals("Второе сообщение", secondRequest.get(3).content());
    }

    @Test
    void shouldClearHistory() {
        when(llmClient.send(anyList()))
                .thenReturn(new ChatMessage("assistant", "Ответ"));

        chatService.sendMessage("Привет");
        assertFalse(chatService.getHistory().isEmpty());
        chatService.clearHistory();
        assertTrue(chatService.getHistory().isEmpty());
    }

    @Test
    void shouldRejectBlankMessage() {
        assertThrows(IllegalArgumentException.class, () -> chatService.sendMessage("   "));
        verifyNoInteractions(llmClient);
    }
}