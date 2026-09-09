# Classic Word Arena

An elegant, feature-rich Android word game built with Kotlin and Jetpack Compose. Classic Word Arena brings the classic 15x15 Scrabble experience to life with adaptive UI themes, intelligent AI opponents, interactive dictionary lookups, pure PCM audio synthesis, and fluid drag-and-drop tile mechanics.

---

## 🌟 Key Features

### 🔠 Authentic Board & Game Engine
- **Standard 15x15 Board**: Fully compliant with classic board layouts featuring Double Letter (2L), Triple Letter (3L), Double Word (2W), Triple Word (3W), and Center Start (★) multiplier squares.
- **Tile Bag Distribution**: Standard 100-tile letter bag with official letter values and blank tile assignments.
- **Valid Move Engine**: Accurate move validation supporting horizontal and vertical placements, connected words, cross-word scoring, and 50-point Bingo bonuses for 7-tile plays.

### 🤖 Intelligent AI Opponents
- **Relaxed ("Zen Sage")**: Friendly, casual word plays (8–22 points target).
- **Balanced ("Scholar Oliver")**: Moderate challenge playing standard tactical words (20–38 points target).
- **Master ("Grandmaster Nova")**: Aggressive high-scoring AI maximizing multipliers and searching for Bingos (up to 70+ points).
- **Custom AI**: Configurable vocabulary ratio (20%–100%), aggressiveness, and maximum word length.

### 🎨 Themes & Customization
- **8 Custom Color Palettes**:
  - *Light Themes*: Botanical Zen, Pastel Sunset, Nordic Slate, Warm Walnut
  - *Dark Themes*: Midnight Obsidian, Deep Forest Dark, Espresso Dark, Amethyst Night
- **System Dark/Light Mode Integration**: Seamlessly follows system preferences or manual theme selection.
- **Portrait & Landscape Support**: Fluid, responsive layout adapted for various mobile and tablet device orientations.

### 🕹️ Tile Placement Mechanics
- **Drag-and-Drop Rack & Board**: Intuitive touch dragging from tray to board and tile rearrangement within the rack.
- **Real-Time Tray Word Hinting**: Automatically detects valid dictionary words formed by contiguous tiles as you rearrange letters on your rack tray, displaying the word name and base point score in a glowing status pill and highlighting the matching tiles on the rack.
- **Snap Placement Mode**: One-tap word positioning with direction toggles for rapid tile placement.
- **Blank Tile Picker**: Interactive dialog to designate letters for blank tiles when placed on the board.

### 📚 Dictionary & Definitions
- **Embedded Lexicon**: Offline dictionary containing over 1,400 core English words and fast prefix lookup.
- **Word Inspection Overlay**: Tap any played tile on the board to view forming words, definitions, score breakdown, and board highlights.
- **Online Fallback**: Automatic fetching of word definitions via external dictionary APIs when offline definitions are not preloaded.

### 🏆 Game Modes & Utilities
- **Flexible Win Conditions**:
  - *Classic End Game*: Bag empty + rack cleared or consecutive passes.
  - *Target Score Race*: First player to hit a target score (e.g., 100 or 150 pts) wins immediately.
  - *Turn Limit Battle*: Set number of rounds per player.
- **Game Utilities**: Unlimited Undo before submission, Tile Swapping, Hints, and Opponent Rack Peeking.
- **Move History Log & Stats**: Detailed move-by-move history with board coordinate highlighting and personal game statistics tracking.

### 🎵 Pure PCM Sound Synthesizer
- Custom-built sound engine using Android `AudioTrack` producing PCM wave chimes for normal plays, high scores, and mega score celebrations without external audio assets.

---

## 🏗️ Architecture & Tech Stack

```
com.example
├── MainActivity.kt                  # Main entry Activity with Edge-to-Edge support
├── ui/theme/                        # Application Material 3 theme and colors
└── game/
    ├── dictionary/
    │   └── WordDictionary.kt        # Offline lexicon, prefix search, and definition provider
    ├── engine/
    │   ├── ScrabbleEngine.kt        # Board creation, move evaluation, and scoring rules
    │   └── AiEngine.kt              # AI bot move generator & candidate evaluation
    ├── model/
    │   └── Models.kt                # Immutable data structures (Tile, BoardCell, FormedWord, Palette, etc.)
    └── ui/
        ├── ScrabbleScreen.kt        # Root Jetpack Compose screen layout
        ├── ScrabbleViewModel.kt     # State flow manager & game orchestration (MVVM)
        └── components/
            ├── BoardView.kt         # Interactive 15x15 Compose board grid
            ├── RackView.kt          # Player rack tray, action bar, and drag-and-drop controller
            ├── TileView.kt          # Tile UI rendering component
            ├── ScoreHeader.kt       # Player/AI score, turn indicator, timer, and action buttons
            ├── CelebrationBanner.kt # High-score animations and bingo banners
            ├── Dialogs.kt           # Settings, History, Stats, Themes, Swaps, Debug overlays
            └── GameAudioSynthesizer.kt # PCM wave chimes engine
```

- **Language**: Kotlin 2.1+
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Unidirectional Data Flow (`StateFlow`, `viewModelScope`)
- **Testing**: Robolectric (JVM host tests) & Roborazzi (Compose screenshot verification)
- **Min SDK**: 24 (Android 7.0) | **Target SDK**: 36 (Android 15 / 16)

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Ladybug / Meerkat or later
- **JDK**: Version 11 or higher
- **Android SDK**: API Level 36 installed

### Building & Running

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/Scrabble-.git
   cd Scrabble-
   ```

2. **Build Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Run Unit Tests**:
   ```bash
   ./gradlew :app:testDebugUnitTest
   ```

4. **Install on Connected Device / Emulator**:
   ```bash
   ./gradlew :app:installDebug
   ```

---

## 📦 APK Generation & Artifacts

Per project conventions, building the project automatically outputs the debug APK artifacts at the root directory:
- `classic_word_arena.apk`
- `app-debug.apk`

---

## 🧪 Testing Strategy

- **Unit Tests**: `ExampleRobolectricTest.kt` verifies dictionary initialization, move evaluation logic, and string resource access via Robolectric.
- **Screenshot Tests**: `GreetingScreenshotTest.kt` uses Roborazzi to render and capture baseline UI components under `app/src/test/screenshots/`.

---

## 📄 Documentation

For an in-depth deep dive into the internal design, game engine state machines, AI candidate matching algorithms, and UI flow, see [ARCHITECTURE.md](docs/ARCHITECTURE.md).

---

## 📜 License

This project is open source and available under the MIT License.
