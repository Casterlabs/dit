package co.casterlabs.dit.types;

import java.util.LinkedList;
import java.util.List;

import co.casterlabs.dit.Dit;
import co.casterlabs.rakurai.json.annotating.JsonClass;
import co.casterlabs.rakurai.json.annotating.JsonExclude;

@JsonClass(exposeAll = true)
public class Conversation {
    public final @JsonExclude long createdAt = System.currentTimeMillis();

    public final String model = Dit.AI_MODEL;

    public final ConversationFile[] files = {
            new ConversationFile("collection", Dit.AI_KNOWLEDGE)
    };

    public final List<Message> messages = new LinkedList<>();

}
