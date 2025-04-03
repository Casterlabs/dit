package co.casterlabs.dit.types;

import co.casterlabs.rakurai.json.annotating.JsonClass;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@JsonClass(exposeAll = true)
public class Message {
    public final Role role;
    public final String content;

}
