# Fruit Slash: Arcade Challenge

A 2D fruit-slicing arcade game built with **Java Swing and Java2D** for the KDU IT22013 Data Structures and Algorithms group assignment.

Slice fruits, avoid bombs, build combos, and complete all 8 levels while exploring arrays, linked lists, stacks, queues, sorting, searching, and recursion.

## Requirements
- JDK 17 or newer

## Build and Run

**Windows**
```bat
build.bat
run.bat
```

**Linux / macOS**
```sh
./build.sh
./run.sh
```

Run automated tests with `test.bat` on Windows or `./test.sh` on Linux/macOS.

## Controls
- **Slice:** Click or drag with the left mouse button
- **Pause / Resume:** `P` or `Esc`
- **Undo:** `U` or `Ctrl+Z`
- **Restart:** `R`
- **Mute / Unmute:** `M`

## DSA Concepts
- **Array:** Active fruits and bombs
- **Singly linked list:** Blade trail
- **Stack:** Undo history
- **Queue:** Upcoming spawns
- **Bubble Sort:** Leaderboard ranking
- **Linear Search:** Player lookup
- **Recursion:** Level progression

## Project Structure
- `src/` — Java game source
- `test/` — Automated test harness
- `docs/` — Project report and viva notes
- `build` / `run` / `test` scripts — Build, launch, and test the game

Scores are saved in `data/leaderboard.txt`.
