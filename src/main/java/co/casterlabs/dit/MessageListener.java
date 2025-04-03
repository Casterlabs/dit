package co.casterlabs.dit;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import co.casterlabs.dit.types.Conversation;
import co.casterlabs.dit.types.Message;
import co.casterlabs.dit.types.Role;
import co.casterlabs.rakurai.json.element.JsonObject;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

public class MessageListener {
    private static Map<String, Conversation> conversations = new HashMap<>();

    @SubscribeEvent
    public void onThreadCreate(ChannelCreateEvent event) {
        if (!event.isFromType(ChannelType.GUILD_PUBLIC_THREAD) && !event.isFromType(ChannelType.GUILD_PRIVATE_THREAD)) return;

        ThreadChannel channel = event.getChannel().asThreadChannel();
        if (channel.getParentChannel().getIdLong() != Dit.FORUM_PARENT_CHANNEL) return;

        String conversationId = channel.getId();
        Conversation conversation = new Conversation();

        conversation.messages.add(new Message(Role.system, Dit.PROMPT));
        conversation.messages.add(new Message(Role.system, "Post title: " + channel.getName()));

        channel.sendMessage(Dit.HEADER).submit();
        channel.sendMessage("-------------------------------------").submit();

        System.out.printf("Conversation started! %s\n", conversationId);

        conversations.put(conversationId, conversation);
    }

    @SubscribeEvent
    public void onThreadDelete(ChannelDeleteEvent event) {
        conversations.remove(event.getChannel().getId());
    }

    @SubscribeEvent
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getMessage().getAuthor().isBot()) return;

        String conversationId = event.getChannel().getId();
        Conversation conversation = conversations.get(conversationId);
        if (conversation == null) return;

        ThreadChannel channel = event.getChannel().asThreadChannel();

        System.out.printf("Got message: %s\n", event.getMessage().getContentRaw());
        conversation.messages.add(new Message(Role.user, event.getMessage().getContentRaw()));

        String responseMessage = null;
        {
            int iter = 0;
            while (true) {
                if (iter > 10) {
                    System.err.println("AI failed to generate a response!");
                    break;
                }
                iter++;

                event.getChannel().sendTyping().submit();

                try {
                    HttpResponse<JsonObject> response = Dit.callAI(conversation);
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        responseMessage = response.body()
                            .getArray("choices")
                            .getObject(0)
                            .getObject("message")
                            .getString("content");
                        System.out.println(response.body());
                        break;
                    }
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        }

        conversation.messages.add(new Message(Role.assistant, responseMessage));

        boolean isHelpResponse = responseMessage.toLowerCase().contains(Dit.HELP_TOKEN);
        boolean isSolvedResponse = responseMessage.toLowerCase().contains(Dit.SOLVED_TOKEN);

        if (isHelpResponse) {
            conversations.remove(conversationId);
            responseMessage = Dit.HELP_PATTERN.matcher(responseMessage).replaceAll(Dit.FORUM_HELP_PING);
        } else if (isSolvedResponse) {
            conversations.remove(conversationId);
            responseMessage = Dit.SOLVED_PATTERN.matcher(responseMessage).replaceAll("");
        }

        if (!responseMessage.isBlank()) {
            channel.sendMessage(responseMessage).submit();
        }

        if (isSolvedResponse) {
            channel.getManager().setLocked(true).submit();
        }
    }

}
