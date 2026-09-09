# Classic Word Arena - Technical Architecture Document

This document details the software architecture, state management patterns, core algorithms, and component design of **Classic Word Arena**.

---

## 🏛️ High-Level System Architecture

Classic Word Arena strictly follows modern Android Architecture guidelines based on **Unidirectional Data Flow (UDF)** and **Model-View-ViewModel (MVVM)** principles.

```
+-------------------------------------------------------------------------+
|                               UI LAYER                                  |
|  [ScrabbleScreen] <--- (Renders State) ---- [ScrabbleViewModel]        |
|  ├── BoardView                                   |                      |
|  ├── RackView                                    | (Dispatches Actions) |
|  ├── ScoreHeader                                 v                      |
|  └── Dialogs & Banners                      UiState                     |
+-------------------------------------------------------------------------+
                                                   |
                                                   v
+-------------------------------------------------------------------------+
|                               DOMAIN LAYER                              |
|  [ScrabbleEngine]         [AiEngine]            [WordDictionary]        |
|  - Board Validation       - Candidate Generator - Lexicon Storage       |
|  - Multiplier Scoring     - Difficulty Filters  - Trie Prefix Search    |
|  - Cross-word Engine      - Matching Matrix     - Definitions           |
+-------------------------------------------------------------------------+
                                                   |
                                                   v
+-------------------------------------------------------------------------+
|                             UTILITY LAYER                               |
|  [GameAudioSynthesizer]                         [Models.kt]             |
|  - AudioTrack PCM Wave Generation               - Tile, BoardCell, etc. |
+-------------------------------------------------------------------------+
```

---

## 💾 Domain Models (`com.example.game.model.Models`)

Core data structures are immutable data classes designed for safe state transformations:

- **`Tile`**: Represents an individual Scrabble tile with `id`, `letter`, `assignedLetter` (for blank tiles), `points`, and `isBlank`.
- **`BoardCell`**: Represents a single cell on the 15x15 grid, storing `row`, `col`, `bonusType` (2L, 3L, 2W, 3W, Center), committed `tile`, and `isHighlighted`.
- **`PlacedTile`**: Represents a tile temporarily placed during the current turn before submission.
- **`FormedWord`**: Captures a word formed on the board during a move along with calculated points, start position, and orientation (`isHorizontal`).
- **`TrayWordInfo`**: Captures information about contiguous valid dictionary words formed directly on the player's rack (`word`, `totalScore`, `startIndex`, `endIndex`).
- **`MoveScoreResult`**: Complete result of move evaluation including `isValid`, `errorMessage`, `primaryWord`, `crossWords`, `totalScore`, and `isBingo`.
- **`Difficulty`**: Enum defining AI bot personalities (`RELAXED`, `BALANCED`, `MASTER`, `CUSTOM`) with target max scores, vocabulary ratios, and aggressiveness factors.
- **`GameThemePalette`**: Contains color definitions for themes across board backgrounds, cell multipliers, tiles, and typography.

---

## ⚙️ Core Engines & Algorithms

### 1. Game Engine (`ScrabbleEngine.kt`)
`ScrabbleEngine` is a stateless singleton responsible for board rules and scoring logic:

- **Board Initialization**: Constructs a 15x15 grid mapped with official premium multiplier cells:
  - **3W (Triple Word)**: Corner cells and outer perimeter positions.
  - **2W (Double Word)**: Diagonal positions leading to center.
  - **3L (Triple Letter)**: Strategic inner cells.
  - **2L (Double Letter)**: Standard letter multiplier cells.
  - **★ Center**: Row 7, Col 7 (acts as 2W for the opening move).
- **Move Evaluation (`evaluateMove`)**:
  1. Checks first move constraint (must cover center cell `(7, 7)`).
  2. Validates linear alignment (all placed tiles must be in a single row or column without gaps).
  3. Verifies connection to existing board tiles (for turn > 1).
  4. Scans primary line to construct the primary word.
  5. Scans perpendicular lines for each placed tile to construct cross-words.
  6. Validates all newly formed words against `WordDictionary`.
  7. Calculates total score taking into account letter multipliers (applied only to newly placed tiles) and word multipliers (cumulative product across all new words).
  8. Adds a **50-point Bingo bonus** if 7 tiles were placed from the rack in a single turn.

### 2. AI Bot Engine (`AiEngine.kt`)
`AiEngine` generates valid moves for computer turns:

- **Candidate Move Generation**:
  - Finds all legal anchor cells on the board (cells adjacent to existing tiles or center cell).
  - Iterates through vocabulary candidates compatible with the bot's rack tiles and board anchors.
  - Evaluates legal placements horizontally and vertically.
- **Difficulty Filtering**:
  - **Relaxed**: Filters candidate moves targeting scores between 8 and 22 points.
  - **Balanced**: Filters moves targeting 20 to 38 points using a 75% vocabulary ratio.
  - **Master**: Evaluates all candidate moves and selects the maximum scoring valid move, prioritizing multipliers and bingos.
  - **Custom**: Applies user-configured vocabulary limits, max word lengths, and aggressiveness.

### 3. Word Dictionary (`WordDictionary.kt`)
- **Lexicon Engine**: Loads a compressed word list (`scrabble_words.txt.gz`) containing standard Scrabble words.
- **Prefix Matching**: Uses fast set lookups and prefix checks to accelerate AI search algorithms (`hasPrefix`).
- **Definitions**: Contains pre-built definitions for common words and seamlessly interfaces with external dictionary REST APIs when definitions are requested for obscure words.

---

## 🔄 State Management (`ScrabbleViewModel.kt`)

`ScrabbleViewModel` maintains `ScrabbleUiState`, exposed via Kotlin `StateFlow<ScrabbleUiState>`:

- **Pending Placements (`pendingPlacements`)**: Maps board coordinates `(row, col)` to placed `Tile` instances before user commits the move.
- **Real-Time Validation**: Automatically re-calculates `evaluateMove` on every tile drag or drop event to provide immediate UI feedback (validity indicator, projected points).
- **Turn Progression**: Handles player submission (`onPlayMove`), turn swapping, AI execution (`triggerAiTurn`), pass turns, and tile exchange (`onSwapTiles`).
- **Undo & Recall**: `onRecallAll` returns all pending board tiles back to the player's rack.

---

## 🎨 UI & Design System (`com.example.game.ui`)

- **`ScrabbleScreen`**: Primary container layout implementing responsive adaptivity for Portrait and Landscape modes.
- **`BoardView`**: Canvas and Compose grid rendering 225 board cells with touch targets, drag-and-drop drop targets, and move highlight overlays.
- **`RackView`**: Renders tile tray, drag handles for tile reordering, action buttons (Play, Recall, Shuffle, Swap, Hint), and status banners.
- **`ScoreHeader`**: Shows player and bot scores, active turn status, turn timer, theme selector toggle, and settings triggers.
- **`Dialogs`**: Modal overlays for theme customization, AI settings, move history, game stats, and debug logs.

---

## 🔊 Sound Engine (`GameAudioSynthesizer.kt`)

`GameAudioSynthesizer` generates sound effects programmatically:

- **Pure Wave Synthesis**: Calculates sine waves with linear fade-out envelopes in Kotlin without relying on raw audio asset files.
- **AudioTrack Stream**: Streams 16-bit PCM mono samples directly to `AudioTrack` on a background thread.
- **Sound Effects**:
  - *Normal Play*: Dual chime (587 Hz -> 880 Hz).
  - *High Score*: Major arpeggio (C5 -> E5 -> G5 -> C6).
  - *Mega Score / Bingo*: Sparkling 7-note ascending cascade (C5 up to C7).

---

## 🧪 Testing Infrastructure

- **Robolectric Tests** (`ExampleRobolectricTest.kt`): Runs on local JVM to verify dictionary loading, word prefix matches, and move evaluation correctness.
- **Roborazzi Screenshot Tests** (`GreetingScreenshotTest.kt`): Captures native pixel screenshots of Compose components to prevent UI regressions.
