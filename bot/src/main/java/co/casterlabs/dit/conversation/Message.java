package co.casterlabs.dit.conversation;

import co.casterlabs.rakurai.json.annotating.JsonClass;

@JsonClass(exposeAll = true)
public record Message(
    Role role,
    String content
) {

}
