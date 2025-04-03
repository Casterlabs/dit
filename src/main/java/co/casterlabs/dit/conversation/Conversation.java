package co.casterlabs.dit.conversation;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.casterlabs.dit.config.AIProvider;

public class Conversation implements Closeable {
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final AIProvider ai;

    public final long createdAt = System.currentTimeMillis();
    public final List<Message> messages = new LinkedList<>();
    public final ConversationHandle handle;

    public Conversation(ConversationHandle handle, AIProvider ai) throws IOException, InterruptedException {
        this.handle = handle;
        this.ai = ai;
        this.ai.begin(this);
    }

    public void process(String userId, String content) {
        this.executorService.submit(() -> {
            this.messages.add(new Message(Role.user, content));
            try {
                this.ai.process(userId, this);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void close() throws IOException {
        this.handle.close();
    }

}
