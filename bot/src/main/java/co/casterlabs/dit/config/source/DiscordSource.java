package co.casterlabs.dit.config.source;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.dit.Dit;
import co.casterlabs.dit.config.AIProvider;
import co.casterlabs.dit.config.ConversationSource;
import co.casterlabs.dit.conversation.Conversation;
import co.casterlabs.dit.conversation.ConversationHandle;
import co.casterlabs.dit.conversation.Message;
import co.casterlabs.dit.conversation.Role;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.annotating.JsonClass;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import co.casterlabs.rakurai.json.validation.JsonValidationException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateLockedEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class DiscordSource implements ConversationSource {
    public static final String HELP_PREFIX = "🚨 ";
    public static final String SOLVED_PREFIX = "✅ ";

    private FastLogger logger;

    private Map<String, Conversation> conversations = new HashMap<>();

    private String previousBotToken;
    private JDA jda;

    private Thread conversationEndThread;

    private AIProvider ai;
    private Config config;

    @JsonClass(exposeAll = true)
    private static class Config {
        public final String botToken = null;
        public final Long forumParentChannel = null;
        public final String helpPing = null;
        public final String header = null;
        public final String imageBaseUrl = null;
    }

    @Override
    public void load(@NonNull JsonObject config, @NonNull AIProvider ai) throws JsonValidationException, JsonParseException {
        this.conversationEndThread = null;
        this.config = Rson.DEFAULT.fromJson(config, Config.class);

        // If the bot token hasn't changed, don't reload.
        // Changing the bot token is considered terminal and will force a full reload.
        if (this.previousBotToken != null) {
            if (this.previousBotToken.equals(this.config.botToken)) {
                this.logger.info("Reloading...");
            } else {
                this.close();
                this.logger = null;
            }
        }

        if (this.logger == null) {
            this.logger = new FastLogger("DiscordSource@" + this.config.botToken.hashCode());
            this.logger.info("Loading...");
        }

        this.conversationEndThread = Thread.ofPlatform().name("Discord Conversation End Thread").start(() -> {
            Thread currentThread = Thread.currentThread();

            while (this.conversationEndThread == currentThread) {
                try {
                    TimeUnit.SECONDS.sleep(30);
                } catch (InterruptedException ignored) {}

                for (Conversation conversation : this.conversations.values().toArray(new Conversation[0])) {
                    if (System.currentTimeMillis() - conversation.createdAt > TimeUnit.MINUTES.toMillis(Dit.CONVERSATION_MAXAGE_MINUTES)) {
                        try {
                            conversation.handle.signalHelp();
                            conversation.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        AIProvider oldAI = this.ai;
        this.ai = ai; // Seamless reload.
        if (oldAI != null) {
            try {
                oldAI.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (this.jda == null) {
            this.previousBotToken = this.config.botToken;
            this.jda = JDABuilder.createDefault(this.config.botToken)
                .enableIntents(
                    GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.GUILD_MESSAGE_REACTIONS,
                    GatewayIntent.GUILD_EXPRESSIONS,
                    GatewayIntent.GUILD_MESSAGE_TYPING
                )
                .setEventManager(new AnnotatedEventManager())
                .addEventListeners(new MessageListener())
                .build();

            try {
                this.jda.awaitReady();
                this.logger.info("Ready!");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            this.jda.getThreadChannels()
                .stream()
                .filter((c -> c.getParentChannel().getIdLong() == this.config.forumParentChannel))
                .filter((c) -> !c.getName().startsWith(HELP_PREFIX))
                .filter((c) -> !c.getName().startsWith(SOLVED_PREFIX))
                .filter((c) -> !c.isLocked())
                .forEach((channel) -> {
                    try {
                        this.startWithHistory(channel);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                });
        }
    }

    @Override
    public Type type() {
        return Type.DISCORD;
    }

    @Override
    public void close() {
        this.logger.info("Shutting down...");

        if (this.jda != null) {
            this.jda.shutdown();
            this.jda = null;
        }

        if (this.ai != null) {
            try {
                this.ai.close();
            } catch (IOException ignored) {}
        }

        this.conversations.forEach((k, v) -> {
            try {
                v.close();
            } catch (IOException ignored) {}
        });
        this.conversations.clear();
    }

    private void startWithHistory(ThreadChannel channel) throws IOException, InterruptedException {
        String conversationId = channel.getId();
        Conversation conversation = new Conversation(new DiscordConversationHandle(channel));

        conversation.messages.add(new Message(Role.system, "Post title: " + channel.getName()));

        // Get entire message history. Add it as an assistant message if the author is a
        // bot, otherwise add it as a user message
        channel.getHistory().retrievePast(100).complete().forEach((message) -> {
            if (message.getAuthor().isBot()) {
                conversation.messages.add(new Message(Role.assistant, message.getContentDisplay()));
            } else {
                conversation.messages.add(new Message(Role.user, message.getContentDisplay()));
            }
        });

        logger.info("Conversation resumed! %s", conversationId);
        this.conversations.put(conversationId, conversation);
    }

    private class MessageListener {

        @SubscribeEvent
        public void onThreadCreate(ChannelCreateEvent event) {
            if (!event.isFromType(ChannelType.GUILD_PUBLIC_THREAD) && !event.isFromType(ChannelType.GUILD_PRIVATE_THREAD)) return;

            ThreadChannel channel = event.getChannel().asThreadChannel();
            if (channel.getParentChannel().getIdLong() != config.forumParentChannel) return;

            try {
                String conversationId = channel.getId();
                Conversation conversation = new Conversation(new DiscordConversationHandle(channel));

                conversation.messages.add(new Message(Role.user, "Post title: " + channel.getName()));

                channel.sendMessage(config.header).complete();
                channel.sendMessage("-------------------------------------").complete();

                logger.info("Conversation started! %s", conversationId);
                conversations.put(conversationId, conversation);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        @SubscribeEvent
        public void onThreadLock(ChannelUpdateLockedEvent event) {
            ThreadChannel channel = event.getChannel().asThreadChannel();
            Conversation conversation = conversations.remove(channel.getId());

            if (channel.isLocked()) {
                if (conversation == null) return;
                try {
                    conversation.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                if (channel.getName().startsWith(SOLVED_PREFIX)) {
                    channel.getManager().setName(channel.getName().substring(SOLVED_PREFIX.length())).submit();
                }

                try {
                    startWithHistory(channel);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        @SubscribeEvent
        public void onThreadDelete(ChannelDeleteEvent event) {
            Conversation conversation = conversations.remove(event.getChannel().getId());
            if (conversation == null) return;

            try {
                conversation.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @SubscribeEvent
        public void onMessageReceived(MessageReceivedEvent event) {
            if (event.getMessage().getAuthor().isBot()) return;

            String conversationId = event.getChannel().getId();
            Conversation conversation = conversations.get(conversationId);
            if (conversation == null) return;

            net.dv8tion.jda.api.entities.Message reference = event.getMessage().getReferencedMessage();
            if (reference != null) {
                if (reference.getAuthor().isBot()) {
                    conversation.messages.add(new Message(Role.user, "User is replying directly to your message: " + reference.getContentRaw()));
                } else {
                    conversation.messages.add(new Message(Role.user, "User is replying directly to their own message: " + reference.getContentRaw()));
                }
            }

            conversation.process(event.getAuthor().getId(), event.getMessage().getContentRaw());
        }

    }

    @RequiredArgsConstructor
    private class DiscordConversationHandle implements ConversationHandle {
        private final ThreadChannel channel;

        @Override
        public AIProvider ai() {
            return ai;
        }

        @Override
        public void startThinking() {
            this.channel.sendTyping().submit();
        }

        @Override
        public void stopThinking() {
            // NOOP
        }

        @Override
        public void postMessage(@Nullable String content) {
            if (content == null || content.isBlank()) return;

            Matcher m = Dit.IMAGE_PATTERN.matcher(content);
            while (m.find()) {
                String imageTag = m.group();

                if (imageTag.equalsIgnoreCase(Dit.IMAGE_EXAMPLE)) {
                    content = content.replace(imageTag, "");
                    continue;
                }

                String imageUrl = imageTag.substring(Dit.IMAGE_SUBSTR_START, imageTag.length() - Dit.IMAGE_SUBSTR_END_DELTA);
                if (config.imageBaseUrl != null) {
                    imageUrl = config.imageBaseUrl + imageUrl;
                }

                content = content.replace(imageTag, String.format("[Image](%s)", imageUrl));
            }

            this.channel.sendMessage(content).submit();
        }

        @Override
        public void signalHelp() {
            this.channel.getManager().setName(HELP_PREFIX + this.channel.getName()).submit();
            this.channel.sendMessage(config.helpPing).submit();
            this.close();
        }

        @Override
        public void signalSolved() {
            this.channel.getManager().setName(SOLVED_PREFIX + this.channel.getName()).submit();
            this.channel.getManager().setLocked(true).submit();
            this.close();
        }

        @Override
        public void close() {
            Conversation conversation = conversations.remove(this.channel.getId());
            if (conversation == null) return;
            logger.info("Conversation ended! %s", this.channel.getId());

            try {
                conversation.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
