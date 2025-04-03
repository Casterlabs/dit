package co.casterlabs.dit;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Launcher {

    public static void main(String[] args) throws InterruptedException {
        JDA jda = JDABuilder.createDefault(Dit.BOT_TOKEN)
            .enableIntents(
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_MESSAGE_REACTIONS,
                GatewayIntent.GUILD_EXPRESSIONS,
                GatewayIntent.GUILD_MESSAGE_TYPING
            )
            .setEventManager(new AnnotatedEventManager())
            .addEventListeners(new MessageListener())
            .build();

        jda.awaitReady();
    }

}
