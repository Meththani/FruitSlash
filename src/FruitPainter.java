import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;

/**
 * Draws every fruit, fruit half and bomb with Java2D shapes (no image files are used).
 * All methods draw centred on the origin of the current Graphics2D transform, so the caller just
 * translates and rotates to the object's position first.
 */
public final class FruitPainter {

    private static final Color LEAF = new Color(0x43A047);
    private static final Color LEAF_DARK = new Color(0x2E7D32);
    private static final Color BROWN = new Color(0x6D4C41);

    private FruitPainter() {
        // static helpers only
    }

    // ------------------------------------------------------------------ whole fruit

    /** Draws a whole (un-sliced) fruit of the given type with the given hitbox radius. */
    public static void drawFruit(Graphics2D g, FruitType type, double r) {
        if (type == FruitType.BANANA) {
            AffineTransform saved = g.getTransform();
            g.rotate(-0.35);
            paintBody(g, type, r);
            drawBananaTips(g, r);
            g.setTransform(saved);
            return;
        }
        paintBody(g, type, r);
        switch (type) {
            case APPLE:
                drawAppleDetails(g, r);
                break;
            case ORANGE:
                drawOrangeDetails(g, r);
                break;
            case WATERMELON:
                drawWatermelonStripes(g, type, r);
                break;
            case STRAWBERRY:
                drawStrawberryDetails(g, r);
                break;
            case LEMON:
                drawLemonDetails(g, r);
                break;
            case GRAPES:
                drawGrapeDetails(g, r);
                break;
            default:
                break;
        }
        drawHighlight(g, r);
    }

    private static void paintBody(Graphics2D g, FruitType type, double r) {
        Shape body = bodyShape(type, r);
        Color rind = type.getRind();
        g.setPaint(new RadialGradientPaint(new Point2D.Double(-r * 0.35, -r * 0.4), (float) (r * 1.7),
                new float[] {0f, 1f}, new Color[] {lighten(rind, 0.35f), darken(rind, 0.35f)}));
        g.fill(body);
        g.setStroke(new BasicStroke(2.2f));
        g.setColor(new Color(0, 0, 0, 70));
        g.draw(body);
    }

    private static void drawHighlight(Graphics2D g, double r) {
        g.setColor(new Color(255, 255, 255, 85));
        g.fill(new Ellipse2D.Double(-r * 0.62, -r * 0.7, r * 0.5, r * 0.32));
    }

    private static void drawAppleDetails(Graphics2D g, double r) {
        g.setStroke(new BasicStroke(3.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(BROWN);
        g.draw(new QuadCurve2D.Double(0, -r * 0.82, 2, -r * 1.05, 7, -r * 1.25));
        AffineTransform saved = g.getTransform();
        g.translate(r * 0.3, -r * 0.98);
        g.rotate(-0.5);
        g.setColor(LEAF);
        g.fill(new Ellipse2D.Double(0, -r * 0.13, r * 0.55, r * 0.26));
        g.setColor(LEAF_DARK);
        g.setStroke(new BasicStroke(1.4f));
        g.draw(new Line2D.Double(0, 0, r * 0.5, 0));
        g.setTransform(saved);
    }

    private static void drawOrangeDetails(Graphics2D g, double r) {
        g.setColor(new Color(190, 90, 0, 90));
        double[][] dots = {{-0.5, -0.1}, {-0.1, 0.45}, {0.35, 0.2}, {0.15, -0.45}, {0.55, -0.3},
                {-0.35, 0.5}, {0.0, 0.05}, {-0.6, 0.25}, {0.5, 0.55}};
        for (double[] d : dots) {
            g.fill(new Ellipse2D.Double(d[0] * r - 1.6, d[1] * r - 1.6, 3.2, 3.2));
        }
        g.setColor(LEAF);
        g.fill(new Ellipse2D.Double(-r * 0.16, -r * 1.02, r * 0.32, r * 0.16));
        g.setColor(LEAF_DARK);
        g.fill(new Ellipse2D.Double(-r * 0.05, -r * 1.12, r * 0.55, r * 0.22));
    }

    private static void drawWatermelonStripes(Graphics2D g, FruitType type, double r) {
        Shape oldClip = g.getClip();
        g.clip(bodyShape(type, r));
        g.setColor(new Color(0x145A1E));
        g.setStroke(new BasicStroke((float) (r * 0.14), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        for (int i = -3; i <= 3; i++) {
            g.draw(new QuadCurve2D.Double(i * r * 0.3, -r * 0.85, i * r * 0.62, 0, i * r * 0.3, r * 0.85));
        }
        g.setClip(oldClip);
    }

    private static void drawStrawberryDetails(Graphics2D g, double r) {
        g.setColor(new Color(255, 235, 140));
        double[][] seeds = {{-0.35, -0.3}, {0.0, -0.4}, {0.35, -0.3}, {-0.5, 0.0}, {-0.15, -0.05},
                {0.2, -0.05}, {0.52, 0.0}, {-0.3, 0.3}, {0.05, 0.28}, {0.35, 0.3}, {-0.08, 0.6}};
        for (double[] s : seeds) {
            g.fill(new Ellipse2D.Double(s[0] * r - 1.7, s[1] * r - 2.4, 3.4, 4.8));
        }
        g.setColor(LEAF);
        for (int i = 0; i < 5; i++) {
            AffineTransform saved = g.getTransform();
            g.translate(0, -r * 0.62);
            g.rotate(-1.25 + i * 0.62);
            g.fill(new Ellipse2D.Double(-r * 0.09, -r * 0.5, r * 0.18, r * 0.5));
            g.setTransform(saved);
        }
        g.setColor(LEAF_DARK);
        g.fill(new Ellipse2D.Double(-r * 0.12, -r * 0.7, r * 0.24, r * 0.16));
    }

    private static void drawLemonDetails(Graphics2D g, double r) {
        g.setColor(new Color(200, 170, 0, 80));
        double[][] dots = {{-0.5, -0.1}, {-0.1, 0.3}, {0.35, 0.1}, {0.1, -0.35}, {0.55, -0.2}, {-0.35, 0.3}};
        for (double[] d : dots) {
            g.fill(new Ellipse2D.Double(d[0] * r - 1.4, d[1] * r - 1.4, 2.8, 2.8));
        }
        g.setColor(LEAF);
        g.fill(new Ellipse2D.Double(r * 0.55, -r * 0.8, r * 0.5, r * 0.22));
    }

    private static void drawGrapeDetails(Graphics2D g, double r) {
        for (double[] c : grapeCentres()) {
            double gr = r * 0.4;
            g.setColor(new Color(255, 255, 255, 70));
            g.fill(new Ellipse2D.Double(c[0] * r - gr * 0.55, c[1] * r - gr * 0.65, gr * 0.5, gr * 0.35));
        }
        g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(BROWN);
        g.draw(new QuadCurve2D.Double(0, -r * 0.8, 1, -r * 1.0, 6, -r * 1.15));
        g.setColor(LEAF);
        g.fill(new Ellipse2D.Double(r * 0.1, -r * 1.12, r * 0.5, r * 0.26));
    }

    private static void drawBananaTips(Graphics2D g, double r) {
        g.setColor(BROWN);
        g.fill(new Ellipse2D.Double(-r * 0.93, -r * 0.62, r * 0.17, r * 0.17));
        g.fill(new Ellipse2D.Double(r * 0.76, -r * 0.62, r * 0.17, r * 0.17));
        g.setColor(new Color(255, 255, 255, 80));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new QuadCurve2D.Double(-r * 0.7, -r * 0.1, 0, r * 0.45, r * 0.7, -r * 0.1));
    }

    // ------------------------------------------------------------------ halves

    /**
     * Draws one half of a sliced fruit showing its inside. The cut runs along the vertical axis, so the
     * caller rotates the Graphics2D to the slice angle. {@code side} -1 draws the left half, +1 the right.
     */
    public static void drawHalf(Graphics2D g, FruitType type, double r, int side) {
        Shape oldClip = g.getClip();
        Rectangle2D.Double halfPlane = side < 0
                ? new Rectangle2D.Double(-r * 2.5, -r * 2.5, r * 2.5, r * 5)
                : new Rectangle2D.Double(0, -r * 2.5, r * 2.5, r * 5);
        g.clip(halfPlane);

        Shape body = bodyShape(type, r);
        Color rind = type.getRind();
        g.setPaint(new RadialGradientPaint(new Point2D.Double(-r * 0.3, -r * 0.35), (float) (r * 1.7),
                new float[] {0f, 1f}, new Color[] {lighten(rind, 0.25f), darken(rind, 0.3f)}));
        g.fill(body);

        Shape inner = AffineTransform.getScaleInstance(0.84, 0.84).createTransformedShape(body);
        Color flesh = type.getFlesh();
        g.setPaint(new RadialGradientPaint(new Point2D.Double(0, 0), (float) (r * 1.05),
                new float[] {0f, 1f}, new Color[] {lighten(flesh, 0.25f), flesh}));
        g.fill(inner);
        drawHalfDetails(g, type, r);
        g.setClip(oldClip);
    }

    private static void drawHalfDetails(Graphics2D g, FruitType type, double r) {
        switch (type) {
            case WATERMELON:
                g.setColor(new Color(20, 20, 20));
                double[][] seeds = {{-0.45, -0.25}, {-0.2, 0.15}, {0.25, -0.2}, {0.45, 0.2}, {0.0, -0.45}, {-0.6, 0.2}};
                for (double[] s : seeds) {
                    g.fill(new Ellipse2D.Double(s[0] * r - 2.2, s[1] * r - 3.4, 4.4, 6.8));
                }
                g.setColor(new Color(255, 190, 200, 160));
                g.setStroke(new BasicStroke(2f));
                g.draw(new Ellipse2D.Double(-r * 0.68, -r * 0.55, r * 1.36, r * 1.1));
                break;
            case APPLE:
                g.setColor(new Color(110, 70, 40));
                g.fill(new Ellipse2D.Double(-r * 0.22, -r * 0.1, r * 0.14, r * 0.3));
                g.fill(new Ellipse2D.Double(r * 0.08, -r * 0.1, r * 0.14, r * 0.3));
                break;
            case ORANGE:
            case LEMON:
                g.setColor(new Color(255, 255, 255, 140));
                g.setStroke(new BasicStroke(2.2f));
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4.0;
                    g.draw(new Line2D.Double(0, 0, Math.cos(a) * r * 0.78, Math.sin(a) * r * 0.78));
                }
                break;
            case STRAWBERRY:
                g.setColor(new Color(255, 255, 255, 120));
                g.fill(new Ellipse2D.Double(-r * 0.2, -r * 0.35, r * 0.4, r * 0.9));
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------------ shapes

    /** The silhouette of a fruit (also used for the cut halves). */
    public static Shape bodyShape(FruitType type, double r) {
        switch (type) {
            case APPLE:
                return new Ellipse2D.Double(-r, -r * 0.9, r * 2, r * 1.9);
            case ORANGE:
                return new Ellipse2D.Double(-r, -r, r * 2, r * 2);
            case WATERMELON:
                return new Ellipse2D.Double(-r, -r * 0.82, r * 2, r * 1.64);
            case BANANA: {
                Area outer = new Area(new Ellipse2D.Double(-r * 0.95, -r * 1.2, r * 1.9, r * 1.9));
                Area cut = new Area(new Ellipse2D.Double(-r * 0.95, -r * 1.75, r * 1.9, r * 1.9));
                outer.subtract(cut);
                return outer;
            }
            case STRAWBERRY: {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(0, r * 1.0);
                p.curveTo(-r * 0.35, r * 0.85, -r * 1.05, r * 0.2, -r * 0.95, -r * 0.35);
                p.curveTo(-r * 0.85, -r * 0.85, -r * 0.2, -r * 0.8, 0, -r * 0.6);
                p.curveTo(r * 0.2, -r * 0.8, r * 0.85, -r * 0.85, r * 0.95, -r * 0.35);
                p.curveTo(r * 1.05, r * 0.2, r * 0.35, r * 0.85, 0, r * 1.0);
                p.closePath();
                return p;
            }
            case LEMON: {
                Area a = new Area(new Ellipse2D.Double(-r * 1.0, -r * 0.78, r * 2.0, r * 1.56));
                a.add(new Area(new Ellipse2D.Double(-r * 1.2, -r * 0.2, r * 0.45, r * 0.4)));
                a.add(new Area(new Ellipse2D.Double(r * 0.75, -r * 0.2, r * 0.45, r * 0.4)));
                return a;
            }
            case GRAPES: {
                Area cluster = new Area();
                double gr = r * 0.4;
                for (double[] c : grapeCentres()) {
                    cluster.add(new Area(new Ellipse2D.Double(c[0] * r - gr, c[1] * r - gr, gr * 2, gr * 2)));
                }
                return cluster;
            }
            default:
                return new Ellipse2D.Double(-r, -r, r * 2, r * 2);
        }
    }

    /** Grape centres in units of the radius: a small triangular bunch. */
    private static double[][] grapeCentres() {
        return new double[][] {
                {-0.58, -0.45}, {0.0, -0.5}, {0.58, -0.45},
                {-0.3, 0.05}, {0.3, 0.05},
                {0.0, 0.58}};
    }

    // ------------------------------------------------------------------ bomb

    /** Draws a bomb: dark sphere, pulsing red warning glow, burning fuse and a red cross. */
    public static void drawBomb(Graphics2D g, double r, double time) {
        double pulse = 0.5 + 0.5 * Math.sin(time * 9.0);

        g.setPaint(new RadialGradientPaint(new Point2D.Double(0, 0), (float) (r * 1.9),
                new float[] {0f, 0.5f, 1f},
                new Color[] {new Color(255, 40, 40, (int) (110 + 80 * pulse)), new Color(255, 40, 40, 40),
                        new Color(255, 40, 40, 0)}));
        g.fill(new Ellipse2D.Double(-r * 1.9, -r * 1.9, r * 3.8, r * 3.8));

        Ellipse2D.Double body = new Ellipse2D.Double(-r, -r, r * 2, r * 2);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(-r * 0.35, -r * 0.4), (float) (r * 1.6),
                new float[] {0f, 0.55f, 1f},
                new Color[] {new Color(0x6B6B78), new Color(0x2B2B33), new Color(0x08080C)}));
        g.fill(body);
        g.setStroke(new BasicStroke(3.2f));
        g.setColor(new Color(255, 70 + (int) (60 * pulse), 60, 230));
        g.draw(body);

        // cap and fuse
        g.setColor(new Color(0x55555F));
        g.fill(new Rectangle2D.Double(-r * 0.28, -r * 1.22, r * 0.56, r * 0.3));
        g.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0xC9A66B));
        QuadCurve2D.Double fuse = new QuadCurve2D.Double(0, -r * 1.2, r * 0.1, -r * 1.55, r * 0.55, -r * 1.55);
        g.draw(fuse);
        double flicker = 0.7 + 0.3 * Math.sin(time * 40.0);
        double sx = r * 0.55;
        double sy = -r * 1.55;
        g.setColor(new Color(255, 200, 60, 190));
        g.fill(new Ellipse2D.Double(sx - 9 * flicker, sy - 9 * flicker, 18 * flicker, 18 * flicker));
        g.setColor(new Color(255, 245, 190));
        g.fill(new Ellipse2D.Double(sx - 4, sy - 4, 8, 8));

        // red danger cross
        g.setStroke(new BasicStroke(6.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(255, 60, 60));
        g.draw(new Line2D.Double(-r * 0.36, -r * 0.2, r * 0.36, r * 0.52));
        g.draw(new Line2D.Double(r * 0.36, -r * 0.2, -r * 0.36, r * 0.52));
        g.setColor(new Color(255, 255, 255, 90));
        g.fill(new Ellipse2D.Double(-r * 0.7, -r * 0.75, r * 0.4, r * 0.25));
    }

    // ------------------------------------------------------------------ colour helpers

    private static Color lighten(Color c, float amount) {
        int r = c.getRed() + (int) ((255 - c.getRed()) * amount);
        int g = c.getGreen() + (int) ((255 - c.getGreen()) * amount);
        int b = c.getBlue() + (int) ((255 - c.getBlue()) * amount);
        return new Color(r, g, b, c.getAlpha());
    }

    private static Color darken(Color c, float amount) {
        return new Color((int) (c.getRed() * (1 - amount)), (int) (c.getGreen() * (1 - amount)),
                (int) (c.getBlue() * (1 - amount)), c.getAlpha());
    }
}
