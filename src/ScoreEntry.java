/** An immutable leaderboard record (player name + score) with validation helpers. */
public final class ScoreEntry {

    public static final int MAX_NAME_LENGTH = 12;
    public static final int MAX_SCORE = 1_000_000;

    private final String name;
    private final int score;

    /**
     * @throws IllegalArgumentException if the name is empty after cleaning or the score is out of range
     */
    public ScoreEntry(String name, int score) {
        String clean = sanitizeName(name);
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("player name is empty or invalid");
        }
        if (!isValidScore(score)) {
            throw new IllegalArgumentException("score out of range: " + score);
        }
        this.name = clean;
        this.score = score;
    }

    /** @return a new entry, or null if the name or score is invalid (never throws). */
    public static ScoreEntry tryCreate(String name, int score) {
        try {
            return new ScoreEntry(name, score);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static boolean isAllowedNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-';
    }

    public static boolean isValidScore(int score) {
        return score >= 0 && score <= MAX_SCORE;
    }

    /**
     * Removes unsupported characters, trims, collapses repeated spaces and limits the length.
     * Tabs and newlines are removed too, so a name can never break the save-file format.
     */
    public static String sanitizeName(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length() && sb.length() < MAX_NAME_LENGTH; i++) {
            char c = raw.charAt(i);
            if (!isAllowedNameChar(c)) {
                continue;
            }
            if (c == ' ' && (sb.length() == 0 || sb.charAt(sb.length() - 1) == ' ')) {
                continue; // no leading or doubled spaces
            }
            sb.append(c);
        }
        return sb.toString().trim();
    }

    /**
     * Parses one save-file line of the form {@code name<TAB>score}.
     *
     * @return the entry, or null if the line is blank, a comment or malformed
     */
    public static ScoreEntry parseLine(String line) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null;
        }
        int tab = line.lastIndexOf('\t');
        if (tab <= 0) {
            return null;
        }
        String rawName = line.substring(0, tab).trim();
        String rawScore = line.substring(tab + 1).trim();
        if (!sanitizeName(rawName).equals(rawName)) {
            return null; // contains characters that could never have been saved by this game
        }
        try {
            return tryCreate(rawName, Integer.parseInt(rawScore));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public String toLine() {
        return name + "\t" + score;
    }

    public String getName() { return name; }
    public int getScore() { return score; }

    @Override
    public String toString() {
        return name + " (" + score + ")";
    }
}
