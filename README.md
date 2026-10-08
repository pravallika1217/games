# 🕵️ ALIBI

**One mystery a day. Can you catch the liar?** A simple, story-driven daily puzzle game for Android.

1. 🧩 **Find the clues.** 12 words, 3 hidden groups (like *Connections*). Each group you find unlocks a clue.
2. 🔎 **Who did it?** Three suspects each give you their alibi. One of them is lying. Tap faces to stamp
   them INNOCENT, then accuse the one who's left. You get two tries.
3. 💬 **The confession.** The culprit tells you *why*, with a twist.
4. 📏 **Bonus (optional).** Slide to a smart guess on a number question (a Fermi question).

You finish with a detective rank (🥇 Sherlock, 🥈 Inspector, 🥉 Constable, 🫠 Suspect), a daily
streak, and a spoiler-free share card:

```
ALIBI #1 🥇 Sherlock (96)
🟨🟨🟨🟨
🟩🟩🟦🟩
🟩🟩🟩🟩
🟦🟦🟦🟦
🕵️ Caught on the 1st try
📏 Bonus 🔥
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
  board/    Step 1 word board (immutable state)
  cases/    Case model, catch-the-culprit state, the daily case library
  fermi/    Bonus question scoring
  score/    Points, ranks, share card
  logic/    Logic-grid puzzle generator, kept for a future "Detective mode"
app/      Android app, Jetpack Compose UI
  GameViewModel.kt   All screen state
  ProgressStore.kt   Today's result + streak (SharedPreferences)
  ui/                Home, Words, Suspects, Bonus and Result screens
```

## Scoring

| Part | Points |
|---|---|
| 🧩 Clues found | up to 40 (−3 per wrong guess) |
| 🔎 Culprit caught | 50 on the 1st try, 25 on the 2nd |
| 📏 Bonus guess | up to 10 |

## Writing a new case

Add a `CaseFile` to `CaseLibrary.cases`:

- a title, an intro, and one line about what happened,
- 3 suspects, each with an emoji and an alibi in their own voice,
- which suspect did it (vary the position!),
- 3 word groups that match the story, and the clue each one unlocks.
  Good clues clear two innocent suspects and catch the liar, so even 2 clues are enough to solve it,
- the culprit's confession, ideally with a twist,
- a bonus number question with its answer, reasoning and slider range.

## Roadmap

- [ ] Daily 8 AM "new case is in 🔍" notification (WorkManager)
- [ ] Load cases from a server instead of shipping them in the app
- [ ] Detective mode: 4 suspects, weapons and rooms, with a logic grid (generator already in `engine/logic`)
- [ ] Friend Case: make a mystery starring your friends and send it as a link
- [ ] Weekly story arc that builds to a Sunday reveal
