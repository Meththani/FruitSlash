import java.awt.geom.Rectangle2D;

/** A clickable button in logical world coordinates. Drawing is done by {@link GameRenderer}. */
public class UiButton {

    /** Colour theme of the button. */
    public enum Style { PRIMARY, SECONDARY, DANGER }

    private final Rectangle2D.Double bounds;
    private String label;
    private final Style style;
    private final Runnable action;
    private boolean hovered;
    private boolean pressed;
    private boolean enabled = true;
    private boolean dimmed; // purely visual: drawn faded but still clickable

    public UiButton(String label, double x, double y, double width, double height,
                    Style style, Runnable action) {
        this.label = label;
        this.bounds = new Rectangle2D.Double(x, y, width, height);
        this.style = style;
        this.action = action;
    }

    public boolean contains(double px, double py) {
        return bounds.contains(px, py);
    }

    public void click() {
        if (enabled && action != null) {
            action.run();
        }
    }

    public Rectangle2D.Double getBounds() { return bounds; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Style getStyle() { return style; }
    public boolean isHovered() { return hovered; }
    public void setHovered(boolean hovered) { this.hovered = hovered; }
    public boolean isPressed() { return pressed; }
    public void setPressed(boolean pressed) { this.pressed = pressed; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isDimmed() { return dimmed; }
    public void setDimmed(boolean dimmed) { this.dimmed = dimmed; }
}
