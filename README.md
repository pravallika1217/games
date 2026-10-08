# 🕵️ ALIBI

**One mystery a day, solved in three acts.** A daily puzzle game for Android that combines
*Connections*, *Murdle* and Fermi estimation questions.

| Act | Like | What you do |
|---|---|---|
| 1. Evidence Board | Connections | Sort 16 words into 4 groups. Each group unlocks a clue. |
| 2. The Deduction | Murdle | Use the clues to find **who**, **with what**, and **where**. |
| 3. The Final Call | Fermi | Make a smart number guess. The closer you are, the more points you get. |

You finish with a detective rank (🥇 Sherlock, 🥈 Inspector, 🥉 Constable, 🫠 Suspect), a daily
streak, and a spoiler-free share card:

```
ALIBI #1 🥈 Inspector (78)
🟨🟨🟨🟨
🟦🟦🟦🟪
🟩🟩🟩🟩
🔎 Who ✅ What ✅ Where ❌
📏 Fermi 🔥
```

## Run it

1. Open this folder in **Android Studio** (Ladybug or newer).
2. Let Gradle sync, then press **Run** on a phone or emulator (Android 7.0+).

Run the game-logic tests (no Android SDK needed):

```
./gradlew :engine:test
```

## Project layout

```
engine/   Plain Kotlin game rules, no Android code. Fully unit-tested.
  logic/    Puzzle generator, human-style solver, brute-force uniqueness check, grid notes
  board/    Act 1 evidence board (immutable state)
  fermi/    Act 3 scoring
  cases/    Case model, clue sentences, the daily case library
  score/    Points, ranks, share card
app/      Android app, Jetpack Compose UI
  GameViewModel.kt   All screen state
  ProgressStore.kt   Today's result + streak (SharedPreferences)
  ui/                Home, Board, Deduction, Fermi and Result screens
```

## How the daily logic puzzle is made

Nobody writes logic grids by hand. For each case, `PuzzleGenerator`:

1. picks a random hidden solution,
2. adds true clues (mostly "not" and "either/or" clues) until a solver that only uses
   human-style deductions can finish the grid,
3. removes every clue that isn't needed.

The tests check hundreds of seeds and confirm every puzzle has **exactly one answer**, can be
**solved without guessing**, and that **every clue is needed**. The bonus clues from Act 1 never
name the culprit, so missing them makes Act 2 harder but never impossible.

## Adding a new case

Add a `CaseFile` to `CaseLibrary.cases`. You write only the creative parts:

- the title and story intro,
- 4 suspects, 4 weapons and 4 places (with emoji),
- 16 board words in 4 groups (level 0 = easiest, 3 = trickiest; add red herrings),
- one Fermi question with its answer and reasoning.

The logic puzzle is generated from `logicSeed`. When the library runs out, cases repeat with a
new seed, so the deduction is new each time.

## Roadmap

- [ ] Daily 8 AM "new case is in 🔍" notification (WorkManager)
- [ ] Load cases from a server (Firestore / JSON on a CDN) instead of shipping them in the app
- [ ] Save progress mid-case
- [ ] Weekly story arc: Monday to Saturday cases build to a Sunday reveal
- [ ] Friend Case: make a case starring your friends and send it as a link
- [ ] Duel: race a friend on the same case
- [ ] Detective Pass: case archive, hints, no ads
