package co.casterlabs.dit.config;

import java.io.Closeable;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.dit.config.source.DiscordSource;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.annotating.JsonClass;
import co.casterlabs.rakurai.json.annotating.JsonSerializer;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import co.casterlabs.rakurai.json.validation.JsonValidationException;
import lombok.NonNull;

@JsonClass(serializer = ConversationSourceSerializer.class)
public interface ConversationSource extends Closeable {

    public void load(@NonNull JsonObject config, @NonNull AIProvider ai) throws JsonValidationException, JsonParseException;

    public Type type();

    public static enum Type {
        DISCORD,
    }

}

class ConversationSourceSerializer implements JsonSerializer<ConversationSource> {

    @Override
    public @Nullable ConversationSource deserialize(@NonNull JsonElement value, @NonNull Class<?> type, @NonNull Rson rson) throws JsonParseException {
        JsonObject config = (JsonObject) value;

        ConversationSource.Type sourceType = ConversationSource.Type.valueOf(config.getString("type"));
        ConversationSource result = switch (sourceType) {
            case DISCORD -> new DiscordSource();
            default -> null;
        };

        if (result != null) {
            AIProvider ai = Rson.DEFAULT.fromJson(config.get("ai"), AIProvider.class);
            result.load(config, ai);
        }

        return result;
    }

}
