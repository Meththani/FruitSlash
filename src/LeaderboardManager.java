import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores the leaderboard on disk and ranks it with a hand-written Bubble Sort.
 * Player lookups use a hand-written Linear Search.
 *
 * <p>The save file is a plain UTF-8 text file with one {@code name<TAB>score} line per entry.
 * Missing, empty or damaged files never crash the game: unreadable lines are skipped.
 */
public class LeaderboardManager {

    public static final String FILE_NAME = "leaderboard.txt";
    public static final int MAX_ENTRIES = 200;
    private static final String HEADER = "# Fruit Slash leaderboard (name<TAB>score)";

    private final Path file;
    private final ArrayList<ScoreEntry> entries = new ArrayList<>();
    private int skippedLines;
    private String lastError = "";

    /** Uses the default save location (see {@link #defaultSavePath()}). */
    public LeaderboardManager() {
        this(defaultSavePath());
    }

    /** Uses an explicit file (used by the tests). */
    public LeaderboardManager(Path file) {
        this.file = file;
        load();
    }

    /**
     * Default save file: {@code data/leaderboard.txt} relative to the folder the game is started from.
     * If that folder cannot be created, falls back to a hidden folder in the user's home directory.
     * No machine-specific absolute path is ever hard-coded.
     */
    public static Path defaultSavePath() {
        Path local = Paths.get("data");
        try {
            Files.createDirectories(local);
            if (Files.isWritable(local)) {
                return local.resolve(FILE_NAME);
            }
        } catch (IOException | RuntimeException ex) {
            // fall through to the home-directory fallback
        }
        String home = System.getProperty("user.home", ".");
        return Paths.get(home, ".fruit-slash", FILE_NAME);
    }

    // ------------------------------------------------------------------ persistence

    private void load() {
        entries.clear();
        skippedLines = 0;
        lastError = "";
        if (file == null || !Files.isRegularFile(file)) {
            return; // missing file: start with an empty leaderboard
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException ex) {
            lastError = "Could not read leaderboard file";
            return; // unreadable (or not valid UTF-8): treat as empty, do not crash
        }
        for (String line : lines) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }
            ScoreEntry entry = ScoreEntry.parseLine(line);
            if (entry == null) {
                skippedLines++;
            } else if (entries.size() < MAX_ENTRIES * 2) {
                entries.add(entry);
            }
        }
        sortAndTrim();
    }

    /**
     * Writes all entries to a temporary file first and then moves it over the real file, so a crash
     * in the middle of saving can never destroy the existing leaderboard.
     *
     * @return true on success
     */
    public boolean save() {
        if (file == null) {
            return false;
        }
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            List<String> lines = new ArrayList<>();
            lines.add(HEADER);
            for (ScoreEntry entry : entries) {
                lines.add(entry.toLine());
            }
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.write(temp, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            lastError = "";
            return true;
        } catch (IOException | RuntimeException ex) {
            lastError = "Could not save leaderboard file";
            return false;
        }
    }

    // ------------------------------------------------------------------ public operations

    /**
     * Validates and adds a score, re-ranks the leaderboard and saves it. Previous entries are kept.
     *
     * @return the stored entry, or null if the name/score is invalid (nothing is changed then)
     */
    public ScoreEntry addScore(String rawName, int score) {
        ScoreEntry entry = ScoreEntry.tryCreate(rawName, score);
        if (entry == null) {
            return null;
        }
        entries.add(entry); // appended at the end: older entries win ties because the sort is stable
        sortAndTrim();
        save();
        return entry;
    }

    /** @return a copy of the entries, ranked from highest to lowest score. */
    public ScoreEntry[] getRankedEntries() {
        return entries.toArray(new ScoreEntry[0]);
    }

    public int size() {
        return entries.size();
    }

    public int getBestScore() {
        return entries.isEmpty() ? 0 : entries.get(0).getScore();
    }

    /** @return 1-based rank of this exact entry object, or -1 if it is not on the board. */
    public int getRankOf(ScoreEntry entry) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i) == entry) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * Looks a player up by name using {@link #linearSearchPlayer(ScoreEntry[], int, String)}.
     *
     * @return 1-based rank of the best-ranked entry of that player, or -1 if the name is not found
     */
    public int searchPlayerRank(String name) {
        ScoreEntry[] ranked = getRankedEntries();
        int index = linearSearchPlayer(ranked, ranked.length, name);
        return index < 0 ? -1 : index + 1;
    }

    public int getSkippedLineCount() { return skippedLines; }
    public String getLastError() { return lastError; }
    public Path getFile() { return file; }

    private void sortAndTrim() {
        ScoreEntry[] array = entries.toArray(new ScoreEntry[0]);
        bubbleSortScoresDescending(array, array.length);
        entries.clear();
        for (int i = 0; i < array.length && i < MAX_ENTRIES; i++) {
            entries.add(array[i]);
        }
    }

    // ------------------------------------------------------------------ algorithms

    /** Sorts the whole array from highest to lowest score. */
    public static void bubbleSortScoresDescending(ScoreEntry[] data) {
        if (data != null) {
            bubbleSortScoresDescending(data, data.length);
        }
    }

    /**
     * Bubble Sort (highest score first) on the first {@code length} elements.
     *
     * <p>Each pass compares neighbouring elements and swaps them when the left one has a LOWER
     * score, so the smallest value "bubbles" to the end. After pass k the last k elements are final,
     * so the inner loop shrinks. If a pass makes no swap the array is already sorted and we stop early.
     *
     * <p>Equal scores are never swapped (strict {@code <}), which makes the sort stable: among equal
     * scores the entry that was saved earlier keeps the better rank.
     *
     * <p>Time: O(n^2) comparisons and swaps in the worst case (reverse order), O(n) when already
     * sorted. Extra memory: O(1).
     */
    public static void bubbleSortScoresDescending(ScoreEntry[] data, int length) {
        if (data == null) {
            return;
        }
        int n = Math.max(0, Math.min(length, data.length));
        for (int pass = 0; pass < n - 1; pass++) {
            boolean swapped = false;
            for (int j = 0; j < n - 1 - pass; j++) {
                if (scoreOf(data[j]) < scoreOf(data[j + 1])) {
                    ScoreEntry temp = data[j];
                    data[j] = data[j + 1];
                    data[j + 1] = temp;
                    swapped = true;
                }
            }
            if (!swapped) {
                break; // already sorted
            }
        }
    }

    private static int scoreOf(ScoreEntry entry) {
        return entry == null ? Integer.MIN_VALUE : entry.getScore(); // null slots sink to the end
    }

    /**
     * Linear (sequential) search for a player name, ignoring upper/lower case and surrounding spaces.
     * Visits the elements one by one from index 0; because the leaderboard is ranked, the first match
     * is that player's best score.
     *
     * <p>Time: O(n) in the worst case (name missing or last); O(1) best case.
     *
     * @return index of the first matching entry, or -1 when there is none (or the name is blank)
     */
    public static int linearSearchPlayer(ScoreEntry[] data, int length, String name) {
        if (data == null || name == null) {
            return -1;
        }
        String wanted = name.trim();
        if (wanted.isEmpty()) {
            return -1;
        }
        int n = Math.max(0, Math.min(length, data.length));
        for (int i = 0; i < n; i++) {
            if (data[i] != null && data[i].getName().equalsIgnoreCase(wanted)) {
                return i;
            }
        }
        return -1;
    }
}
