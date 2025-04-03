package co.casterlabs.dit.types;

import co.casterlabs.rakurai.json.annotating.JsonClass;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@JsonClass(exposeAll = true)
public class ConversationFile {
    public final String type;
    public final String id;

}
