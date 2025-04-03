package co.casterlabs.dit.config;

import java.util.List;

import co.casterlabs.rakurai.json.annotating.JsonClass;

@JsonClass(exposeAll = true)
public class Config {
    public final List<ConversationSource> sources = null;

}
