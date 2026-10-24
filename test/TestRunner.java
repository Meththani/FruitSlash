import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Lightweight, dependency-free test harness (no JUnit needed).
 * Every check is explicit and deterministic; the process exit code is 0 only if ALL checks pass.
 *
 * Run (after compiling, see README):  java -cp out TestRunner
 */
public class TestRunner {

    private static int passed = 0;
    private static int failed = 0;
    private static String currentGroup = "";

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");

        testObjectArray();
        testSinglyLinkedList();
        testSliceTrail();
        testArrayStack();
        testCircularQueue();
        testBubbleSort();
        testLinearSearch();
        testRecursiveLevelProgression();
        testSegmentGeometry();
        testStateTransitions();
        testSliceScoringAndNoDoubleSlice();
        testBombPenalty();
        testUndoConsistency();
        testUndoInvalidAttempts();
        testLevelProgressionInEngine();
        testSpawnQueueInEngine();
        testMissedFruitAndGameOver();
        testStaleInputIgnored();
        testLeaderboardSaveReload();
        testLeaderboardMalformedData();
        testScoreEntryValidation();
        testRenderSmoke();

        System.out.println();
        System.out.println("==============================================");
        System.out.println("Checks passed: " + passed + "   failed: " + failed);
        System.out.println(failed == 0 ? "RESULT: ALL TESTS PASSED" : "RESULT: SOME TESTS FAILED");
        System.out.println("==============================================");
        System.exit(failed == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------ mini framework

    private static void group(String name) {
        currentGroup = name;
        System.out.println();
        System.out.println("[" + name + "]");
    }

    private static void check(String description, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS  " + description);
        } else {
            failed++;
            System.out.println("  FAIL  " + description + "   (in group: " + currentGroup + ")");
        }
    }

    private static void checkEquals(String description, Object expected, Object actual) {
        boolean same = expected == null ? actual == null : expected.equals(actual);
        if (same) {
            passed++;
            System.out.println("  PASS  " + description);
        } else {
            failed++;
            System.out.println("  FAIL  " + description + "   expected=" + expected + " actual=" + actual
                    + "   (in group: " + currentGroup + ")");
        }
    }

    // ------------------------------------------------------------------ helpers

    private static Fruit still(FruitType type, double x, double y) {
        return new Fruit(type, x, y, 0, 0, 0, 0); // no velocity, no gravity: stays where we put it
    }

    private static Bomb stillBomb(double x, double y) {
        return new Bomb(x, y, 0, 0, 0, 0);
    }

    private static GameEngine newRunningEngine() {
        GameEngine engine = new GameEngine(3, 12345L);
        check("startNewGame works from the menu", engine.startNewGame(0));
        return engine;
    }

    private static void click(GameEngine engine, double x, double y) {
        engine.beginStroke(x, y);
        engine.endStroke();
    }

    private static Path tempDir() throws IOException {
        return Files.createTempDirectory("fruitslash-test");
    }

    private static void deleteRecursively(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
            // best effort clean-up
        }
    }

    // ------------------------------------------------------------------ 1. array

    private static void testObjectArray() {
        group("1. Array (ObjectArray): insert, update, remove");
        ObjectArray array = new ObjectArray(3);
        checkEquals("new array is empty", 0, array.size());
        check("capacity is stored", array.capacity() == 3);

        Fruit a = still(FruitType.APPLE, 10, 10);
        Fruit b = still(FruitType.ORANGE, 20, 20);
        Fruit c = still(FruitType.LEMON, 30, 30);
        check("insert 1", array.add(a));
        check("insert 2", array.add(b));
        check("insert 3", array.add(c));
        check("insert beyond capacity is rejected (no exception)", !array.add(still(FruitType.GRAPES, 1, 1)));
        check("array reports full", array.isFull());
        check("null insert rejected", !new ObjectArray(2).add(null));
        check("get(1) returns second element", array.get(1) == b);

        Fruit mover = new Fruit(FruitType.APPLE, 0, 0, 100, 50, 0, 0);
        mover.update(0.5);
        check("update moves x by vx*dt", Math.abs(mover.getX() - 50) < 1e-9);
        check("update moves y by vy*dt (no gravity)", Math.abs(mover.getY() - 25) < 1e-9);

        b.deactivate();
        int removed = array.compactInactive();
        checkEquals("compactInactive removes exactly the deactivated object", 1, removed);
        checkEquals("size after compaction", 2, array.size());
        check("order is preserved (stable compaction)", array.get(0) == a && array.get(1) == c);

        boolean threw = false;
        try {
            array.get(2);
        } catch (IndexOutOfBoundsException ex) {
            threw = true;
        }
        check("get() beyond size throws IndexOutOfBoundsException", threw);

        check("removeAt(0) succeeds", array.removeAt(0));
        check("removeAt(invalid) returns false", !array.removeAt(5));
        check("remaining element shifted to index 0", array.get(0) == c && array.size() == 1);
        array.clear();
        checkEquals("clear empties the array", 0, array.size());

        ObjectArray many = new ObjectArray(6);
        Fruit[] fs = new Fruit[6];
        for (int i = 0; i < 6; i++) {
            fs[i] = still(FruitType.APPLE, i, i);
            many.add(fs[i]);
        }
        fs[0].deactivate();
        fs[1].deactivate();
        fs[2].deactivate();
        fs[5].deactivate();
        many.compactInactive();
        check("consecutive removals do not skip anything (2 left: indices 3 and 4)",
                many.size() == 2 && many.get(0) == fs[3] && many.get(1) == fs[4]);
    }

    // ------------------------------------------------------------------ 2. linked list

    private static void testSinglyLinkedList() {
        group("2. Singly linked list: insert, remove, size, clear");
        SinglyLinkedList<Integer> list = new SinglyLinkedList<>();
        check("new list is empty", list.isEmpty() && list.size() == 0);
        check("removeFirst on empty list returns null", list.removeFirst() == null);
        check("peekFirst on empty list returns null", list.peekFirst() == null);
        list.addLast(1);
        list.addLast(2);
        list.addLast(3);
        checkEquals("size after 3 addLast", 3, list.size());
        checkEquals("peekFirst is the oldest", 1, list.peekFirst());
        checkEquals("peekLast is the newest", 3, list.peekLast());
        checkEquals("removeFirst returns 1", 1, list.removeFirst());
        checkEquals("removeFirst returns 2", 2, list.removeFirst());
        checkEquals("size is 1", 1, list.size());
        checkEquals("removeFirst returns 3", 3, list.removeFirst());
        check("list empty again", list.isEmpty());
        list.addLast(9);
        checkEquals("tail pointer is valid after list became empty", 9, list.peekLast());
        int sum = 0;
        for (int v : list) {
            sum += v;
        }
        checkEquals("iteration visits elements", 9, sum);
        list.addLast(10);
        list.clear();
        check("clear empties the list", list.isEmpty() && list.size() == 0 && list.peekFirst() == null);
        boolean threw = false;
        try {
            list.addLast(null);
        } catch (NullPointerException ex) {
            threw = true;
        }
        check("null elements are rejected", threw);
    }

    private static void testSliceTrail() {
        group("2b. SliceTrail (bounded linked-list trail)");
        SliceTrail trail = new SliceTrail(5, 1.0);
        trail.beginStroke();
        for (int i = 0; i < 12; i++) {
            trail.addPoint(i, i, 0.0);
        }
        checkEquals("trail never exceeds maxPoints", 5, trail.size());
        double firstX = trail.getPoints().iterator().next().getX();
        checkEquals("oldest points were removed first (head is x=7)", 7.0, firstX);
        trail.update(0.5);
        checkEquals("young points stay", 5, trail.size());
        trail.update(2.0);
        checkEquals("old points expire", 0, trail.size());
        trail.addPoint(1, 1, 3.0);
        trail.clear();
        checkEquals("clear empties the trail", 0, trail.size());
    }

    // ------------------------------------------------------------------ 3. stack

    private static void testArrayStack() {
        group("3. Stack: push, peek, pop, empty behaviour");
        ArrayStack<String> stack = new ArrayStack<>(3);
        check("new stack is empty", stack.isEmpty());
        check("pop on empty returns null", stack.pop() == null);
        check("peek on empty returns null", stack.peek() == null);
        check("push a", stack.push("a"));
        check("push b", stack.push("b"));
        checkEquals("peek returns last pushed", "b", stack.peek());
        checkEquals("peek does not remove", 2, stack.size());
        checkEquals("pop returns b (LIFO)", "b", stack.pop());
        checkEquals("pop returns a", "a", stack.pop());
        check("empty after popping everything", stack.isEmpty());
        check("pop on empty again is safe", stack.pop() == null);
        stack.push("x");
        stack.push("y");
        stack.push("z");
        check("stack reports full", stack.isFull());
        check("push on full stack returns false", !stack.push("w"));
        checkEquals("full stack keeps its top element", "z", stack.peek());
        stack.clear();
        check("clear empties the stack", stack.isEmpty() && stack.size() == 0);
        check("null push rejected", !stack.push(null));
    }

    // ------------------------------------------------------------------ 4. queue

    private static void testCircularQueue() {
        group("4. Queue: FIFO order, enqueue, dequeue, empty behaviour");
        CircularQueue<Integer> queue = new CircularQueue<>(3);
        check("new queue is empty", queue.isEmpty());
        check("dequeue on empty returns null", queue.dequeue() == null);
        check("peek on empty returns null", queue.peek() == null);
        check("enqueue 1", queue.enqueue(1));
        check("enqueue 2", queue.enqueue(2));
        check("enqueue 3", queue.enqueue(3));
        check("enqueue on full queue returns false", !queue.enqueue(4));
        checkEquals("peek returns the front (1)", 1, queue.peek());
        checkEquals("dequeue 1 (FIFO)", 1, queue.dequeue());
        check("wrap-around: enqueue 4 into the freed slot", queue.enqueue(4));
        checkEquals("dequeue 2", 2, queue.dequeue());
        checkEquals("dequeue 3", 3, queue.dequeue());
        checkEquals("dequeue 4 (came from the wrapped slot)", 4, queue.dequeue());
        check("queue empty at the end", queue.isEmpty() && queue.size() == 0);
        queue.enqueue(7);
        queue.clear();
        check("clear empties the queue", queue.isEmpty() && queue.peek() == null);
        // stress the wrap-around many times
        boolean ordered = true;
        int next = 0;
        int expected = 0;
        for (int round = 0; round < 50; round++) {
            queue.enqueue(next++);
            queue.enqueue(next++);
            ordered &= queue.dequeue() == expected++;
            ordered &= queue.dequeue() == expected++;
        }
        check("FIFO order holds over 100 wrapped operations", ordered && queue.isEmpty());
    }

    // ------------------------------------------------------------------ 5. bubble sort

    private static ScoreEntry[] entries(Object... nameScorePairs) {
        ScoreEntry[] result = new ScoreEntry[nameScorePairs.length / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = new ScoreEntry((String) nameScorePairs[2 * i], (Integer) nameScorePairs[2 * i + 1]);
        }
        return result;
    }

    private static boolean isDescending(ScoreEntry[] e) {
        for (int i = 0; i + 1 < e.length; i++) {
            if (e[i].getScore() < e[i + 1].getScore()) {
                return false;
            }
        }
        return true;
    }

    private static void testBubbleSort() {
        group("5. Bubble Sort (leaderboard ranking)");
        ScoreEntry[] data = entries("A", 50, "B", 300, "C", 120, "D", 10, "E", 300, "F", 75);
        LeaderboardManager.bubbleSortScoresDescending(data, data.length);
        check("unsorted input becomes descending", isDescending(data));
        checkEquals("highest score first", 300, data[0].getScore());
        checkEquals("lowest score last", 10, data[5].getScore());

        ScoreEntry[] ties = entries("first", 100, "second", 100, "third", 100, "low", 5, "high", 900);
        LeaderboardManager.bubbleSortScoresDescending(ties);
        checkEquals("stable: 900 first", "high", ties[0].getName());
        check("stable: equal scores keep their original order",
                ties[1].getName().equals("first") && ties[2].getName().equals("second")
                        && ties[3].getName().equals("third"));
        checkEquals("lowest last", "low", ties[4].getName());

        ScoreEntry[] allEqual = entries("x", 5, "y", 5, "z", 5);
        LeaderboardManager.bubbleSortScoresDescending(allEqual);
        check("all-equal input unchanged", allEqual[0].getName().equals("x") && allEqual[2].getName().equals("z"));

        ScoreEntry[] reversed = entries("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
        LeaderboardManager.bubbleSortScoresDescending(reversed);
        check("worst case (ascending input) is fully reversed", isDescending(reversed) && reversed[0].getScore() == 5);

        ScoreEntry[] already = entries("a", 9, "b", 5, "c", 1);
        LeaderboardManager.bubbleSortScoresDescending(already);
        check("already sorted input stays sorted", isDescending(already) && already[0].getName().equals("a"));

        ScoreEntry[] empty = new ScoreEntry[0];
        LeaderboardManager.bubbleSortScoresDescending(empty);
        check("empty array is handled", empty.length == 0);
        ScoreEntry[] single = entries("solo", 42);
        LeaderboardManager.bubbleSortScoresDescending(single);
        check("single element is handled", single.length == 1 && single[0].getScore() == 42);
        LeaderboardManager.bubbleSortScoresDescending(null);
        check("null array does not crash", true);

        ScoreEntry[] partial = entries("a", 1, "b", 2, "c", 3, "d", 4);
        LeaderboardManager.bubbleSortScoresDescending(partial, 2);
        check("only the first `length` elements are sorted",
                partial[0].getScore() == 2 && partial[1].getScore() == 1 && partial[2].getScore() == 3);
    }

    // ------------------------------------------------------------------ 6. linear search

    private static void testLinearSearch() {
        group("6. Linear Search (player lookup)");
        ScoreEntry[] data = entries("Alice", 500, "Bob", 400, "Cara", 300, "alice", 100);
        checkEquals("finds an existing name", 1, LeaderboardManager.linearSearchPlayer(data, data.length, "Bob"));
        checkEquals("case-insensitive match", 2, LeaderboardManager.linearSearchPlayer(data, data.length, "cArA"));
        checkEquals("first (best-ranked) match is returned when a name occurs twice", 0,
                LeaderboardManager.linearSearchPlayer(data, data.length, "ALICE"));
        checkEquals("surrounding spaces are ignored", 1, LeaderboardManager.linearSearchPlayer(data, data.length, "  bob "));
        checkEquals("missing name returns -1", -1, LeaderboardManager.linearSearchPlayer(data, data.length, "Zed"));
        checkEquals("partial name is not an exact match", -1, LeaderboardManager.linearSearchPlayer(data, data.length, "Al"));
        checkEquals("blank name returns -1", -1, LeaderboardManager.linearSearchPlayer(data, data.length, "   "));
        checkEquals("null name returns -1", -1, LeaderboardManager.linearSearchPlayer(data, data.length, null));
        checkEquals("null array returns -1", -1, LeaderboardManager.linearSearchPlayer(null, 0, "Bob"));
        checkEquals("empty array returns -1", -1, LeaderboardManager.linearSearchPlayer(new ScoreEntry[0], 0, "Bob"));
        checkEquals("length limit is respected (Cara is beyond length 2)", -1,
                LeaderboardManager.linearSearchPlayer(data, 2, "Cara"));
    }

    // ------------------------------------------------------------------ 7. recursion

    private static void testRecursiveLevelProgression() {
        group("7. Recursive level progression");
        LevelConfig l1 = LevelConfig.first();
        checkEquals("level 1 is the base configuration", 1, l1.getLevel());
        LevelConfig l3 = LevelManager.advanceLevelRecursively(l1, 3, GameConfig.MAX_LEVEL);
        checkEquals("recursion reaches the requested level 3", 3, l3.getLevel());
        LevelConfig l2 = LevelManager.configForLevel(2);
        check("target grows with the level", l3.getFruitTarget() > l2.getFruitTarget()
                && l2.getFruitTarget() > l1.getFruitTarget());
        check("launch waves get busier", l3.getWaveInterval() < l1.getWaveInterval());
        check("launch speed increases", l3.getSpeedScale() > l1.getSpeedScale());
        check("bomb chance increases", l3.getBombChance() > l1.getBombChance());

        checkEquals("base case: target equal to current returns immediately", 1,
                LevelManager.advanceLevelRecursively(l1, 1, 8).getLevel());
        checkEquals("maximum-level base case: target above max is clamped", 8,
                LevelManager.advanceLevelRecursively(l1, 99, 8).getLevel());
        checkEquals("maximum level reached exactly", 8, LevelManager.configForLevel(8).getLevel());
        checkEquals("target below 1 is clamped to level 1", 1, LevelManager.advanceLevelRecursively(l1, -5, 8).getLevel());
        checkEquals("invalid maximum (0) is treated as 1", 1, LevelManager.advanceLevelRecursively(l1, 5, 0).getLevel());
        checkEquals("custom maximum 3 clamps a target of 10", 3, LevelManager.advanceLevelRecursively(l1, 10, 3).getLevel());
        LevelConfig atMax = LevelManager.configForLevel(GameConfig.MAX_LEVEL);
        check("difficulty values stay within their caps at the maximum level",
                atMax.getSpeedScale() <= 1.40 && atMax.getBombChance() <= 0.35
                        && atMax.getWaveInterval() >= 0.70 && atMax.getMaxWaveSize() <= 6);
        checkEquals("never recurses past the current level (already beyond target)", 8,
                LevelManager.advanceLevelRecursively(atMax, 2, 8).getLevel());
        // huge target must terminate quickly without StackOverflowError
        boolean ok;
        try {
            ok = LevelManager.advanceLevelRecursively(l1, Integer.MAX_VALUE, 8).getLevel() == 8;
        } catch (StackOverflowError error) {
            ok = false;
        }
        check("huge target terminates safely (no stack overflow)", ok);
    }

    // ------------------------------------------------------------------ geometry / states

    private static void testSegmentGeometry() {
        group("Collision geometry");
        check("segment passing through a circle hits",
                GameObject.segmentHitsCircle(0, 0, 100, 0, 50, 5, 10));
        check("segment far from the circle misses",
                !GameObject.segmentHitsCircle(0, 0, 100, 0, 50, 50, 10));
        check("segment ending before the circle misses (clamped to the end point)",
                !GameObject.segmentHitsCircle(0, 0, 20, 0, 50, 0, 10));
        check("zero-length segment (a click) inside the circle hits",
                GameObject.segmentHitsCircle(50, 50, 50, 50, 55, 50, 10));
        check("zero-length segment outside the circle misses",
                !GameObject.segmentHitsCircle(50, 50, 50, 50, 90, 50, 10));
    }

    private static void testStateTransitions() {
        group("Game state machine");
        GameEngine engine = new GameEngine(3, 1L);
        checkEquals("engine starts in MENU", GameState.MENU, engine.getState());
        check("MENU -> PAUSED is rejected", !engine.pause() && engine.getState() == GameState.MENU);
        check("MENU -> PLAYING via requestState is rejected", !engine.requestState(GameState.PLAYING));
        check("MENU -> GAME_OVER is rejected", !engine.requestState(GameState.GAME_OVER));
        check("MENU -> LEADERBOARD allowed", engine.requestState(GameState.LEADERBOARD));
        check("LEADERBOARD -> MENU allowed", engine.requestState(GameState.MENU));
        check("MENU -> HOW_TO_PLAY allowed", engine.requestState(GameState.HOW_TO_PLAY));
        check("HOW_TO_PLAY -> LEADERBOARD is rejected", !engine.requestState(GameState.LEADERBOARD));
        engine.requestState(GameState.MENU);
        check("startNewGame from MENU", engine.startNewGame(0));
        check("starting while already PLAYING is rejected", !engine.startNewGame(0));
        check("PLAYING -> PAUSED", engine.pause());
        check("PAUSED -> PLAYING (resume)", engine.resume());
        engine.pause();
        check("PAUSED -> MENU allowed", engine.requestState(GameState.MENU));
        check("resume from MENU is rejected", !engine.resume());
        check("GameState helper rejects same-state transition", !GameState.MENU.canTransitionTo(GameState.MENU));
        check("LEVEL_TRANSITION can only go to PLAYING",
                GameState.LEVEL_TRANSITION.canTransitionTo(GameState.PLAYING)
                        && !GameState.LEVEL_TRANSITION.canTransitionTo(GameState.MENU));
    }

    // ------------------------------------------------------------------ engine behaviour

    private static void testSliceScoringAndNoDoubleSlice() {
        group("8a. Scoring and single-slice guarantee");
        GameEngine engine = newRunningEngine();
        Fruit apple = still(FruitType.APPLE, 600, 300);
        engine.getObjects().add(apple);

        engine.beginStroke(500, 300);
        engine.extendStroke(700, 300); // drag across the apple
        checkEquals("first slice: score = base points", FruitType.APPLE.getPoints(), engine.getScore());
        checkEquals("first slice: fruit counter", 1, engine.getFruitsSliced());
        checkEquals("first slice: combo", 1, engine.getCombo());
        checkEquals("first slice: history has one event", 1, engine.getHistorySize());
        check("sliced fruit is removed from the active array", engine.getObjects().size() == 0);

        engine.extendStroke(500, 300); // drag back over the same place
        engine.extendStroke(700, 300);
        checkEquals("no repeated scoring from repeated mouse events", FruitType.APPLE.getPoints(), engine.getScore());
        checkEquals("fruit counter unchanged", 1, engine.getFruitsSliced());
        engine.endStroke();

        // second fruit shortly afterwards -> combo of 2 with bonus
        Fruit orange = still(FruitType.ORANGE, 400, 400);
        engine.getObjects().add(orange);
        click(engine, 400, 400);
        int expected = FruitType.APPLE.getPoints() + FruitType.ORANGE.getPoints() + GameConfig.COMBO_BONUS_PER_STEP;
        checkEquals("combo bonus is added for the second quick slice", expected, engine.getScore());
        checkEquals("combo is 2", 2, engine.getCombo());

        // combo expires with time
        engine.update(0.05);
        for (int i = 0; i < 30; i++) {
            engine.update(0.05); // 1.5 s > COMBO_WINDOW
        }
        checkEquals("combo resets after the window expires", 0, engine.getCombo());
    }

    private static void testBombPenalty() {
        group("8b. Bomb penalty");
        GameEngine engine = newRunningEngine();
        engine.getObjects().add(still(FruitType.WATERMELON, 300, 300));
        click(engine, 300, 300);
        engine.getObjects().add(still(FruitType.WATERMELON, 500, 300));
        click(engine, 500, 300);
        int scoreBefore = engine.getScore();
        check("score is at least the bomb penalty before the bomb", scoreBefore >= GameConfig.BOMB_SCORE_PENALTY);

        engine.getObjects().add(stillBomb(800, 300));
        click(engine, 800, 300);
        checkEquals("bomb costs one life", 2, engine.getLives());
        checkEquals("bomb removes the penalty from the score", scoreBefore - GameConfig.BOMB_SCORE_PENALTY, engine.getScore());
        checkEquals("bomb resets the combo", 0, engine.getCombo());
        checkEquals("bomb does not count as a sliced fruit", 2, engine.getFruitsSliced());
        check("still PLAYING with lives left", engine.getState() == GameState.PLAYING);

        // score never goes negative
        GameEngine zero = newRunningEngine();
        zero.getObjects().add(stillBomb(500, 300));
        click(zero, 500, 300);
        checkEquals("score cannot go below zero", 0, zero.getScore());
        checkEquals("life lost even at zero score", 2, zero.getLives());

        // three bombs end the game
        GameEngine dead = newRunningEngine();
        for (int i = 0; i < 3; i++) {
            dead.getObjects().add(stillBomb(300 + i * 200, 300));
            click(dead, 300 + i * 200, 300);
        }
        checkEquals("losing the last life ends the run", GameState.GAME_OVER, dead.getState());
        checkEquals("lives reach zero", 0, dead.getLives());
    }

    private static void testUndoConsistency() {
        group("8c. Score and undo consistency");
        GameEngine engine = newRunningEngine();
        engine.getObjects().add(still(FruitType.APPLE, 600, 300));
        click(engine, 600, 300);
        int scoreAfterSlice = engine.getScore();
        checkEquals("score after slice", 10, scoreAfterSlice);

        GameEngine.UndoResult result = engine.tryUndo();
        checkEquals("undo succeeds", GameEngine.UndoResult.SUCCESS, result);
        checkEquals("undo reverses the score exactly", 0, engine.getScore());
        checkEquals("undo reverses the fruit counter", 0, engine.getFruitsSliced());
        checkEquals("undo reverses the level counter", 0, engine.getLevelFruitsSliced());
        checkEquals("undo restores the combo", 0, engine.getCombo());
        checkEquals("undo consumes one charge", GameConfig.INITIAL_UNDO_CHARGES - 1, engine.getUndoCharges());
        checkEquals("history is empty after undo", 0, engine.getHistorySize());
        checkEquals("exactly one object was restored (no duplicates)", 1, engine.getObjects().size());
        GameObject restored = engine.getObjects().get(0);
        check("restored object is a fruit at its original place",
                !restored.isBomb() && Math.abs(restored.getX() - 600) < 1e-9 && Math.abs(restored.getY() - 300) < 1e-9);
        check("restored object is active and un-sliced", restored.isActive() && !restored.isSliced());

        click(engine, 600, 300);
        checkEquals("restored object is briefly immune: same click does not slice it", 0, engine.getScore());
        for (int i = 0; i < 10; i++) {
            engine.update(0.05); // update() clamps each step to 0.05 s, so 10 steps = 0.5 s > 0.45 s immunity
        }
        click(engine, 600, 300);
        checkEquals("after the immunity it can be sliced again and scores once", 10, engine.getScore());
        checkEquals("history has the new slice only once", 1, engine.getHistorySize());

        // undo of a bomb gives the life and the penalty back
        GameEngine b = newRunningEngine();
        b.getObjects().add(still(FruitType.WATERMELON, 200, 200));
        click(b, 200, 200);
        b.getObjects().add(still(FruitType.WATERMELON, 400, 200));
        click(b, 400, 200);
        int before = b.getScore();
        b.getObjects().add(stillBomb(900, 300));
        click(b, 900, 300);
        check("bomb cost a life and points", b.getLives() == 2 && b.getScore() == before - GameConfig.BOMB_SCORE_PENALTY);
        checkEquals("undo of the bomb succeeds", GameEngine.UndoResult.SUCCESS, b.tryUndo());
        checkEquals("life is restored", 3, b.getLives());
        checkEquals("penalty is returned", before, b.getScore());
        check("the bomb is restored as one object", b.getObjects().size() == 1 && b.getObjects().get(0).isBomb());
        checkEquals("combo returns to its pre-bomb value (still inside its time window)", 2, b.getCombo());

        // LIFO order across several slices
        GameEngine lifo = newRunningEngine();
        lifo.getObjects().add(still(FruitType.APPLE, 200, 200));
        click(lifo, 200, 200);
        lifo.getObjects().add(still(FruitType.WATERMELON, 400, 200));
        click(lifo, 400, 200);
        int total = lifo.getScore();
        lifo.tryUndo();
        checkEquals("most recent slice is undone first (watermelon: 20 + combo bonus 2)", total - 22, lifo.getScore());
        lifo.tryUndo();
        checkEquals("then the older slice", 0, lifo.getScore());
        checkEquals("combo back to zero", 0, lifo.getCombo());
        checkEquals("two charges used", 2, lifo.getUndosUsed());
    }

    private static void testUndoInvalidAttempts() {
        group("8d. Invalid undo attempts");
        GameEngine engine = new GameEngine(3, 5L);
        checkEquals("undo in MENU is refused", GameEngine.UndoResult.NOT_PLAYING, engine.tryUndo());
        engine.startNewGame(0);
        checkEquals("undo with an empty stack is refused", GameEngine.UndoResult.NOTHING_TO_UNDO, engine.tryUndo());
        checkEquals("failed undo does not use a charge", GameConfig.INITIAL_UNDO_CHARGES, engine.getUndoCharges());

        // too old
        engine.getObjects().add(still(FruitType.LEMON, 500, 300));
        click(engine, 500, 300);
        for (int i = 0; i < 80; i++) { // 4 s > UNDO_WINDOW; no object stays in the array that could be missed
            engine.update(0.05);
            clearArray(engine);
        }
        check("old history is pruned automatically", engine.getHistorySize() == 0);
        int scoreBefore = engine.getScore();
        checkEquals("undo of an expired slice is refused", GameEngine.UndoResult.NOTHING_TO_UNDO, engine.tryUndo());
        checkEquals("refused undo does not change the score", scoreBefore, engine.getScore());

        // no charges
        GameEngine noCharges = newRunningEngine();
        int attempts = 0;
        while (noCharges.getUndoCharges() > 0 && attempts++ < 10) {
            noCharges.getObjects().add(still(FruitType.APPLE, 300, 300));
            click(noCharges, 300, 300);
            noCharges.tryUndo();
        }
        checkEquals("all charges were used", 0, noCharges.getUndoCharges());
        noCharges.getObjects().add(still(FruitType.APPLE, 300, 300));
        click(noCharges, 300, 300);
        int score = noCharges.getScore();
        checkEquals("undo without charges is refused", GameEngine.UndoResult.NO_CHARGES, noCharges.tryUndo());
        checkEquals("score unchanged after refused undo", score, noCharges.getScore());
        checkEquals("slice event is kept after a refused undo", 1, noCharges.getHistorySize());

        // no room to restore
        GameEngine full = newRunningEngine();
        full.getObjects().add(still(FruitType.APPLE, 100, 100));
        click(full, 100, 100);
        while (!full.getObjects().isFull()) {
            full.getObjects().add(still(FruitType.GRAPES, 1000, 600)); // far away from the click
        }
        checkEquals("undo with a full array is refused", GameEngine.UndoResult.NO_ROOM, full.tryUndo());
        checkEquals("event is kept so the player can retry", 1, full.getHistorySize());
        check("score unchanged when there was no room", full.getScore() == 10);

        // after the run ended
        GameEngine over = newRunningEngine();
        over.getObjects().add(still(FruitType.APPLE, 300, 300));
        click(over, 300, 300);
        for (int i = 0; i < 3; i++) {
            over.getObjects().add(stillBomb(500 + i * 150, 500));
            click(over, 500 + i * 150, 500);
        }
        checkEquals("run is over", GameState.GAME_OVER, over.getState());
        checkEquals("undo cannot restore anything in a finished game", GameEngine.UndoResult.NOT_PLAYING, over.tryUndo());
        checkEquals("no objects exist in the finished game", 0, over.getObjects().size());

        // undo after pause
        GameEngine paused = newRunningEngine();
        paused.getObjects().add(still(FruitType.APPLE, 300, 300));
        click(paused, 300, 300);
        paused.pause();
        checkEquals("undo while paused is refused", GameEngine.UndoResult.NOT_PLAYING, paused.tryUndo());
    }

    private static void clearArray(GameEngine engine) {
        ObjectArray array = engine.getObjects();
        for (int i = 0; i < array.size(); i++) {
            array.get(i).deactivate();
        }
        array.compactInactive();
    }

    private static void testLevelProgressionInEngine() {
        group("Level progression inside the engine (recursion is invoked)");
        GameEngine engine = newRunningEngine();
        int target = engine.getLevelTarget();
        checkEquals("level 1 target comes from the level configuration", LevelManager.configForLevel(1).getFruitTarget(), target);
        for (int i = 0; i < target; i++) {
            engine.getObjects().add(still(FruitType.APPLE, 640, 360));
            click(engine, 640, 360);
        }
        checkEquals("slicing the target amount completes the level", GameState.LEVEL_TRANSITION, engine.getState());
        check("level bonus was awarded", engine.getLastLevelBonus() == GameConfig.LEVEL_BONUS_PER_LEVEL);
        checkEquals("pending configuration is level 2 (built recursively)", 2, engine.getPendingConfig().getLevel());
        checkEquals("playfield is cleared for the transition", 0, engine.getObjects().size());
        checkEquals("undo history is cleared for the transition", 0, engine.getHistorySize());
        checkEquals("undo cannot be used during the transition", GameEngine.UndoResult.NOT_PLAYING, engine.tryUndo());
        check("mouse input is ignored during the transition", ignoredStroke(engine));
        checkEquals("an undo charge is rewarded", GameConfig.INITIAL_UNDO_CHARGES + 1, engine.getUndoCharges());

        for (int i = 0; i < 100 && engine.getState() == GameState.LEVEL_TRANSITION; i++) {
            engine.update(0.05);
        }
        checkEquals("the transition ends in PLAYING", GameState.PLAYING, engine.getState());
        checkEquals("level number advanced", 2, engine.getLevel());
        checkEquals("fruit counter of the new level starts at 0", 0, engine.getLevelFruitsSliced());
        check("the new level has a larger target", engine.getLevelTarget() > target);
        check("the spawn queue was refilled for the new level", engine.getSpawnQueueSize() > 0);

        // play through every level to reach victory
        GameEngine run = newRunningEngine();
        int safety = 0;
        while (run.getState() != GameState.VICTORY && safety++ < 5000) {
            if (run.getState() == GameState.LEVEL_TRANSITION) {
                run.update(0.05);
                continue;
            }
            run.getObjects().add(still(FruitType.APPLE, 640, 360));
            click(run, 640, 360);
            run.update(0.05);
            clearArray(run); // avoid random spawns interfering with lives
        }
        checkEquals("slicing through all levels ends in VICTORY", GameState.VICTORY, run.getState());
        checkEquals("victory happens at the maximum level", GameConfig.MAX_LEVEL, run.getLevel());
        check("victory does not advance beyond the maximum level", run.getLevel() <= GameConfig.MAX_LEVEL);
        check("victory screen can go back to the menu", run.requestState(GameState.MENU));
    }

    private static boolean ignoredStroke(GameEngine engine) {
        int before = engine.getTrail().size();
        engine.beginStroke(100, 100);
        engine.extendStroke(200, 200);
        return engine.getTrail().size() == before;
    }

    private static void testSpawnQueueInEngine() {
        group("Queue in the engine: spawn requests are consumed FIFO");
        GameEngine engine = new GameEngine(3, 99L);
        engine.startNewGame(0);
        check("queue is filled with upcoming requests at start", engine.getSpawnQueueSize() >= GameConfig.SPAWN_LOOKAHEAD);
        int maxActive = 0;
        int launched = 0;
        int previous = 0;
        for (int i = 0; i < 400; i++) {
            engine.update(0.016);
            if (engine.getState() != GameState.PLAYING) {
                break;
            }
            int now = engine.getObjects().size();
            if (now > previous) {
                launched += now - previous;
            }
            previous = now;
            maxActive = Math.max(maxActive, now);
            if (engine.getSpawnQueueSize() < 1) {
                check("queue never runs dry while playing", false);
            }
        }
        check("objects were launched from the queue", launched > 0);
        check("array never exceeds its capacity", maxActive <= GameConfig.MAX_ACTIVE_OBJECTS);

        // identical seeds give identical spawns: the queue is deterministic
        GameEngine a = new GameEngine(3, 7L);
        GameEngine b = new GameEngine(3, 7L);
        a.startNewGame(0);
        b.startNewGame(0);
        boolean same = true;
        for (int i = 0; i < 200; i++) {
            a.update(0.016);
            b.update(0.016);
            if (a.getObjects().size() != b.getObjects().size()) {
                same = false;
            } else if (a.getObjects().size() > 0 && Math.abs(a.getObjects().get(0).getX() - b.getObjects().get(0).getX()) > 1e-9) {
                same = false;
            }
        }
        check("same seed gives the same launch sequence", same);

        // queue and bombs: requests contain full launch data
        LevelConfig cfg = LevelManager.configForLevel(4);
        SpawnRequest fruit = LevelManager.createRequest(cfg, new java.util.Random(1), false, 0.5);
        SpawnRequest bomb = LevelManager.createRequest(cfg, new java.util.Random(1), true, 0.2);
        check("fruit request carries type, position, speed and delay",
                fruit.getKind() == SpawnRequest.Kind.FRUIT && fruit.getFruitType() != null
                        && fruit.getVy() < 0 && fruit.getX() > 0 && fruit.getDelay() == 0.5);
        check("bomb request has no fruit type", bomb.getKind() == SpawnRequest.Kind.BOMB && bomb.getFruitType() == null);
    }

    private static void testMissedFruitAndGameOver() {
        group("Missed fruit, lives and game over");
        GameEngine engine = new GameEngine(2, 3L);
        engine.startNewGame(0);
        // a fruit that is already falling below the screen
        Fruit falling = new Fruit(FruitType.APPLE, 300, GameConfig.WORLD_HEIGHT + 100, 0, 200, 0, 0);
        engine.getObjects().add(falling);
        engine.update(0.016);
        checkEquals("a missed fruit costs one life", 1, engine.getLives());
        check("missed fruit is removed from the array",
                engine.getObjects().size() == 0 || engine.getObjects().get(0) != falling);

        // a missed bomb is harmless
        Bomb bomb = new Bomb(300, GameConfig.WORLD_HEIGHT + 100, 0, 200, 0, 0);
        engine.getObjects().add(bomb);
        engine.update(0.016);
        checkEquals("a missed bomb costs nothing", 1, engine.getLives());

        Fruit second = new Fruit(FruitType.LEMON, 600, GameConfig.WORLD_HEIGHT + 100, 0, 200, 0, 0);
        engine.getObjects().add(second);
        engine.update(0.016);
        checkEquals("last life lost -> GAME_OVER", GameState.GAME_OVER, engine.getState());
        check("result state allows returning from the leaderboard",
                engine.requestState(GameState.LEADERBOARD) && engine.requestState(GameState.GAME_OVER));
        check("restart is possible from GAME_OVER", engine.startNewGame(0) && engine.getState() == GameState.PLAYING);
        checkEquals("restart resets lives", 2, engine.getLives());
        checkEquals("restart resets score", 0, engine.getScore());
        checkEquals("restart resets the level", 1, engine.getLevel());
        checkEquals("restart resets undo charges", GameConfig.INITIAL_UNDO_CHARGES, engine.getUndoCharges());
        checkEquals("restart clears objects", 0, engine.getObjects().size());
    }

    private static void testStaleInputIgnored() {
        group("Stale mouse events");
        GameEngine engine = newRunningEngine();
        engine.getObjects().add(still(FruitType.APPLE, 500, 300));
        engine.beginStroke(100, 100); // stroke starts away from the fruit
        engine.pause();
        engine.extendStroke(500, 300);
        engine.resume();
        engine.extendStroke(500, 300); // stroke was cancelled by the pause: must be ignored
        checkEquals("a drag that began before a pause cannot slice after it", 0, engine.getScore());
        engine.beginStroke(500, 300);
        checkEquals("a new press works normally", 10, engine.getScore());

        GameEngine over = newRunningEngine();
        over.getObjects().add(still(FruitType.APPLE, 500, 300));
        for (int i = 0; i < 3; i++) {
            over.getObjects().add(stillBomb(100 + i * 100, 100));
            click(over, 100 + i * 100, 100);
        }
        check("game is over", over.getState() == GameState.GAME_OVER);
        over.beginStroke(500, 300);
        over.extendStroke(520, 300);
        checkEquals("slicing after game over is impossible", 0, over.getScore());
        checkEquals("trail stays empty after game over", 0, over.getTrail().size());

        // paused game does not move
        GameEngine p = newRunningEngine();
        Fruit mover = new Fruit(FruitType.APPLE, 300, 300, 100, 0, 0, 0);
        p.getObjects().add(mover);
        p.pause();
        double xBefore = mover.getX();
        p.update(0.05);
        check("objects do not move while paused", mover.getX() == xBefore);
        double timeBefore = p.getGameTime();
        p.update(0.05);
        check("game time does not advance while paused", p.getGameTime() == timeBefore);
        GameEngine menu = new GameEngine(3, 1);
        menu.update(0.05);
        check("nothing updates on the menu", menu.getObjects().size() == 0 && menu.getGameTime() == 0);
    }

    // ------------------------------------------------------------------ leaderboard persistence

    private static void testLeaderboardSaveReload() throws IOException {
        group("9. Leaderboard save / reload");
        Path dir = tempDir();
        try {
            Path file = dir.resolve("sub").resolve("board.txt"); // directory does not exist yet
            LeaderboardManager manager = new LeaderboardManager(file);
            checkEquals("missing file gives an empty leaderboard", 0, manager.size());
            checkEquals("best score of an empty board is 0", 0, manager.getBestScore());

            ScoreEntry a = manager.addScore("Alice", 300);
            manager.addScore("Bob", 500);
            manager.addScore("Cara", 300);
            manager.addScore("Dan", 100);
            check("addScore returns the stored entry", a != null && a.getName().equals("Alice"));
            check("file was created (including its folder)", Files.isRegularFile(file));

            ScoreEntry[] ranked = manager.getRankedEntries();
            check("entries are ranked descending", isDescending(ranked));
            checkEquals("Bob (500) is rank 1", "Bob", ranked[0].getName());
            check("tie keeps insertion order: Alice before Cara",
                    ranked[1].getName().equals("Alice") && ranked[2].getName().equals("Cara"));
            checkEquals("best score is 500", 500, manager.getBestScore());
            checkEquals("rank of an exact entry", 2, manager.getRankOf(a));

            LeaderboardManager reloaded = new LeaderboardManager(file);
            checkEquals("reload keeps all entries", 4, reloaded.size());
            ScoreEntry[] again = reloaded.getRankedEntries();
            boolean same = again.length == ranked.length;
            for (int i = 0; same && i < again.length; i++) {
                same = again[i].getName().equals(ranked[i].getName()) && again[i].getScore() == ranked[i].getScore();
            }
            check("reloaded order and values are identical", same);
            checkEquals("search works after reload (case-insensitive)", 1, reloaded.searchPlayerRank("bOB"));
            checkEquals("search for an unknown player", -1, reloaded.searchPlayerRank("Nobody"));

            reloaded.addScore("Eve", 700);
            LeaderboardManager third = new LeaderboardManager(file);
            checkEquals("adding a score never loses previous scores", 5, third.size());
            checkEquals("new high score ranks first after reload", "Eve", third.getRankedEntries()[0].getName());

            check("invalid score is rejected", reloaded.addScore("Zed", -5) == null);
            check("empty name is rejected", reloaded.addScore("   ", 10) == null);
            check("rejected entries are not stored", new LeaderboardManager(file).size() == 5);
            check("no temporary file is left behind", !Files.exists(dir.resolve("sub").resolve("board.txt.tmp")));
        } finally {
            deleteRecursively(dir);
        }
    }

    private static void testLeaderboardMalformedData() throws IOException {
        group("10. Missing / malformed saved data");
        Path dir = tempDir();
        try {
            Path empty = dir.resolve("empty.txt");
            Files.write(empty, new byte[0]);
            checkEquals("empty file gives an empty leaderboard", 0, new LeaderboardManager(empty).size());

            Path garbage = dir.resolve("garbage.txt");
            String content = "# header\n"
                    + "Alice\t120\n"
                    + "this line has no tab\n"
                    + "Bob\tabc\n"
                    + "\t50\n"
                    + "Cara\t-7\n"
                    + "Dan\t99999999999\n"
                    + "Eve\t300\n"
                    + "Bad\u0000Name\t10\n"
                    + "\n"
                    + "Frank\t0\n"
                    + "Tab\tIn\tName\t77\n";
            Files.write(garbage, content.getBytes(StandardCharsets.UTF_8));
            LeaderboardManager manager = new LeaderboardManager(garbage);
            ScoreEntry[] ranked = manager.getRankedEntries();
            checkEquals("only the valid lines are loaded (Alice, Eve, Frank)", 3, manager.size());
            checkEquals("best valid entry is first", "Eve", ranked[0].getName());
            check("malformed lines were counted as skipped", manager.getSkippedLineCount() >= 6);
            check("the game can still add scores to a damaged file", manager.addScore("Gina", 50) != null);
            check("saving repaired the file (reload has 4 entries)", new LeaderboardManager(garbage).size() == 4);

            Path binary = dir.resolve("binary.txt");
            byte[] junk = new byte[256];
            for (int i = 0; i < junk.length; i++) {
                junk[i] = (byte) (i * 7 + 130);
            }
            Files.write(binary, junk);
            boolean survived;
            try {
                survived = new LeaderboardManager(binary).size() >= 0;
            } catch (RuntimeException ex) {
                survived = false;
            }
            check("a binary junk file does not crash the loader", survived);

            Path asDirectory = dir.resolve("iamadirectory");
            Files.createDirectories(asDirectory);
            check("a directory in place of the file does not crash", new LeaderboardManager(asDirectory).size() == 0);

            check("default save path is relative or in the user's home (no hard-coded user name)",
                    !LeaderboardManager.defaultSavePath().toString().contains("C:\\Users\\"));
        } finally {
            deleteRecursively(dir);
        }
    }

    private static void testScoreEntryValidation() {
        group("Name / score validation");
        checkEquals("sanitize trims and collapses spaces", "Ann Lee", ScoreEntry.sanitizeName("   Ann    Lee  "));
        checkEquals("sanitize removes tabs, newlines and symbols", "Bob", ScoreEntry.sanitizeName("B\to\nb;,<>"));
        checkEquals("sanitize limits the length", ScoreEntry.MAX_NAME_LENGTH,
                ScoreEntry.sanitizeName("abcdefghijklmnopqrstuvwxyz").length());
        checkEquals("sanitize null gives empty", "", ScoreEntry.sanitizeName(null));
        check("tryCreate rejects an empty name", ScoreEntry.tryCreate("", 5) == null);
        check("tryCreate rejects a score above the maximum", ScoreEntry.tryCreate("A", ScoreEntry.MAX_SCORE + 1) == null);
        check("tryCreate accepts zero", ScoreEntry.tryCreate("A", 0) != null);
        check("parseLine round-trips toLine", ScoreEntry.parseLine(new ScoreEntry("Zoe_1", 42).toLine()).getScore() == 42);
        check("parseLine rejects null", ScoreEntry.parseLine(null) == null);
    }

    // ------------------------------------------------------------------ rendering smoke test

    private static void testRenderSmoke() {
        group("GUI render smoke test (headless, every screen)");
        try {
            Path dir = tempDir();
            try {
                LeaderboardManager board = new LeaderboardManager(dir.resolve("render.txt"));
                board.addScore("Render", 123);
                GameEngine engine = new GameEngine(3, 11L);
                SoundManager sound = new SoundManager(false);
                GamePanel panel = new GamePanel(engine, board, sound, () -> { });
                panel.setSize(900, 560);
                java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(900, 560,
                        java.awt.image.BufferedImage.TYPE_INT_ARGB);

                boolean ok = true;
                try {
                    java.awt.Graphics2D g = image.createGraphics();
                    panel.paint(g); // MENU
                    engine.requestState(GameState.HOW_TO_PLAY);
                    panel.paint(g);
                    engine.requestState(GameState.MENU);
                    engine.requestState(GameState.LEADERBOARD);
                    panel.paint(g);
                    engine.requestState(GameState.MENU);
                    engine.startNewGame(0);
                    for (int i = 0; i < 120; i++) {
                        engine.update(0.016);
                        panel.paint(g); // PLAYING with spawned objects
                    }
                    engine.pause();
                    panel.paint(g); // PAUSED
                    engine.resume();
                    engine.getObjects().add(stillBomb(300, 300));
                    engine.beginStroke(300, 300);
                    engine.getObjects().add(still(FruitType.BANANA, 700, 300));
                    engine.extendStroke(700, 300);
                    panel.paint(g);
                    g.dispose();
                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                    ok = false;
                }
                check("menu, how-to-play, leaderboard, gameplay and pause render without exceptions", ok);
            } finally {
                deleteRecursively(dir);
            }
        } catch (IOException | java.awt.HeadlessException ex) {
            check("render smoke test could run in this environment (" + ex + ")", false);
        }
    }
}
