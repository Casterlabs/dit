package co.casterlabs.dit.conversation;

import java.io.Closeable;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.dit.config.AIProvider;

public interface ConversationHandle extends Closeable {

    public AIProvider ai();

    public void startThinking();

    public void stopThinking();

    /**
     * @param content may be blank or null.
     */
    public void postMessage(@Nullable String content);

    public void signalHelp();

    public void signalSolved();

}
