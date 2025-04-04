package co.casterlabs.dit.config;

import java.io.Closeable;
import java.io.IOException;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.dit.config.ai.OpenWebUIProvider;
import co.casterlabs.dit.conversation.Conversation;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.annotating.JsonClass;
import co.casterlabs.rakurai.json.annotating.JsonSerializer;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import co.casterlabs.rakurai.json.validation.JsonValidationException;
import lombok.NonNull;

@JsonClass(serializer = AIProviderSerializer.class)
public interface AIProvider extends Closeable {

    public void load(@NonNull JsonObject config) throws JsonValidationException, JsonParseException;

    public Type type();

    public void process(String userId, Conversation conversation) throws IOException, InterruptedException;

    public static enum Type {
        OPENWEBUI,
    }

}

class AIProviderSerializer implements JsonSerializer<AIProvider> {

    @Override
    public @Nullable AIProvider deserialize(@NonNull JsonElement value, @NonNull Class<?> type, @NonNull Rson rson) throws JsonParseException {
        JsonObject config = (JsonObject) value;

        AIProvider.Type sourceType = AIProvider.Type.valueOf(config.getString("type"));
        AIProvider result = switch (sourceType) {
            case OPENWEBUI -> new OpenWebUIProvider();
            default -> null;
        };

        if (result != null) {
            result.load(config);
        }

        return result;
    }

}
