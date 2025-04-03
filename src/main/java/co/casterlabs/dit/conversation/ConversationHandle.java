package co.casterlabs.dit.conversation;

import java.io.Closeable;

import org.jetbrains.annotations.Nullable;

public interface ConversationHandle extends Closeable {

    public void startThinking();

    public void stopThinking();

    /**
     * @param content may be blank or null.
     */
    public void postMessage(@Nullable String content);

    public void signalHelp();

    public void signalSolved();

}
