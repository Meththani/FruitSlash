import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/**
 * Plays short sound effects that are synthesised in code (no audio files needed).
 * If the computer has no audio device, or anything goes wrong, the game simply stays silent.
 */
public class SoundManager {

    private static final float SAMPLE_RATE = 22050f;

    private final Map<SoundEffect, Clip> clips = new ConcurrentHashMap<>();
    private volatile boolean enabled = true;

    /** @param loadAudio false disables audio completely (used by headless tests). */
    public SoundManager(boolean loadAudio) {
        if (loadAudio) {
            Thread loader = new Thread(this::loadClips, "sound-loader");
            loader.setDaemon(true); // never keeps the application alive
            loader.start();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** Plays an effect if sound is on and the clip loaded; never throws. */
    public void play(SoundEffect effect) {
        if (!enabled) {
            return;
        }
        Clip clip = clips.get(effect);
        if (clip == null) {
            return;
        }
        try {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0);
            clip.start();
        } catch (RuntimeException ex) {
            // ignore: sound is optional
        }
    }

    /** Releases the audio lines. */
    public void shutdown() {
        for (Clip clip : clips.values()) {
            try {
                clip.stop();
                clip.close();
            } catch (RuntimeException ex) {
                // ignore
            }
        }
        clips.clear();
    }

    // ------------------------------------------------------------------ synthesis

    private void loadClips() {
        for (SoundEffect effect : SoundEffect.values()) {
            try {
                byte[] pcm = synthesise(effect);
                AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
                Clip clip = AudioSystem.getClip();
                clip.open(format, pcm, 0, pcm.length);
                clips.put(effect, clip);
            } catch (Exception | LinkageError ex) {
                return; // no audio available on this machine: stay silent
            }
        }
    }

    private static byte[] synthesise(SoundEffect effect) {
        switch (effect) {
            case SLICE:
                return slice();
            case BOMB:
                return bomb();
            case MISS:
                return sweep(0.28, 260, 110, 0.35);
            case UNDO:
                return sweep(0.20, 380, 980, 0.30);
            case CLICK:
                return sweep(0.05, 900, 900, 0.25);
            case LEVEL_UP:
                return arpeggio(new double[] {523.25, 659.25, 783.99, 1046.5}, 0.11);
            case VICTORY:
                return arpeggio(new double[] {523.25, 659.25, 783.99, 1046.5, 783.99, 1046.5, 1318.5}, 0.13);
            case GAME_OVER:
                return arpeggio(new double[] {392.0, 349.23, 311.13, 261.63}, 0.2);
            default:
                return new byte[0];
        }
    }

    private static byte[] slice() {
        int n = (int) (SAMPLE_RATE * 0.16);
        Random rnd = new Random(1);
        double[] s = new double[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double env = Math.pow(1 - t, 2.2);
            double freq = 2200 - 1700 * t;
            phase += 2 * Math.PI * freq / SAMPLE_RATE;
            s[i] = env * (0.35 * (rnd.nextDouble() * 2 - 1) + 0.25 * Math.sin(phase));
        }
        return toPcm(s);
    }

    private static byte[] bomb() {
        int n = (int) (SAMPLE_RATE * 0.65);
        Random rnd = new Random(2);
        double[] s = new double[n];
        double low = 0;
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double env = Math.pow(1 - t, 1.6);
            low += 0.12 * ((rnd.nextDouble() * 2 - 1) - low); // one-pole low-pass of white noise
            phase += 2 * Math.PI * (70 - 35 * t) / SAMPLE_RATE;
            s[i] = env * (1.6 * low + 0.45 * Math.sin(phase));
        }
        return toPcm(s);
    }

    private static byte[] sweep(double seconds, double fromHz, double toHz, double volume) {
        int n = (int) (SAMPLE_RATE * seconds);
        double[] s = new double[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double env = Math.min(1.0, t * 20) * (1 - t);
            phase += 2 * Math.PI * (fromHz + (toHz - fromHz) * t) / SAMPLE_RATE;
            s[i] = volume * env * Math.sin(phase);
        }
        return toPcm(s);
    }

    private static byte[] arpeggio(double[] notes, double noteSeconds) {
        int per = (int) (SAMPLE_RATE * noteSeconds);
        double[] s = new double[per * notes.length + (int) (SAMPLE_RATE * 0.15)];
        for (int k = 0; k < notes.length; k++) {
            double phase = 0;
            for (int i = 0; i < per + (int) (SAMPLE_RATE * 0.15) && k * per + i < s.length; i++) {
                double t = i / (per + SAMPLE_RATE * 0.15);
                double env = Math.min(1.0, i / 120.0) * Math.pow(1 - Math.min(1.0, t), 1.5);
                phase += 2 * Math.PI * notes[k] / SAMPLE_RATE;
                s[k * per + i] += 0.28 * env * (Math.sin(phase) + 0.3 * Math.sin(2 * phase));
            }
        }
        return toPcm(s);
    }

    private static byte[] toPcm(double[] samples) {
        byte[] out = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            double v = Math.max(-1.0, Math.min(1.0, samples[i]));
            short value = (short) Math.round(v * 30000);
            out[2 * i] = (byte) (value & 0xFF);
            out[2 * i + 1] = (byte) ((value >> 8) & 0xFF);
        }
        return out;
    }
}
