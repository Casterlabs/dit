package co.casterlabs.dit;

import java.util.regex.Pattern;

public class Dit {
    public static final String HELP_TOKEN = "help[]";
    public static final Pattern HELP_PATTERN = Pattern.compile("help\\[\\]", Pattern.CASE_INSENSITIVE);

    public static final String SOLVED_TOKEN = "solved[]";
    public static final Pattern SOLVED_PATTERN = Pattern.compile("solved\\[\\]", Pattern.CASE_INSENSITIVE);

    public static final String IMAGE_EXAMPLE = "[image /path/to/image.png]";
    public static final Pattern IMAGE_PATTERN = Pattern.compile("\\[image .+?\\]", Pattern.CASE_INSENSITIVE);
    public static final int IMAGE_SUBSTR_START = "[image ".length();
    public static final int IMAGE_SUBSTR_END_DELTA = "]".length();

    public static final int CONVERSATION_CUTOFF_THRESHOLD = Integer.parseInt(System.getenv().getOrDefault("CONVERSATION_CUTOFF_THRESHOLD", "150"));
    public static final long CONVERSATION_MAXAGE_MINUTES = Long.parseLong(System.getenv().getOrDefault("CONVERSATION_MAXAGE_MINUTES", "360"));

}
