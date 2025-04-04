package co.casterlabs.dit.conversation;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Conversation implements Closeable {
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public final long createdAt = System.currentTimeMillis();
    public final List<Message> messages = new LinkedList<>();
    public final ConversationHandle handle;

    public Conversation(ConversationHandle handle) throws IOException, InterruptedException {
        this.handle = handle;
    }

    public void process(String userId, String content) {
        this.executorService.submit(() -> {
            this.messages.add(new Message(Role.user, content));
            try {
                this.handle.ai().process(userId, this);
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
