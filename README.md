# 🕵️ ALIBI

**One mystery a day, and you're the detective.** A story-driven daily puzzle game for Android.

1. 📞 **The call.** The Commissioner rings with today's case.
2. 🚧 **Arrival.** Police tape, and Constable Pandu's 4-line briefing. The cause is always "unknown".
3. 🔍 **Examine.** Tap 3 marked spots on the body (or the broken cupboard, the cut bag…),
   then answer one question: how did it happen?
4. 🗑️ **Search.** A forensics note gives a riddle about *where* to look, never *what* you'll find.
   Tap places in the room to search them. When you find the clue, decide **who it points to**,
   using the facts on each suspect's card. Right answers tie red string 🧶 to that suspect.
5. 💡 **Questioning.** Show your evidence to each suspect. Innocent people explain; the liar gets
   caught with a big **GOTCHA!**
6. 🚔 **Arrest.** Pick who did it. Two warrants: one wrong arrest is forgiven.
7. ⭐ **Result.** Stars, time, mistakes, streak, a spoiler-free share text, and a countdown to
   tomorrow's case.

Every case screen has the same shape: a 5-part progress bar on top, the scene in the middle,
one big button at the bottom.

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
  cases/    Case content (Case.kt, CaseLibrary.kt) and the rules for searching,
            questioning and arresting (Investigation.kt)
  score/    Stars, share text and career ranks
  logic/    Logic-grid puzzle generator, kept for a future "Detective mode"
app/      Android app, Jetpack Compose UI
  GameViewModel.kt   All game state, one screen at a time
  ProgressStore.kt   Name, career, streak and today's result (SharedPreferences)
  Sfx.kt             Built-in tones and vibration
  ui/                StartScreens (name, home, call), CrimeSceneScreens (arrival, examine,
                     search), EndScreens (questioning, vote, reveal, result)
```

## Scoring

Three stars to start. Lose one for 4 or more mistakes (wrong answers and wrong searches; a hint
counts as 2), and one for a wrong arrest. If the culprit escapes, no stars.

## Writing a new case

Add a `CaseFile` to `CaseLibrary.cases`. The tests check the rules below.

- **Briefing:** 4 lines, the last one "unknown".
- **Examination:** 3 spots and one "how did it happen?" question.
- **Room:** about 11 places to search, each with a funny line for when nothing is there.
- **3 clues:** a riddle about where it is, what you find (mark the key words with `**bold**`),
  and which suspect it points to. Make the first clue point to an innocent who can explain it.
- **3 suspects:** each with 2 visible facts the clues can match, where they say they were, an
  alibi, and a reply to every clue. Only the culprit has "gotchas". Vary which chair they sit in.
- A release line for each innocent, the confession, and what happens if the culprit escapes.

## Roadmap

- [ ] First-time tutorial hand on the search and questioning screens
- [ ] Real illustrated art instead of emoji
- [ ] Daily "📞 New case" notification at midnight (WorkManager)
- [ ] Load cases from a server instead of shipping them in the app
- [ ] Friend Case: make a mystery starring your friends and send it as a link
