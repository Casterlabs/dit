package co.casterlabs.dit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import co.casterlabs.dit.config.Config;
import co.casterlabs.rakurai.json.Rson;

public class Launcher {
    private static final File CONFIG_FILE = new File("config.json");

    public static void main(String[] args) throws IOException {
        String configContents = Files.readString(CONFIG_FILE.toPath());
        Dit.config = Rson.DEFAULT.fromJson(configContents, Config.class);
    }

}
