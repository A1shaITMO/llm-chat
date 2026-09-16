package llmchat.cli;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import llmchat.client.LlmClientException;
import llmchat.service.ChatService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Component
public class ChatCli implements CommandLineRunner {
    private final ChatService chatService;

    public ChatCli(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public void run(String... args) {
        try (BufferedReader reader = new BufferedReader( new InputStreamReader(System.in))) {
            initialPrint();

            while (true) {
                System.out.print("Вы: ");

                String input = reader.readLine();
                if (input == null) break;
                input = input.trim();

                if (input.equalsIgnoreCase("/exit")) break;

                if (input.equalsIgnoreCase("/clear")) {
                    chatService.clearHistory();
                    System.out.println("История диалога очищена.");
                    continue;
                }

                try {
                    String response = chatService.sendMessage(input);
                    System.out.println("Assistant: " + response);
                } catch (LlmClientException | IllegalArgumentException e) {
                    System.out.println("Ошибка: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Ошибка чтения из консоли: " + e.getMessage());
        }
    }

    private void initialPrint() {
        System.out.println("==============================");
        System.out.println("          LLM Chat :P         ");
        System.out.println("==============================");
        System.out.println("Команды:");
        System.out.println("  /clear — очистить историю");
        System.out.println("  /exit  — выйти");
        System.out.println();
    }
}