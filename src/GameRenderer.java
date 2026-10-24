import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Draws every screen. All drawing happens in the logical 1280 x 720 world; the panel has already
 * scaled and centred the Graphics2D. This class never changes game state (rendering is separate
 * from updating).
 */
final class GameRenderer {

    static final int LEFT = 0;
    static final int CENTER = 1;
    static final int RIGHT = 2;

    static final int LEADERBOARD_ROWS = 8;

    private static final int W = GameConfig.WORLD_WIDTH;
    private static final int H = GameConfig.WORLD_HEIGHT;

    private static final Color CYAN = new Color(90, 220, 255);
    private static final Color GOLD = new Color(255, 214, 64);
    private static final Color WHITE = new Color(245, 245, 255);
    private static final Color MUTED = new Color(170, 175, 215);
    private static final Color RED = new Color(255, 90, 90);
    private static final Color GREEN = new Color(120, 255, 150);
    private static final Color PANEL_FILL = new Color(18, 14, 44, 225);
    private static final Color PANEL_BORDER = new Color(130, 110, 255, 200);

    private static final String[] TECH_TITLES = {
            "1. Array",
            "2. Linked list",
            "3. Stack",
            "4. Queue",
            "5. Bubble Sort",
            "6. Linear Search",
            "7. Recursion"};
    private static final String[] TECH_TEXTS = {
            "ObjectArray (capacity 32) holds all active fruits and bombs. Each frame they are moved and hit-tested, "
                    + "then finished ones are removed by one stable compaction pass.",
            "SinglyLinkedList stores the blade trail's recent mouse points: addLast on every mouse move, "
                    + "removeFirst when a point is too old or the trail passes 20 points.",
            "ArrayStack<SliceEvent> records every slice. UNDO peeks, validates, then pops the newest event and "
                    + "reverses its score, life and combo changes (LIFO).",
            "CircularQueue<SpawnRequest> holds upcoming launches: new waves are enqueued at the rear and the engine "
                    + "dequeues from the front when a launch is due.",
            "LeaderboardManager.bubbleSortScoresDescending ranks the scores (stable, early exit). "
                    + "Worst case O(n^2).",
            "LeaderboardManager.linearSearchPlayer checks entries one by one, ignoring case. Worst case O(n).",
            "LevelManager.advanceLevelRecursively builds each level from the previous one. Base case: the target "
                    + "level is reached (clamped to level 8), so it always ends."};

    private final GameEngine engine;
    private final LeaderboardManager board;
    private final UiState ui;

    private final Map<Integer, Font> fontCache = new HashMap<>();
    private BufferedImage backgroundImage;

    GameRenderer(GameEngine engine, LeaderboardManager board, UiState ui) {
        this.engine = engine;
        this.board = board;
        this.ui = ui;
    }

    // ===================================================================================== entry point

    void render(Graphics2D g) {
        double t = ui.animTime;
        drawBackground(g, t);
        GameState state = engine.getState();
        switch (state) {
            case MENU:
                drawMenu(g, t);
                break;
            case HOW_TO_PLAY:
                drawHowToPlay(g);
                break;
            case LEADERBOARD:
                drawLeaderboard(g);
                break;
            case PLAYING:
            case PAUSED:
            case LEVEL_TRANSITION:
                drawGameScene(g, t);
                drawHud(g);
                if (state == GameState.PAUSED) {
                    drawPauseOverlay(g);
                } else if (state == GameState.LEVEL_TRANSITION) {
                    drawTransitionOverlay(g);
                }
                break;
            case GAME_OVER:
            case VICTORY:
                drawGameScene(g, t);
                drawResultScreen(g, state == GameState.VICTORY);
                break;
            default:
                break;
        }
        for (UiButton button : ui.buttons) {
            drawButton(g, button);
        }
        if (engine.getFlashAlpha() > 0.01 && state == GameState.PLAYING) {
            Color f = engine.getFlashColor();
            g.setColor(new Color(f.getRed(), f.getGreen(), f.getBlue(),
                    (int) (255 * Math.min(1.0, engine.getFlashAlpha()) * 0.55)));
            g.fillRect(0, 0, W, H);
        }
    }

    // ===================================================================================== background

    private void drawBackground(Graphics2D g, double t) {
        if (backgroundImage == null) {
            backgroundImage = buildBackground();
        }
        g.drawImage(backgroundImage, 0, 0, null);
        // slowly drifting soft glows give the arcade backdrop some life
        for (int i = 0; i < 4; i++) {
            double x = W * (0.5 + 0.42 * Math.sin(t * 0.13 + i * 1.7));
            double y = H * (0.5 + 0.38 * Math.cos(t * 0.11 + i * 2.3));
            Color c = i % 2 == 0 ? new Color(120, 70, 255, 34) : new Color(40, 200, 255, 30);
            g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), 280f,
                    new float[] {0f, 1f}, new Color[] {c, new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)}));
            g.fill(new Rectangle2D.Double(x - 280, y - 280, 560, 560));
        }
    }

    private BufferedImage buildBackground() {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(26, 10, 52), 0, H, new Color(8, 30, 68)));
        g.fillRect(0, 0, W, H);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(W * 0.2, H * 0.15), 620f,
                new float[] {0f, 1f}, new Color[] {new Color(150, 60, 220, 90), new Color(150, 60, 220, 0)}));
        g.fillRect(0, 0, W, H);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(W * 0.85, H * 0.9), 650f,
                new float[] {0f, 1f}, new Color[] {new Color(30, 190, 255, 80), new Color(30, 190, 255, 0)}));
        g.fillRect(0, 0, W, H);
        // faint arcade grid on the lower part
        g.setStroke(new BasicStroke(1.2f));
        for (int i = 0; i < 12; i++) {
            double y = H * 0.62 + Math.pow(i / 11.0, 1.8) * H * 0.38;
            g.setColor(new Color(120, 160, 255, 22 + i * 2));
            g.draw(new Line2D.Double(0, y, W, y));
        }
        for (int i = -14; i <= 14; i++) {
            g.setColor(new Color(120, 160, 255, 20));
            g.draw(new Line2D.Double(W / 2.0 + i * 38, H * 0.62, W / 2.0 + i * 130, H));
        }
        // fixed star field
        Random rnd = new Random(42);
        for (int i = 0; i < 90; i++) {
            double x = rnd.nextDouble() * W;
            double y = rnd.nextDouble() * H * 0.7;
            double s = 1 + rnd.nextDouble() * 2.2;
            g.setColor(new Color(255, 255, 255, 40 + rnd.nextInt(110)));
            g.fill(new Ellipse2D.Double(x, y, s, s));
        }
        g.dispose();
        return img;
    }

    // ===================================================================================== menu

    private void drawMenu(Graphics2D g, double t) {
        // decorative floating fruit
        FruitType[] types = FruitType.values();
        double[][] spots = {{150, 190}, {1130, 200}, {240, 440}, {1050, 450}, {110, 620}, {1170, 630}, {345, 330}, {930, 585}};
        for (int i = 0; i < spots.length; i++) {
            double bob = Math.sin(t * 1.3 + i * 1.1) * 12;
            AffineTransform saved = g.getTransform();
            g.translate(spots[i][0], spots[i][1] + bob);
            g.rotate(t * 0.45 * (i % 2 == 0 ? 1 : -1) + i);
            g.scale(1.55, 1.55);
            FruitPainter.drawFruit(g, types[i % types.length], types[i % types.length].getRadius());
            g.setTransform(saved);
        }
        AffineTransform saved = g.getTransform();
        g.translate(1020, 300 + Math.sin(t * 1.6) * 10);
        g.scale(1.25, 1.25);
        FruitPainter.drawBomb(g, Bomb.RADIUS, t);
        g.setTransform(saved);

        // title
        double pulse = 1.0 + 0.015 * Math.sin(t * 2.4);
        AffineTransform title = g.getTransform();
        g.translate(W / 2.0, 140);
        g.scale(pulse, pulse);
        Font titleFont = font(100);
        g.setFont(titleFont);
        FontMetrics fm = g.getFontMetrics();
        String text = "FRUIT SLASH";
        double tx = -fm.stringWidth(text) / 2.0;
        g.setColor(new Color(0, 0, 0, 140));
        g.drawString(text, (float) tx + 5, 6);
        g.setPaint(new GradientPaint(0, -70, new Color(255, 230, 90), 0, 10, new Color(255, 70, 130)));
        g.drawString(text, (float) tx, 0);
        g.setTransform(title);
        drawText(g, "ARCADE CHALLENGE", W / 2.0, 196, 34, CYAN, CENTER, true);
        drawText(g, "HIGH SCORE  " + board.getBestScore(), W / 2.0, 676, 28, GOLD, CENTER, true);
        drawText(g, "General Sir John Kotelawala Defence University  |  IT22013 Data Structures and Algorithms",
                W / 2.0, 706, 15, new Color(150, 155, 200), CENTER, false);
    }

    // ===================================================================================== how to play

    private void drawHowToPlay(Graphics2D g) {
        drawPanel(g, 110, 36, 1060, 590);
        if (ui.showTechInfo) {
            drawText(g, "TECHNICAL INFO", W / 2.0, 100, 50, GOLD, CENTER, true);
            drawText(g, "For the lecturer / evaluator - how each required concept is used in this game",
                    W / 2.0, 130, 19, MUTED, CENTER, false);
            double y = 176;
            for (int i = 0; i < TECH_TITLES.length; i++) {
                drawText(g, TECH_TITLES[i], 150, y, 20, CYAN, LEFT, true);
                y = drawWrapped(g, TECH_TEXTS[i], 380, y, 760, 18, WHITE, 23);
                y += 14;
            }
            return;
        }
        drawText(g, "HOW TO PLAY", W / 2.0, 100, 54, GOLD, CENTER, true);
        double y = 150;
        y = section(g, "GOAL", "Slice the flying fruit to score points. Slice the target number of fruits to clear a "
                + "level, and clear all " + GameConfig.MAX_LEVEL + " levels to win!", y);
        y = section(g, "CONTROLS", "Click or drag the mouse across fruit to slice it. P or Esc pauses. U or the UNDO "
                + "button reverses your last slice. R restarts (pause menu or game-over screen). M mutes the sound.", y);
        y = section(g, "RULES", "Missing a fruit costs a life. Slicing a bomb costs a life and "
                + GameConfig.BOMB_SCORE_PENALTY + " points - never slice the bomb! Slice fruit quickly, one after "
                + "another, to build a combo for bonus points.", y);
        y = section(g, "UNDO SLICE", "You start with " + GameConfig.INITIAL_UNDO_CHARGES + " undo charges (maximum "
                + GameConfig.MAX_UNDO_CHARGES + ", and +1 for every level you clear). Undo takes back the points and "
                + "returns the fruit - or gives back the life if you sliced a bomb. It only works for "
                + GameConfig.UNDO_WINDOW + " seconds after the slice.", y);
        // fruit legend
        FruitType[] types = FruitType.values();
        for (int i = 0; i <= types.length; i++) {
            double cx = 190 + i * 128;
            double cy = 554;
            AffineTransform saved = g.getTransform();
            g.translate(cx, cy);
            g.rotate(-0.25 + 0.1 * i);
            if (i < types.length) {
                g.scale(0.85, 0.85);
                FruitPainter.drawFruit(g, types[i], types[i].getRadius());
            } else {
                g.scale(0.8, 0.8);
                FruitPainter.drawBomb(g, Bomb.RADIUS, ui.animTime);
            }
            g.setTransform(saved);
            String label = i < types.length ? types[i].getDisplayName() + " +" + types[i].getPoints() : "Bomb!";
            drawText(g, label, cx, 608, 15, i < types.length ? WHITE : RED, CENTER, true);
        }
    }

    private double section(Graphics2D g, String title, String body, double y) {
        drawText(g, title, 150, y, 21, CYAN, LEFT, true);
        double next = drawWrapped(g, body, 150, y + 26, 980, 19, WHITE, 24);
        return next + 12;
    }

    // ===================================================================================== leaderboard

    private void drawLeaderboard(Graphics2D g) {
        drawText(g, "LEADERBOARD", W / 2.0, 62, 52, GOLD, CENTER, true);
        drawTextInput(g, ui.searchInput);
        if (!ui.searchMessage.isEmpty()) {
            Color c = ui.highlightRank > 0 ? GREEN : RED;
            drawText(g, ui.searchMessage, W / 2.0, 170, 22, c, CENTER, true);
        }

        drawPanel(g, 230, 182, 820, 396);
        drawText(g, "RANK", 268, 214, 19, CYAN, LEFT, true);
        drawText(g, "PLAYER", 390, 214, 19, CYAN, LEFT, true);
        drawText(g, "SCORE", 1012, 214, 19, CYAN, RIGHT, true);

        ScoreEntry[] ranked = board.getRankedEntries();
        int pages = Math.max(1, (ranked.length + LEADERBOARD_ROWS - 1) / LEADERBOARD_ROWS);
        int page = Math.max(0, Math.min(ui.leaderboardPage, pages - 1));
        if (ranked.length == 0) {
            drawText(g, "No scores yet - go and play a game!", W / 2.0, 380, 28, MUTED, CENTER, true);
        }
        for (int row = 0; row < LEADERBOARD_ROWS; row++) {
            int index = page * LEADERBOARD_ROWS + row;
            if (index >= ranked.length) {
                break;
            }
            int rank = index + 1;
            double y = 226 + row * 42;
            boolean highlighted = rank == ui.highlightRank;
            if (highlighted) {
                g.setColor(new Color(255, 214, 64, 70 + (int) (30 * Math.sin(ui.animTime * 6))));
                g.fill(new RoundRectangle2D.Double(244, y, 792, 38, 14, 14));
                g.setColor(GOLD);
                g.setStroke(new BasicStroke(2f));
                g.draw(new RoundRectangle2D.Double(244, y, 792, 38, 14, 14));
            } else if (row % 2 == 0) {
                g.setColor(new Color(255, 255, 255, 14));
                g.fill(new RoundRectangle2D.Double(244, y, 792, 38, 14, 14));
            }
            Color rankColor = rank == 1 ? GOLD : rank == 2 ? new Color(205, 210, 225)
                    : rank == 3 ? new Color(220, 150, 90) : WHITE;
            drawText(g, "#" + rank, 268, y + 28, 24, rankColor, LEFT, true);
            drawText(g, ranked[index].getName(), 390, y + 28, 24, WHITE, LEFT, true);
            drawText(g, String.valueOf(ranked[index].getScore()), 1012, y + 28, 24, GOLD, RIGHT, true);
        }
        drawText(g, "Page " + (page + 1) + " / " + pages + "   (" + ranked.length
                + (ranked.length == 1 ? " score)" : " scores)"),
                W / 2.0, 614, 18, MUTED, CENTER, false);
    }

    // ===================================================================================== game scene

    private void drawGameScene(Graphics2D g, double t) {
        // soft vignette keeps attention on the centre of the play area
        g.setPaint(new RadialGradientPaint(new Point2D.Double(W / 2.0, H / 2.0), (float) (W * 0.65),
                new float[] {0.55f, 1f}, new Color[] {new Color(0, 0, 0, 0), new Color(0, 0, 0, 110)}));
        g.fillRect(0, 0, W, H);

        AffineTransform original = g.getTransform();
        if (engine.getShakeTime() > 0) {
            double mag = 14 * Math.min(1.0, engine.getShakeTime() / 0.45);
            g.translate((Math.random() - 0.5) * mag, (Math.random() - 0.5) * mag);
        }

        for (Particle p : engine.getParticles()) {
            drawParticle(g, p);
        }
        for (FruitHalf half : engine.getHalves()) {
            AffineTransform saved = g.getTransform();
            g.translate(half.getX(), half.getY());
            g.rotate(half.getRotation());
            if (half.getAlpha() < 1.0) {
                g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,
                        (float) half.getAlpha()));
            }
            FruitPainter.drawHalf(g, half.getType(), half.getRadius(), half.getSide());
            g.setComposite(java.awt.AlphaComposite.SrcOver);
            g.setTransform(saved);
        }
        ObjectArray objects = engine.getObjects();
        for (int i = 0; i < objects.size(); i++) {
            GameObject object = objects.get(i);
            if (!object.isActive()) {
                continue;
            }
            AffineTransform saved = g.getTransform();
            g.translate(object.getX(), object.getY());
            if (object.isImmune(engine.getGameTime())) {
                g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,
                        (float) (0.45 + 0.25 * Math.sin(t * 25))));
            }
            if (object.isBomb()) {
                g.rotate(Math.sin(object.getRotation()) * 0.25);
                FruitPainter.drawBomb(g, object.getRadius(), t);
            } else {
                g.rotate(object.getRotation());
                FruitPainter.drawFruit(g, ((Fruit) object).getType(), object.getRadius());
            }
            g.setComposite(java.awt.AlphaComposite.SrcOver);
            g.setTransform(saved);
        }
        drawTrail(g);
        for (FloatingText text : engine.getFloatingTexts()) {
            drawFloatingText(g, text);
        }
        g.setTransform(original);
    }

    private void drawParticle(Graphics2D g, Particle p) {
        double life = p.getLifeFraction();
        double size = Math.max(0.5, p.getSize());
        Color c = p.getColor();
        int baseAlpha = c.getAlpha();
        if (p.isGlow()) {
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (baseAlpha * life * 0.28)));
            g.fill(new Ellipse2D.Double(p.getX() - size * 1.9, p.getY() - size * 1.9, size * 3.8, size * 3.8));
        }
        int alpha = (int) (baseAlpha * Math.min(1.0, life * 1.6));
        if (size > 25) { // juice splat: soft blob that fades towards its edge
            g.setPaint(new RadialGradientPaint(new Point2D.Double(p.getX(), p.getY()), (float) size,
                    new float[] {0f, 0.55f, 1f},
                    new Color[] {new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha * 3 / 4),
                            new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha / 3),
                            new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)}));
            g.fill(new Ellipse2D.Double(p.getX() - size, p.getY() - size, size * 2, size * 2));
            return;
        }
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
        g.fill(new Ellipse2D.Double(p.getX() - size, p.getY() - size, size * 2, size * 2));
    }

    private void drawTrail(Graphics2D g) {
        SliceTrail trail = engine.getTrail();
        double now = engine.getGameTime();
        SliceTrail.TrailPoint previous = null;
        for (SliceTrail.TrailPoint point : trail.getPoints()) {
            if (previous != null && previous.getStrokeId() == point.getStrokeId()) {
                double life = Math.max(0.0, Math.min(1.0, 1.0 - (now - point.getTime()) / trail.getMaxAge()));
                Line2D.Double line = new Line2D.Double(previous.getX(), previous.getY(), point.getX(), point.getY());
                g.setStroke(new BasicStroke((float) (6 + 16 * life), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(110, 215, 255, (int) (80 * life)));
                g.draw(line);
                g.setStroke(new BasicStroke((float) (2 + 5 * life), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(255, 255, 255, (int) (255 * life)));
                g.draw(line);
            }
            previous = point;
        }
    }

    private void drawFloatingText(Graphics2D g, FloatingText text) {
        int size = (int) Math.round(text.getSize() * text.getScale());
        int alpha = (int) (255 * text.getAlpha());
        Color c = text.getColor();
        drawText(g, text.getText(), text.getX() + 2, text.getY() + 3, size, new Color(0, 0, 0, alpha * 7 / 10),
                CENTER, true);
        drawText(g, text.getText(), text.getX(), text.getY(), size,
                new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha), CENTER, true);
    }

    // ===================================================================================== HUD

    private void drawHud(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(5, 3, 20, 190), 0, 70, new Color(5, 3, 20, 0)));
        g.fillRect(0, 0, W, 78);

        drawText(g, "SCORE", 26, 24, 15, CYAN, LEFT, true);
        drawText(g, String.valueOf(engine.getScore()), 26, 58, 36, WHITE, LEFT, true);

        if (engine.getCombo() >= 2) {
            double pop = 1.0 + 0.3 * engine.getComboPulse();
            drawText(g, "COMBO x" + engine.getCombo(), 232, 56, (int) (26 * pop), GOLD, LEFT, true);
        }

        drawText(g, "LEVEL " + engine.getLevel() + " / " + GameConfig.MAX_LEVEL, W / 2.0, 28, 24, WHITE, CENTER, true);
        double barW = 300;
        double barX = W / 2.0 - barW / 2;
        double fraction = Math.min(1.0, engine.getLevelFruitsSliced() / (double) Math.max(1, engine.getLevelTarget()));
        g.setColor(new Color(255, 255, 255, 45));
        g.fill(new RoundRectangle2D.Double(barX, 38, barW, 12, 12, 12));
        g.setPaint(new GradientPaint((float) barX, 0, new Color(90, 240, 150), (float) (barX + barW), 0,
                new Color(90, 200, 255)));
        g.fill(new RoundRectangle2D.Double(barX, 38, Math.max(12, barW * fraction), 12, 12, 12));
        drawText(g, engine.getLevelFruitsSliced() + " / " + engine.getLevelTarget() + " fruits", W / 2.0, 68, 14,
                MUTED, CENTER, true);

        int lives = engine.getStartingLives();
        for (int i = 0; i < lives; i++) {
            double cx = 1018 - (lives - 1 - i) * 38;
            boolean full = i < engine.getLives();
            drawHeart(g, cx, 38, 13, full ? new Color(255, 70, 100) : new Color(80, 80, 110, 160),
                    full ? new Color(255, 200, 210) : new Color(150, 150, 180, 160));
        }
    }

    private void drawHeart(Graphics2D g, double cx, double cy, double s, Color fill, Color stroke) {
        Path2D.Double heart = new Path2D.Double();
        heart.moveTo(cx, cy + s * 0.95);
        heart.curveTo(cx - s * 1.5, cy - s * 0.05, cx - s * 0.95, cy - s * 1.1, cx, cy - s * 0.4);
        heart.curveTo(cx + s * 0.95, cy - s * 1.1, cx + s * 1.5, cy - s * 0.05, cx, cy + s * 0.95);
        heart.closePath();
        g.setColor(fill);
        g.fill(heart);
        g.setColor(stroke);
        g.setStroke(new BasicStroke(2f));
        g.draw(heart);
    }

    // ===================================================================================== overlays

    private void drawPauseOverlay(Graphics2D g) {
        g.setColor(new Color(4, 2, 18, 170));
        g.fillRect(0, 0, W, H);
        drawText(g, "PAUSED", W / 2.0, 210, 92, WHITE, CENTER, true);
        drawText(g, "Take a breath - the fruit will wait.", W / 2.0, 252, 24, MUTED, CENTER, false);
    }

    private void drawTransitionOverlay(Graphics2D g) {
        g.setColor(new Color(4, 2, 18, 150));
        g.fillRect(0, 0, W, H);
        int done = engine.getLevel();
        LevelConfig next = engine.getPendingConfig();
        double pulse = 1.0 + 0.03 * Math.sin(ui.animTime * 8);
        AffineTransform saved = g.getTransform();
        g.translate(W / 2.0, 250);
        g.scale(pulse, pulse);
        drawText(g, "LEVEL " + done + " COMPLETE!", 0, 0, 68, GOLD, CENTER, true);
        g.setTransform(saved);
        drawText(g, "+" + engine.getLastLevelBonus() + " bonus points", W / 2.0, 308, 30, WHITE, CENTER, true);
        drawText(g, "+1 Undo charge", W / 2.0, 346, 24, CYAN, CENTER, true);
        drawText(g, "NEXT: LEVEL " + next.getLevel(), W / 2.0, 412, 34, GREEN, CENTER, true);
        drawText(g, "Slice " + next.getFruitTarget() + " fruits  -  faster launches  -  more bombs",
                W / 2.0, 450, 22, MUTED, CENTER, true);
        double barW = 420;
        double barX = W / 2.0 - barW / 2;
        g.setColor(new Color(255, 255, 255, 45));
        g.fill(new RoundRectangle2D.Double(barX, 486, barW, 14, 14, 14));
        g.setColor(GREEN);
        g.fill(new RoundRectangle2D.Double(barX, 486, Math.max(14, barW * engine.getTransitionProgress()), 14, 14, 14));
        drawText(g, "Get ready...", W / 2.0, 530, 20, WHITE, CENTER, true);
    }

    // ===================================================================================== result screen

    private void drawResultScreen(Graphics2D g, boolean victory) {
        g.setColor(new Color(4, 2, 18, 175));
        g.fillRect(0, 0, W, H);

        double pulse = 1.0 + 0.02 * Math.sin(ui.animTime * 3);
        AffineTransform saved = g.getTransform();
        g.translate(W / 2.0, 100);
        g.scale(pulse, pulse);
        drawText(g, victory ? "VICTORY!" : "GAME OVER", 0, 0, 74, victory ? GOLD : RED, CENTER, true);
        g.setTransform(saved);
        if (victory) {
            drawText(g, "You cleared all " + GameConfig.MAX_LEVEL + " levels!", W / 2.0, 134, 22, WHITE, CENTER, true);
        }

        drawPanel(g, 200, 148, 880, 520);

        // left column: statistics
        drawText(g, "FINAL SCORE", 240, 192, 18, CYAN, LEFT, true);
        drawText(g, String.valueOf(engine.getScore()), 240, 246, 58, GOLD, LEFT, true);
        if (engine.isNewBest()) {
            double badge = 1.0 + 0.06 * Math.sin(ui.animTime * 7);
            AffineTransform s2 = g.getTransform();
            g.translate(520, 230);
            g.scale(badge, badge);
            drawText(g, "NEW BEST!", 0, 0, 26, GREEN, CENTER, true);
            g.setTransform(s2);
        }
        String[] labels = {"Level reached", "Fruits sliced", "Best combo", "Best score"};
        String[] values = {engine.getLevel() + " / " + GameConfig.MAX_LEVEL, String.valueOf(engine.getFruitsSliced()),
                "x" + engine.getBestCombo(), String.valueOf(engine.getBestScore())};
        for (int i = 0; i < labels.length; i++) {
            double y = 302 + i * 38;
            drawText(g, labels[i], 240, y, 23, MUTED, LEFT, true);
            drawText(g, values[i], 620, y, 23, WHITE, RIGHT, true);
        }

        // right column: top five
        drawText(g, "TOP 5", 690, 192, 18, CYAN, LEFT, true);
        ScoreEntry[] ranked = board.getRankedEntries();
        if (ranked.length == 0) {
            drawText(g, "No saved scores yet", 690, 236, 21, MUTED, LEFT, false);
        }
        for (int i = 0; i < Math.min(5, ranked.length); i++) {
            double y = 222 + i * 40;
            boolean mine = ui.scoreSaved && ui.savedRank == i + 1;
            if (mine) {
                g.setColor(new Color(255, 214, 64, 60));
                g.fill(new RoundRectangle2D.Double(680, y - 4, 370, 34, 12, 12));
            }
            drawText(g, "#" + (i + 1), 694, y + 21, 21, i == 0 ? GOLD : WHITE, LEFT, true);
            drawText(g, ranked[i].getName(), 748, y + 21, 21, WHITE, LEFT, true);
            drawText(g, String.valueOf(ranked[i].getScore()), 1034, y + 21, 21, GOLD, RIGHT, true);
        }

        // name entry (a score of 0 is never saved)
        if (engine.getScore() > 0) {
            drawText(g, ui.scoreSaved ? "SCORE SAVED" : "SAVE YOUR SCORE", 240, 482, 18, CYAN, LEFT, true);
            drawTextInput(g, ui.nameInput);
            if (!ui.statusMessage.isEmpty()) {
                Color c = ui.scoreSaved ? GREEN : RED;
                drawText(g, ui.statusMessage, 240, 572, 20, c, LEFT, true);
            }
        } else {
            drawText(g, "A score of 0 is not saved to the leaderboard.", 240, 520, 21, MUTED, LEFT, true);
        }
        drawText(g, "Press R to play again once your name is saved  -  Esc for the main menu",
                W / 2.0, 692, 16, MUTED, CENTER, false);
    }

    // ===================================================================================== widgets

    private void drawPanel(Graphics2D g, double x, double y, double w, double h) {
        RoundRectangle2D.Double shape = new RoundRectangle2D.Double(x, y, w, h, 34, 34);
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new RoundRectangle2D.Double(x + 4, y + 8, w, h, 34, 34));
        g.setColor(PANEL_FILL);
        g.fill(shape);
        g.setStroke(new BasicStroke(2.5f));
        g.setColor(PANEL_BORDER);
        g.draw(shape);
    }

    private void drawButton(Graphics2D g, UiButton b) {
        Rectangle2D.Double r = b.getBounds();
        Color top;
        Color bottom;
        switch (b.getStyle()) {
            case PRIMARY:
                top = new Color(90, 235, 140);
                bottom = new Color(25, 150, 100);
                break;
            case DANGER:
                top = new Color(255, 120, 120);
                bottom = new Color(185, 40, 70);
                break;
            default:
                top = new Color(135, 120, 255);
                bottom = new Color(70, 55, 190);
                break;
        }
        boolean inactive = !b.isEnabled() || b.isDimmed();
        if (!b.isEnabled()) {
            top = new Color(110, 110, 135);
            bottom = new Color(70, 70, 95);
        }
        double offset = 0;
        if (b.isPressed() && b.isEnabled()) {
            top = top.darker();
            bottom = bottom.darker();
            offset = 2;
        } else if (b.isHovered() && b.isEnabled()) {
            top = brighten(top);
            bottom = brighten(bottom);
        }
        float alpha = inactive ? 0.55f : 1.0f;
        g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));

        RoundRectangle2D.Double shape = new RoundRectangle2D.Double(r.x, r.y + offset, r.width, r.height,
                r.height * 0.45, r.height * 0.45);
        if (b.isHovered() && b.isEnabled() && !b.isPressed()) {
            g.setColor(new Color(top.getRed(), top.getGreen(), top.getBlue(), 70));
            g.fill(new RoundRectangle2D.Double(r.x - 5, r.y - 5, r.width + 10, r.height + 10,
                    r.height * 0.6, r.height * 0.6));
        } else if (!b.isPressed()) {
            g.setColor(new Color(0, 0, 0, 90));
            g.fill(new RoundRectangle2D.Double(r.x, r.y + 5, r.width, r.height, r.height * 0.45, r.height * 0.45));
        }
        g.setPaint(new GradientPaint(0, (float) r.y, top, 0, (float) (r.y + r.height), bottom));
        g.fill(shape);
        g.setColor(new Color(255, 255, 255, 70));
        g.fill(new RoundRectangle2D.Double(r.x + 6, r.y + offset + 3, r.width - 12, r.height * 0.38,
                r.height * 0.3, r.height * 0.3));
        g.setStroke(new BasicStroke(b.isHovered() ? 3f : 2f));
        g.setColor(new Color(255, 255, 255, b.isHovered() ? 235 : 130));
        g.draw(shape);

        int size = (int) Math.min(28, Math.round(r.height * 0.46));
        drawText(g, b.getLabel(), r.x + r.width / 2, r.y + offset + r.height / 2 + size * 0.36, size,
                new Color(255, 255, 255), CENTER, true);
        g.setComposite(java.awt.AlphaComposite.SrcOver);
    }

    private static Color brighten(Color c) {
        return new Color(Math.min(255, c.getRed() + 40), Math.min(255, c.getGreen() + 40), Math.min(255, c.getBlue() + 40));
    }

    private void drawTextInput(Graphics2D g, TextInput input) {
        Rectangle2D.Double b = input.getBounds();
        RoundRectangle2D.Double shape = new RoundRectangle2D.Double(b.x, b.y, b.width, b.height, 16, 16);
        g.setColor(new Color(6, 4, 24, 235));
        g.fill(shape);
        g.setStroke(new BasicStroke(input.isFocused() ? 3f : 2f));
        g.setColor(input.isFocused() ? CYAN : new Color(130, 120, 220, 190));
        g.draw(shape);
        int size = 24;
        String text = input.getText();
        if (text.isEmpty() && !input.isFocused()) {
            drawText(g, input.getPlaceholder(), b.x + 16, b.y + b.height / 2 + size * 0.36, size, new Color(120, 125, 165),
                    LEFT, false);
        } else {
            drawText(g, text, b.x + 16, b.y + b.height / 2 + size * 0.36, size, WHITE, LEFT, true);
            if (input.isFocused() && ((int) (ui.animTime * 2)) % 2 == 0) {
                g.setFont(font(size));
                double caretX = b.x + 16 + g.getFontMetrics().stringWidth(text) + 3;
                g.setColor(CYAN);
                g.fill(new Rectangle2D.Double(caretX, b.y + 9, 3, b.height - 18));
            }
        }
    }

    // ===================================================================================== text helpers

    private Font font(int size) {
        return fontCache.computeIfAbsent(size, s -> new Font(Font.SANS_SERIF, Font.BOLD, s));
    }

    private Font plainFont(int size) {
        return fontCache.computeIfAbsent(-size, s -> new Font(Font.SANS_SERIF, Font.PLAIN, size));
    }

    /** Draws a single line of text; {@code y} is the baseline. */
    void drawText(Graphics2D g, String text, double x, double y, int size, Color color, int align, boolean bold) {
        g.setFont(bold ? font(size) : plainFont(size));
        FontMetrics fm = g.getFontMetrics();
        double width = fm.stringWidth(text);
        double drawX = align == CENTER ? x - width / 2 : align == RIGHT ? x - width : x;
        g.setColor(color);
        g.drawString(text, (float) drawX, (float) y);
    }

    /** Draws word-wrapped text and returns the baseline y of the NEXT line. */
    private double drawWrapped(Graphics2D g, String text, double x, double y, double maxWidth,
                               int size, Color color, int lineHeight) {
        g.setFont(plainFont(size));
        FontMetrics fm = g.getFontMetrics();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        double cursor = y;
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(candidate) > maxWidth && line.length() > 0) {
                drawText(g, line.toString(), x, cursor, size, color, LEFT, false);
                cursor += lineHeight;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) {
            drawText(g, line.toString(), x, cursor, size, color, LEFT, false);
            cursor += lineHeight;
        }
        return cursor;
    }
}
