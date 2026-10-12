# Publishing ALIBI: Daily Detective on Google Play

Everything the Play Console asks for, in order. Files mentioned here are in this `store/` folder.

## 0. Before you start

- [ ] The app runs on your phone, and you've played all the cases with **Next day ▶**.
- [x] Your email is filled in at the bottom of `PRIVACY.md`, and pushed to `main`.
- [ ] `launchDay` in `content/cases/schedule.json` is set to your real launch date.

## 1. Create the developer account (one time, about $25)

1. Go to https://play.google.com/console and sign in with the Google account you want to own the app.
2. Choose **Personal** (just you) account type, pay the fee, and verify your identity and phone
   number. Verification can take a few days.

## 2. Create the app

**Create app** → App name `ALIBI: Daily Detective` → Language English (India) → **Game** →
**Free** → accept the declarations.

## 3. Build the upload file (in Android Studio)

1. **Build → Generate Signed App Bundle or APK → Android App Bundle → Next.**
2. **Create new…** key store. Save it somewhere safe, with a strong password.
   ⚠️ **Back up this .jks file and its passwords (Google Drive + a USB stick).
   Without it you can never update the app.**
3. Choose **release**, then **Create**. The file is `app/release/app-release.aab`.
4. Every later upload: add 1 to `versionCode` in `app/build.gradle.kts` first.

## 4. Store listing (Grow → Store presence → Main store listing)

| Field | What to use |
|---|---|
| App name, short and full description | Copy from `listing.md` |
| App icon (512×512) | `icon-512.png` |
| Feature graphic (1024×500) | `feature-graphic.png` |
| Phone screenshots (at least 2, best 4–6) | Take them on your phone: call screen, examine, clues, GOTCHA, result |
| Category | Game → **Puzzle** |
| Contact email | alibi.dailydetective@gmail.com |
| Privacy policy | `https://github.com/pravallika1217/games/blob/main/PRIVACY.md` |

## 5. App content (Policy → App content): answers

- **Privacy policy:** the link above.
- **Ads:** No, the app has no ads.
- **App access:** All functionality is available without special access.
- **Target audience:** 13–15, 16–17 and 18+. **Don't pick under 13**: it's a murder mystery,
  and children's apps have much stricter rules.
- **Content rating questionnaire:** category Game. Violence: references to crime and murder,
  no blood or gore shown. No sexual content, no swearing, no gambling, no user chat.
  Expect a Teen / 12+ rating.
- **Data safety:** Does the app collect or share user data? **No.** (Everything stays on the
  phone, and the case downloads send no personal data.)
- **News app:** No. **Government app:** No. **Financial features:** None. **Health:** No.

## 6. Closed test: required for new personal accounts

Google won't let a new personal account publish to everyone until **at least 12 testers** have
had the app installed for **14 days in a row**.

1. **Testing → Closed testing → Create track.** Upload the `.aab`.
2. Add testers by email (a Google Group or a list of up to 100 emails). Ask 15–20 friends,
   so you still have 12 if some drop out.
3. Send them the opt-in link. They must **keep the app installed for 14 days**.
4. Use these two weeks to fix what they report, and to write more cases.

## 7. Go live

After 14 days: **Dashboard → Apply for production access**. Answer the questions about your
test. Google reviews it (a few days), then **Production → Create release** → upload the newest
`.aab` → **Start rollout**. 🎉

## After launch

- New cases: add the file, add the id to the end of `schedule.json`, run `./gradlew :engine:test`,
  push to `main`. No app update needed.
- App changes: bump `versionCode`, build a new signed bundle, upload a new release.
