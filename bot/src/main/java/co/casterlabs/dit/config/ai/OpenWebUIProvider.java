package co.casterlabs.dit.config.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;

import co.casterlabs.dit.Dit;
import co.casterlabs.dit.config.AIProvider;
import co.casterlabs.dit.conversation.Conversation;
import co.casterlabs.dit.conversation.Message;
import co.casterlabs.dit.conversation.Role;
import co.casterlabs.dit.util.RsonBodyHandler;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.annotating.JsonClass;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import co.casterlabs.rakurai.json.validation.JsonValidationException;
import lombok.NonNull;

public class OpenWebUIProvider implements AIProvider {
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private Config config;

    @JsonClass(exposeAll = true)
    private static class Config {
        public final String url = null;
        public final String apiKey = null;
        public final String model = null;
        public final JsonObject additionalParameters = null;
        public final String prompt = null;
    }

    @Override
    public void load(@NonNull JsonObject config) throws JsonValidationException, JsonParseException {
        this.config = Rson.DEFAULT.fromJson(config, Config.class);
    }

    @Override
    public Type type() {
        return Type.OPENWEBUI;
    }

    @Override
    public void process(String userId, Conversation conversation) {
        String responseMessage = null;
        {
            int iter = 0;
            while (true) {
                if (iter > 50) {
                    System.err.println("AI failed to generate a response!");
                    break;
                }
                iter++;

                conversation.handle.startThinking();

                try {
                    HttpResponse<JsonObject> response = this.call(conversation);
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        responseMessage = response.body()
                            .getArray("choices")
                            .getObject(0)
                            .getObject("message")
                            .getString("content");
//                        System.out.println(response.body());
                        break;
                    }
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                } finally {
                    conversation.handle.stopThinking();
                }
            }
        }

        conversation.messages.add(new Message(Role.assistant, responseMessage));

        boolean isHelpResponse = responseMessage.toLowerCase().contains(Dit.HELP_TOKEN);
        boolean isSolvedResponse = responseMessage.toLowerCase().contains(Dit.SOLVED_TOKEN);

        responseMessage = Dit.HELP_PATTERN.matcher(responseMessage).replaceAll("");
        responseMessage = Dit.SOLVED_PATTERN.matcher(responseMessage).replaceAll("");

        conversation.handle.postMessage(responseMessage);

        if (isHelpResponse) {
            conversation.handle.signalHelp();
        } else if (isSolvedResponse) {
            conversation.handle.signalSolved();
        }
    }

    private HttpResponse<JsonObject> call(Conversation conversation) throws IOException, InterruptedException {
        JsonArray messages = new JsonArray();
        messages.add(Rson.DEFAULT.toJson(new Message(Role.system, this.config.prompt)));
        conversation.messages.forEach((m) -> messages.add(Rson.DEFAULT.toJson(m)));

        JsonObject body = new JsonObject()
            .put("model", this.config.model)
            .put("messages", messages);

        if (this.config.additionalParameters != null) {
            this.config.additionalParameters.forEach((e) -> body.put(e.getKey(), e.getValue()));
        }

        return this.httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(this.config.url + "/chat/completions"))
                .POST(BodyPublishers.ofString(body.toString()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + this.config.apiKey)
                .build(),
            RsonBodyHandler.of(JsonObject.class)
        );
    }

    @Override
    public void close() throws IOException {
        this.httpClient.close();
    }

}
