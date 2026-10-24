/**
 * Every screen / phase of the game. The engine only allows the transitions listed in
 * {@link #canTransitionTo(GameState)}, so an invalid jump (for example MENU -> PAUSED)
 * is rejected instead of corrupting the game.
 */
public enum GameState {
    MENU,
    HOW_TO_PLAY,
    LEADERBOARD,
    PLAYING,
    PAUSED,
    LEVEL_TRANSITION,
    GAME_OVER,
    VICTORY;

    /** @return true when moving from this state to {@code next} is a legal transition. */
    public boolean canTransitionTo(GameState next) {
        if (next == null || next == this) {
            return false;
        }
        switch (this) {
            case MENU:
                return next == PLAYING || next == HOW_TO_PLAY || next == LEADERBOARD;
            case HOW_TO_PLAY:
                return next == MENU;
            case LEADERBOARD:
                return next == MENU || next == GAME_OVER || next == VICTORY;
            case PLAYING:
                return next == PAUSED || next == LEVEL_TRANSITION
                        || next == GAME_OVER || next == VICTORY;
            case PAUSED:
                return next == PLAYING || next == MENU;
            case LEVEL_TRANSITION:
                return next == PLAYING;
            case GAME_OVER:
            case VICTORY:
                return next == PLAYING || next == MENU || next == LEADERBOARD;
            default:
                return false;
        }
    }
}
