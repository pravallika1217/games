# 🕵️ ALIBI

**One mystery a day, and you're the detective.** A story-driven daily puzzle game for Android.

1. 🪪 **Your ID card.** Pick your name once; everyone in the game calls you "Inspector ___".
2. 📞 **The midnight call.** The Commissioner phones with today's case, typed out letter by letter.
3. 🔦 **Search the crime scene.** The room is dark. Drag your finger to move the torch. When a clue
   sparkles ✨ in the light, tap it to pick it up and put it in your evidence bag.
4. 📌 **The evidence wall.** Witnesses come one at a time and say exactly what to find, e.g.
   *"Find the 4 SWEETS on the wall."* Tap the right notes to pin them with red string; wrong notes
   cost Pandu's chai ☕. Then the witness gives their statement. Constable Pandu gives hints.
5. 💡 **The interrogation room.** Call in each suspect, show them statements, and watch them react.
   Innocent people relax 😌. The liar starts sweating 💦. Your notebook fills itself in.
6. 🚔 **The arrest.** Handcuffs, a CASE CLOSED stamp, and the culprit's confession with a twist.
7. 📰 **Tomorrow's newspaper.** A front page about you, with your rating and a share button.

Solving cases promotes you: Rookie → Sub-Inspector → Inspector → ACP → Commissioner.

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
  board/    The evidence wall (immutable state)
  cases/    Case content, the interrogation room, the daily case library
  score/    Scoring, career ranks, the newspaper and share text
  logic/    Logic-grid puzzle generator, kept for a future "Detective mode"
app/      Android app, Jetpack Compose UI
  GameViewModel.kt   All game state, one screen at a time
  ProgressStore.kt   Name, career, streak and today's newspaper (SharedPreferences)
  Sfx.kt             Built-in tones and vibration
  ui/                Id/Desk, Call, Scene, Wall, Room, Arrest and News screens
```

## Scoring

Start at 100. Each wrong note costs 5, each missed statement 10, each hint 5, and each wrong
arrest 25. If the culprit escapes, the score can't go above 30.
90+ is ★★★ Sherlock, 70+ ★★ Sharp Inspector, 45+ ★ Constable on Duty.

## Writing a new case

Add a `CaseFile` to `CaseLibrary.cases`:

- a title, the Commissioner's call (`{name}` becomes the detective's name) and a one-line crime,
- a crime scene: 3 pieces of evidence (emoji, position, what it means) and some background objects,
- 3 word groups for the wall, each with its witness: what they ask you to find (give an example that
  isn't on the wall) and the statement they give. Plus 4 trick notes that fit no witness,
- 3 suspects with an alibi and a few small-talk lines; vary which chair the culprit sits in,
- reactions: one statement that makes each innocent relax, and one that makes the culprit sweat,
- what the Commissioner says when an innocent is arrested,
- the confession, what happens if they escape, and the detective's quote for the newspaper.

The tests check that only the culprit ever gets nervous and that every innocent can be cleared.

## Roadmap

- [ ] Daily "📞 New case" notification at midnight (WorkManager)
- [ ] Load cases from a server instead of shipping them in the app
- [ ] Office that grows with your rank, and a cabinet of solved case files
- [ ] Rain and noir music
- [ ] Friend Case: make a mystery starring your friends and send it as a link
