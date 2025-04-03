package co.casterlabs.dit;

import java.util.regex.Pattern;

import co.casterlabs.dit.config.Config;

public class Dit {
    public static final String HELP_TOKEN = "help[]";
    public static final Pattern HELP_PATTERN = Pattern.compile("help\\[\\]", Pattern.CASE_INSENSITIVE);

    public static final String SOLVED_TOKEN = "solved[]";
    public static final Pattern SOLVED_PATTERN = Pattern.compile("solved\\[\\]", Pattern.CASE_INSENSITIVE);

    public static Config config;

}
