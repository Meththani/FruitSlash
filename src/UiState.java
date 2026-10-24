import java.util.ArrayList;
import java.util.List;

/** Mutable user-interface state shared by {@link GamePanel} (input) and {@link GameRenderer} (drawing). */
final class UiState {

    final List<UiButton> buttons = new ArrayList<>();
    final TextInput nameInput = new TextInput(240, 492, 300, 46, ScoreEntry.MAX_NAME_LENGTH, "Your name");
    final TextInput searchInput = new TextInput(330, 92, 380, 44, ScoreEntry.MAX_NAME_LENGTH, "Type a player name");

    UiButton hovered;
    UiButton pressed;

    /** Seconds of UI animation time (keeps running in every state). */
    double animTime;

    // result screen
    String statusMessage = "";
    boolean scoreSaved;
    int savedRank = -1;

    // leaderboard screen
    int leaderboardPage;
    String searchMessage = "";
    int highlightRank = -1;
    GameState leaderboardReturn = GameState.MENU;

    // how-to-play screen
    boolean showTechInfo;
}
