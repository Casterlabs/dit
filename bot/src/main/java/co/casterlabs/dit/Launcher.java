package co.casterlabs.dit;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import co.casterlabs.dit.config.AIProvider;
import co.casterlabs.dit.config.ConversationSource;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonObject;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class Launcher {
    private static final File CONFIG_FOLDER = new File("./config");

    private static Map<String, ConversationSource> configs = new HashMap<>();

    public static void main(String[] args) throws IOException {
        reload();

        WatchService watcher = FileSystems.getDefault().newWatchService();
        CONFIG_FOLDER.toPath().register(
            watcher,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE
        );

        while (true) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException e) {
                return;
            }
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                reload();
            }
            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
    }

    private static void reload() throws IOException {
        Set<String> present = new HashSet<>();

        for (File file : CONFIG_FOLDER.listFiles()) {
            if (file.isDirectory()) continue;
            if (!file.getName().endsWith(".json")) continue;

            present.add(file.getName());

            String configContents = Files.readString(file.toPath());
            if (configs.containsKey(file.getName())) {
                JsonObject newConfig = Rson.DEFAULT.fromJson(configContents, JsonObject.class);
                AIProvider newAi = Rson.DEFAULT.fromJson(newConfig.get("ai"), AIProvider.class);

                ConversationSource source = configs.get(file.getName());
                source.load(newConfig, newAi);
            } else {
                ConversationSource source = Rson.DEFAULT.fromJson(configContents, ConversationSource.class);
                configs.put(file.getName(), source);
            }
        }

        for (String configName : configs.keySet()) {
            if (!present.contains(configName)) {
                ConversationSource source = configs.remove(configName);
                source.close();
            }
        }

        FastLogger.logStatic("Loaded %s configs", configs.size());
    }

}
