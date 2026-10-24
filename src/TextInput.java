import java.awt.geom.Rectangle2D;

/** A tiny single-line text box (player name, leaderboard search) drawn by {@link GameRenderer}. */
public class TextInput {

    private final StringBuilder text = new StringBuilder();
    private final int maxLength;
    private final Rectangle2D.Double bounds;
    private final String placeholder;
    private boolean focused;

    public TextInput(double x, double y, double width, double height, int maxLength, String placeholder) {
        this.bounds = new Rectangle2D.Double(x, y, width, height);
        this.maxLength = maxLength;
        this.placeholder = placeholder;
    }

    /** Appends a character if it is allowed in names and there is room. */
    public boolean typeChar(char c) {
        if (text.length() >= maxLength || !ScoreEntry.isAllowedNameChar(c)) {
            return false;
        }
        text.append(c);
        return true;
    }

    public void backspace() {
        if (text.length() > 0) {
            text.deleteCharAt(text.length() - 1);
        }
    }

    public void setText(String value) {
        text.setLength(0);
        if (value != null) {
            text.append(value, 0, Math.min(value.length(), maxLength));
        }
    }

    public String getText() { return text.toString(); }
    public Rectangle2D.Double getBounds() { return bounds; }
    public String getPlaceholder() { return placeholder; }
    public boolean isFocused() { return focused; }
    public void setFocused(boolean focused) { this.focused = focused; }
    public boolean contains(double px, double py) { return bounds.contains(px, py); }
}
