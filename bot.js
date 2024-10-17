import { Client, GatewayIntentBits } from 'discord.js';
import { existsSync, readFileSync, readdirSync } from 'fs';
import { normalize } from 'path';
import dotenv from 'dotenv';

dotenv.config();

const client = new Client({
    intents: [
        GatewayIntentBits.Guilds,
        GatewayIntentBits.GuildMessages,
        GatewayIntentBits.MessageContent,
        GatewayIntentBits.GuildMessageReactions,
        GatewayIntentBits.GuildMessageTyping,
        GatewayIntentBits.GuildMembers,
        GatewayIntentBits.GuildScheduledEvents,
        GatewayIntentBits.GuildModeration,
        GatewayIntentBits.GuildVoiceStates,
        GatewayIntentBits.GuildIntegrations,
        GatewayIntentBits.GuildWebhooks,
        GatewayIntentBits.GuildEmojisAndStickers,
        GatewayIntentBits.GuildInvites,
        GatewayIntentBits.MessageContent,
        GatewayIntentBits.GuildMessageReactions,
        GatewayIntentBits.GuildMessages,
        GatewayIntentBits.MessageContent,
    ]
});

const AI_PROMPT = readFileSync('prompt.txt', 'utf-8')
    .replace(
        "<doclist>",
        readdirSync("./docs")
            .map((f) => `- ${f}`)
            .join("\n")
    );

const activeConversations = {}; // id: message[]

client.once('ready', () => {
    console.log(`Logged in as ${client.user.tag}!`);
});

client.on('threadCreate', (thread) => {
    if (!thread.parent) return;
    if (thread.parent.type != 15) return; // 15 is the type for a Forum Channel
    if (thread.parent.id != process.env.FORUM_PARENT_CHANNEL) return;

    console.log(`New forum post created: ${thread.name} in ${thread.parent.name}`);
    activeConversations[thread.id] = [
        {
            "role": "system",
            "content": AI_PROMPT
        },
        {
            "role": "user",
            "content": `SYSTEM MESSAGE. CHAT TITLE:\n\n${thread.name}`
        }
    ]; // Get ready for an inevitable messageCreate.
});

client.on('messageCreate', async (message) => {
    if (message.author.bot) return;
    if (!message.channel.isThread()) return;
    if (message.channel.parent.id != process.env.FORUM_PARENT_CHANNEL) return;
    if (!activeConversations[message.channel.id]) return; // AI doesn't know about this thread OR asked for help.

    console.log(`New message in thread ${message.channel.name}: ${message.content}`);

    try {
        message.channel.sendTyping();

        const conversation = activeConversations[message.channel.id];

        if (message.content.length == 0) {
            conversation.push({
                "role": "user",
                "content": "SYSTEM MESSAGE: USER UPLOADED FILE OR PICTURE. YOU CANNOT READ IT. ASK THEM TO DESCRIBE."
            });
        } else {
            conversation.push({
                "role": "user",
                "content": message.content
            });
        }

        let iter = 0;
        while (true) {
            if (iter > 10) {
                console.error("AI tried (and failed) 10 times. Abandoning this conversation.");
                delete activeConversations[message.channel.id];
                message.channel.send("TODO: HELP! FAILED TO PROCESS!");
                return;
            }

            const response = await fetch(`${process.env.AI_URL}/chat/completions`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${process.env.AI_KEY}`
                },
                body: JSON.stringify({
                    model: process.env.AI_MODEL,
                    messages: conversation
                })
            })
                .then(async (response) => {
                    if (!response.ok) {
                        throw `${response.status}: ${response.statusText}. ${await response.text()}`;
                    }
                    return response;
                })
                .then((response) => response.json());

            const text = response.choices[0].message.content;
            conversation.push(response.choices[0].message);

            console.debug(conversation);

            if (text.toLowerCase().includes("help[]")) {
                delete activeConversations[message.channel.id];
                message.channel.send("TODO: HELP!");
                break;
            } else if (text.toLowerCase().includes("documentation[")) {
                if (!/documentation\[.*\]/i.test(text)) {
                    conversation.push({
                        "role": "user",
                        "content": "SYSTEM MESSAGE: QUERY MALFORMED. TRY AGAIN."
                    });
                    continue;
                }

                const path = normalize("./docs/" + text.slice("documentation[".length, -1));
                if (!path.startsWith("docs/") && !path.startsWith("docs\\")) {
                    console.debug(path)
                    conversation.push({
                        "role": "user",
                        "content": "SYSTEM MESSAGE: QUERY INVALID/UNKNOWN PAGE. TRY AGAIN."
                    });
                    continue;
                }

                if (!existsSync(path)) {
                    console.debug(path)
                    conversation.push({
                        "role": "user",
                        "content": "SYSTEM MESSAGE: QUERY INVALID/UNKNOWN PAGE. TRY AGAIN."
                    });
                    continue;
                }

                message.channel.send(`🤔 _Reading the docs for \`${path.substring("docs/".length, path.lastIndexOf("."))}\`..._`);

                const doc = readFileSync(path, 'utf-8');
                conversation.push({
                    "role": "user",
                    "content": "SYSTEM MESSAGE: QUERY VALID. CONTENTS:\n\n" + doc
                });
                continue;
            } else {
                message.channel.send(text);
                break;
            }
        }
    } catch (e) {
        console.error(e);
        delete activeConversations[message.channel.id];
        message.channel.send("TODO: HELP! INTERNAL ERROR!");
    }
});

client.login(process.env.BOT_TOKEN);