# Fruit Slash: Arcade Challenge

A 2D fruit-slicing arcade game written in **Java (Swing + Java2D)** for the KDU
**IT22013 Data Structures and Algorithms** group assignment (BSc (Hons) IT / IS, Intake 42).

Slice flying fruit with the mouse, dodge the bombs, build combos, survive eight levels.
Behind the scenes the game is powered by hand-written data structures and algorithms
(array, singly linked list, stack, queue, Bubble Sort, Linear Search, recursion) -
the player never has to answer a question about them.

* No external libraries, no JavaFX, no image or audio files, no internet. Everything (fruit, bombs,
  particles, buttons, sound effects) is drawn or synthesised in code.
* Requires **JDK 17 or newer** (a JDK, not just a JRE - you need `javac`).

---

## 1. Build and run (Windows)

Open **Command Prompt** (or PowerShell) in the project folder - the folder that contains `src`, `test`
and this README.

### Quick way - scripts

```bat
build.bat      :: compile everything into the "out" folder
run.bat        :: build if needed, then start the game
test.bat       :: build, then run the automated test harness
```

### Manual commands (no scripts needed)

```bat
mkdir out
javac -d out src\*.java test\*.java
java -cp out Main
```

Run the automated tests:

```bat
java -cp out TestRunner
```

The test runner prints `PASS` / `FAIL` for each check and ends with
`RESULT: ALL TESTS PASSED` (exit code 0) or `RESULT: SOME TESTS FAILED` (exit code 1).

> **If `javac` is "not recognized":** the JDK is not on your PATH. Install a JDK 17+ (for example Eclipse
> Temurin) and tick "add to PATH", or call it with the full path, e.g.
> `"C:\Program Files\Eclipse Adoptium\jdk-17...\bin\javac" -d out src\*.java test\*.java`.
>
> **Fallback if the wildcard form fails** (rare, folder names with unusual characters):
> `dir /s /b src\*.java test\*.java > sources.txt` then `javac -d out @sources.txt`.

### Linux / macOS

```sh
./build.sh && ./run.sh        # or: ./test.sh
```

(equivalent to `javac -d out src/*.java test/*.java` and `java -cp out Main`).

### Where scores are saved

`data/leaderboard.txt`, created next to where you start the game (the folder you ran the command from).
If that folder is not writable the game falls back to a hidden `.fruit-slash` folder in your home
directory. It is a plain text file (`name<TAB>score` per line) - delete it to reset the leaderboard.
A missing, empty or damaged file never crashes the game.

---

## 2. How to play

| Action | Control |
|---|---|
| Slice fruit | Click, or hold the left mouse button and drag across the fruit |
| Pause / resume | `P` or `Esc` (or the PAUSE button). The game also pauses if the window loses focus |
| Undo your last slice | `U`, `Ctrl+Z`, or the UNDO button |
| Restart | `R` (from the pause screen, or the game-over screen once your name is saved) |
| Mute / unmute | `M` (or the SOUND button on the menu) |
| Back / main menu | `Esc` on the how-to-play, leaderboard and game-over screens |
| Leaderboard paging | `Left` / `Right` arrows or the PREV / NEXT buttons |

**Rules**

* Missing a fruit (letting it fall) costs **1 life** (you start with 3).
* Slicing a **bomb** costs **1 life** and 25 points (never below 0) and resets your combo. Ignoring a bomb is safe.
* Slice fruit quickly one after another (within 1.1 s) to build a **combo** worth +2 points per step.
* Slice the level's fruit target to clear it (10 fruits on level 1, +3 per level). Clear all **8 levels** to win.
* **Undo Slice** (3 charges, +1 for every level cleared, maximum 5): takes back the points and puts the
  fruit back - or gives back the life if you sliced a bomb. Works only for 3.5 s after the slice.

| Fruit | Points | Fruit | Points |
|---|---|---|---|
| Apple | 10 | Strawberry | 15 |
| Orange | 10 | Banana | 15 |
| Lemon | 10 | Grapes | 15 |
| Watermelon | 20 | | |

Open **HOW TO PLAY -> TECHNICAL INFO** in the game for a short, lecturer-oriented explanation of how each
data structure is used.

---

## 3. Project layout

```
FruitSlash/
  src/        game source code (31 files, default package)
  test/       TestRunner.java  (self-contained test harness, no JUnit needed)
  docs/       REPORT.md (university report draft), VIVA.md (viva preparation)
  build.bat  run.bat  test.bat   (Windows)      build.sh  run.sh  test.sh   (Linux/macOS)
```

All classes are in the default package, so the commands above match the files exactly.

### Where each required concept lives

| Requirement | Class | Used in the game for |
|---|---|---|
| Array | `ObjectArray` | All active fruits and bombs: moved, hit-tested, compacted every frame |
| Custom singly linked list | `SinglyLinkedList` (used by `SliceTrail`) | The glowing blade trail (recent mouse points) |
| Custom stack | `ArrayStack<SliceEvent>` | History of slices for the **Undo Slice** power-up |
| Custom queue | `CircularQueue<SpawnRequest>` | Upcoming fruit/bomb launches |
| Bubble Sort | `LeaderboardManager.bubbleSortScoresDescending` | Ranking the leaderboard (stable) |
| Linear Search | `LeaderboardManager.linearSearchPlayer` | Searching a player name on the leaderboard |
| Recursion | `LevelManager.advanceLevelRecursively` | Building each level's difficulty on every level start / level-up |

| Class | Responsibility |
|---|---|
| `Main`, `GameFrame` | Entry point, window, clean shutdown |
| `GamePanel` | The custom `JPanel`: `paintComponent`, Swing `Timer` loop, mouse/keyboard input, screen buttons |
| `GameRenderer`, `FruitPainter` | All drawing with Java2D (no state changes) |
| `GameEngine` | The game model: state machine, update loop, slicing, combo, undo, spawning, levels |
| `GameState`, `GameConfig` | The legal-transition table and every tunable constant |
| `GameObject`, `Fruit`, `Bomb`, `FruitType` | Flying objects, physics, hitboxes |
| `Particle`, `FruitHalf`, `FloatingText` | Visual effects (bounded lists) |
| `SpawnRequest`, `SliceEvent`, `LevelConfig`, `LevelManager` | Data carried by the queue / stack, level difficulty |
| `ScoreEntry`, `LeaderboardManager` | Validation, file persistence, Bubble Sort, Linear Search |
| `UiButton`, `TextInput`, `UiState`, `SoundManager`, `SoundEffect` | Buttons, text boxes, UI state, synthesised sound |

### Tuning the game

Every gameplay number is a constant in `GameConfig.java` (for example `STARTING_LIVES`, `MAX_LEVEL`,
`UNDO_WINDOW`, `BOMB_SCORE_PENALTY`). Level difficulty is defined by `LevelConfig.first()` and
`LevelConfig.nextHarder()`.

---

## 4. Verification status (what has and has not been checked)

This section is deliberately specific so nothing is overstated.

**Executed during development (OpenJDK 21.0.10, Ubuntu Linux):**

* The whole project compiles with `javac -Xlint:all` with **no warnings**, and also with `--release 17`
  (so it only uses the Java 17 API).
* The test harness (`java -cp out TestRunner`): **290 checks, 290 passed, 0 failed**
  (22 groups covering all ten required test areas - see `docs/REPORT.md` section 10).
* The real application window was driven under a virtual display (Xvfb) with `java.awt.Robot`
  (real mouse and keyboard events): menu navigation, pause/resume, slicing fruit with real mouse drags,
  level completion, the `U` undo key, game over, typing a name, saving, leaderboard search, and restart.
  After correcting one wrong expectation inside the throw-away driver script itself (it expected an upper-case
  name although the simulated key presses are lower-case), the final run on the finished code passed every step
  (0 failures). The driver is a development tool and is not part of the submission.
* Screens were rendered to images at several window sizes (800x500, 1920x1080, 2200x700, 900x900) to check
  scaling and letter-boxing.

**NOT checked (please do these yourselves):**

* Running on **Windows** (the development machine was Linux). The code uses only portable Java and relative paths,
  but your group should run `test.bat` and play the game once on a Windows machine before submitting.
* **Sound output.** The sound effects are synthesised in code, but the development environment had no audio device,
  so only the "silent fallback" path was exercised. If your machine has no audio device the game stays silent.
* High-DPI / display-scaling behaviour and multi-monitor setups.
* A long play-through of all 8 levels by a human (the automated tests do reach `VICTORY`, but difficulty balance
  has not been playtested by a person).

---

## 5. Manual GUI test checklist

Run `run.bat` and tick each item. (Leave a box empty if you did not run it.)

| # | Feature | Steps | Expected result | Pass? |
|---|---|---|---|---|
| 1 | Mouse slicing | Press PLAY. Drag across a flying fruit | Fruit splits into two halves, juice, `+points`, blade trail | |
| 2 | Click slicing | Click directly on a fruit | Fruit is sliced | |
| 3 | No double slice | Drag back and forth over a sliced fruit's spot | Score counted once | |
| 4 | Bomb | Slice a bomb | Flash, shake, "BOMB!", one heart lost, combo reset | |
| 5 | Miss | Let a fruit fall | "MISS!" and one heart lost | |
| 6 | Combo | Slice several fruits within about a second | "COMBO xN" shown, bonus points added | |
| 7 | Pause / resume | Press `P`, wait, press `P` again | Everything freezes while paused; continues after | |
| 8 | Pause menu | Press `Esc`, click RESTART / MAIN MENU / RESUME | Each button works | |
| 9 | Level progression | Slice 10 fruits on level 1 | "LEVEL 1 COMPLETE" overlay, then level 2 (target 13), +1 undo charge | |
| 10 | Undo (fruit) | Slice a fruit, press `U` quickly | Score drops by that slice, fruit reappears and flickers (immune for a moment) | |
| 11 | Undo (bomb) | Slice a bomb, press `U` | Life and 25 points returned, bomb reappears | |
| 12 | Undo limits | Press `U` with no recent slice / after using all charges / more than 3.5 s later | Message such as "Nothing to undo" or "No undo charges left"; nothing changes | |
| 13 | Game over | Lose all lives | Game-over screen: final score, level, fruits sliced, best score, top 5 | |
| 14 | Save score | Type a name, press Enter or SAVE | "Saved! You are ranked #N" | |
| 15 | Leaderboard | Click LEADERBOARD | Rank, name, score, sorted high to low, paging works | |
| 16 | Search | Type a name (any capitalisation) and press Enter / SEARCH | Row highlighted and shown; unknown name gives "No player named ... found" | |
| 17 | Persistence | Close the game, start it again | Scores still there; menu shows HIGH SCORE | |
| 18 | Restart | On game over, after saving, press `R` or PLAY AGAIN | New game starts with 3 lives, score 0, level 1 | |
| 19 | Sound toggle | Press `M` or the SOUND button | Label switches ON/OFF | |
| 20 | Resize | Drag the window larger and smaller (down to 800x500) | Game scales, stays centred, slicing still hits where you point | |
| 21 | Exit | Click EXIT or close the window | Application ends | |
| 22 | Damaged save file | Edit `data/leaderboard.txt`, add junk lines, start the game | Game starts; junk lines are ignored | |

---

## 6. Troubleshooting

| Problem | Fix |
|---|---|
| `'javac' is not recognized` | Install a JDK (not JRE) 17+ and add it to PATH |
| `Error: Could not find or load main class Main` | Run `java -cp out Main` from the project folder, after `build.bat` |
| Window too small / blurry on a scaled display | Resize the window; the game scales to any size >= 800x500 |
| No sound | The machine may have no audio device; the game is fully playable without sound |
| Want to reset scores | Delete `data/leaderboard.txt` |
#   F r u i t S l a s h  
 