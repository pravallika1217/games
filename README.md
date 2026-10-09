# 🕵️ ALIBI

**One mystery a day, and you're the detective.** A story-driven daily puzzle game for Android.

1. 📞 **The call.** The Commissioner rings with today's case.
2. 🚧 **Arrival.** Police tape, and Constable Pandu's 4-line briefing. The cause is always "unknown".
3. 🔍 **Examine.** Tap 3 marked spots on the body (or the broken cupboard, the cut bag…),
   then answer one question: how did it happen?
4. 👥 **Who was there?** 8 people, each card showing the same few facts (Family / Staff,
   glasses, how they came…). The culprit is one of them.
5. 🔎 **Follow the clues.** Each lead starts from something you already found and asks you to
   think: *"The wrapper is round, with yellow crumbs. Which sweet was in it?"* A wrong answer is
   a mistake. The right one reveals the clue, and the clue rules people out by one fact on their
   cards. Cross them out (or mark them) and Check. 3 leads leave 3 suspects.
6. 💡 **Questioning.** Show your evidence to each suspect. Innocent people explain; the liar gets
   caught with a big **GOTCHA!**
7. 🚔 **Arrest.** Pick who did it. Two warrants: one wrong arrest is forgiven.
8. ⭐ **Result.** Stars, time, mistakes, streak, a spoiler-free share text, and a countdown to
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
content/cases/   The cases, as JSON. This folder is what gets published on the web,
                 and it's also shipped inside the app for offline play.
  schedule.json    Which case plays on which day
  <id>.json        One file per case
engine/   Plain Kotlin game rules, no Android code. Fully unit-tested.
  cases/    The case format and its fairness checks (Case.kt), reading JSON (CaseJson.kt),
            and the rules for leads, questioning and arresting (Investigation.kt)
  score/    Stars, share text and career ranks
  logic/    Logic-grid puzzle generator, kept for a future "Detective mode"
app/      Android app, Jetpack Compose UI
  GameViewModel.kt   All game state, one screen at a time
  CaseRepository.kt  Loads today's case: downloaded, or shipped in the app
  ProgressStore.kt   Name, career, streak and today's result (SharedPreferences)
  Sfx.kt             Built-in tones and vibration
  ui/                StartScreens (name, home, call), CrimeSceneScreens (arrival, examine),
                     InvestigationScreens (people, clues), EndScreens (questioning, vote,
                     reveal, result)
```

## Where cases come from

Like Wordle and Connections, every case is a small data file and the app fetches today's one.

1. `content/cases/schedule.json` lists case ids in play order, starting on `launchDay`.
   Case #1 plays on launch day, #2 the next day, and so on. When the list runs out it starts
   again from the top. **Only ever add to the end of the list**, so past days keep their case.
2. On start, the app shows the case it already has, then downloads the newest `schedule.json`
   plus today's and tomorrow's case files from `CASES_URL` (in `app/build.gradle.kts`), checks
   them with the engine, and saves them on the phone. No internet? It uses what it saved last
   time, or the cases shipped inside the app.
3. `CASES_URL` points at this repo's `content/cases` folder on GitHub. That only works while
   the repo is public. Otherwise host the folder anywhere that serves static files (GitHub
   Pages, Firebase Hosting, any web server) and change the URL.

**To publish a new case:** add `<id>.json` to `content/cases`, add the id to the end of
`schedule.json`, run `./gradlew :engine:test`, and push. Players get it without an app update.

## Scoring

Three stars to start. Lose one for 3 or more mistakes (wrong answers in the examination and
the leads, and wrong Checks), and one for a wrong arrest. If the culprit escapes, no stars.

## Writing a new case

Copy `content/cases/sangeet.json` and change the story. `./gradlew :engine:test` checks every
case file, and the app checks every download the same way, so an unfair case never reaches
players. The checks:

- **Examination:** 3 spots and one "how did it happen?" question.
- **People:** 5 to 10 (8 is best). Each card shows a `group` (Family, Staff, Guest…) and the same
  kinds of facts in the same order, like glasses and how they came.
- **Leads:** 1 to 4 (3 is best). Each one has:
  - `from`: what you already found that starts this lead.
  - `question`, `options`, `answer`, plus `wrongWhy`: why each wrong option is wrong.
  - `reveal`, then the clue: `emoji`, `name`, `found` (key words in `**bold**`).
  - `task` and `fact`: `"cross"` crosses out everyone whose card does NOT say `fact`;
    `"mark"` marks everyone whose card does. `fact` must match a card word for word.
  - `ask`, `why`, and a short `card` label for questioning.
- After all the leads, the people left must be exactly the ones with an `interview` (2 to 4).
  The culprit must survive every lead and be marked by every "mark" lead.
- **Interviews:** where they say they were, an alibi, and a reply for every lead. Only the
  culprit has `gotchas` (the lines that catch them lying); innocents need a `release` line.
- A `confession`, and an `escapeStory` for when the culprit gets away.

## Roadmap

- [ ] First-time tutorial hand on the search and questioning screens
- [ ] Real illustrated art instead of emoji
- [ ] Daily "📞 New case" notification at midnight (WorkManager)
- [ ] Friend Case: make a mystery starring your friends and send it as a link
