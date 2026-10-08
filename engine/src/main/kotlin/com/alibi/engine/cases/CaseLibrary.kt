package com.alibi.engine.cases

import com.alibi.engine.board.EvidenceGroup
import com.alibi.engine.fermi.FermiQuestion
import java.time.LocalDate

/** The daily case schedule. Case #1 is on [LAUNCH_DAY], then one new case per day. */
object CaseLibrary {
    val LAUNCH_DAY: Long = LocalDate.of(2026, 10, 8).toEpochDay()

    fun caseNumber(epochDay: Long): Int = (epochDay - LAUNCH_DAY + 1).coerceAtLeast(1).toInt()

    fun forDay(epochDay: Long): PlayableCase {
        val number = caseNumber(epochDay)
        return PlayableCase(number, cases[Math.floorMod(number - 1, cases.size)])
    }

    val cases: List<CaseFile> = listOf(
        CaseFile(
            title = "Death at the Sangeet",
            intro = "Music, dancing, 400 guests… and at midnight, rich Uncle Raj is found dead. 😱 " +
                "Three people had a reason. Only one is lying about where they were.",
            crime = "Uncle Raj was found dead in the kitchen at midnight, next to a half-eaten laddu. 🟠",
            suspects = listOf(
                Suspect("Aunty Kamala", "👵", "I was in the mehndi tent all night, doing the bride's mehndi!"),
                Suspect("DJ Rocky", "🎧", "I never left my DJ booth. Ask anyone who was dancing!"),
                Suspect("Cousin Vikram", "🕶️", "I was outside in the parking lot, talking on the phone."),
            ),
            culprit = 2,
            groups = listOf(
                EvidenceGroup("Wedding sweets", listOf("Jalebi", "Laddu", "Barfi", "Rasgulla"), 0),
                EvidenceGroup("Things with strings", listOf("Kite", "Puppet", "Sitar", "Racket"), 1),
                EvidenceGroup("___ ring", listOf("Ear", "Nose", "Key", "Boxing"), 2),
            ),
            clues = listOf(
                "🍬 The sweets cook: \"At midnight, a man wearing sunglasses came into my kitchen and asked for a laddu.\"",
                "🎵 50 dancers saw DJ Rocky in his booth all night. The music never stopped once.",
                "💍 The bride: \"Aunty Kamala was with me the whole time. My mehndi took 3 hours!\"",
            ),
            confession = "Vikram takes off his sunglasses. \"Uncle Raj was giving all his money to the temple. " +
                "I just wanted my share… so I put something in his laddu.\" 🕶️💔",
            bonus = FermiQuestion(
                prompt = "DJ Rocky played non-stop from 8 PM to 1 AM. How many songs did he play?",
                answer = 85.0,
                unit = "songs",
                explanation = "5 hours = 300 minutes. One song ≈ 3.5 minutes. 300 ÷ 3.5 ≈ 85 songs.",
                min = 10.0,
                max = 500.0,
            ),
        ),
        CaseFile(
            title = "The Hostel Midnight Heist",
            intro = "The night before the big exam, the question paper vanishes from the warden's locked cupboard. 📄 " +
                "The hostel gate was shut all night. The thief is still inside.",
            crime = "The exam paper was stolen from the warden's cupboard at 1 AM. The lock wasn't broken. 🔐",
            suspects = listOf(
                Suspect("Night Guard Ramu", "🔦", "I was at the main gate all night. Nobody gets past me!"),
                Suspect("Topper Anu", "🤓", "I was in the library, studying till 2 AM. Like always."),
                Suspect("Gym Bro Karthik", "💪", "I was on the terrace doing push-ups. 500 of them!"),
            ),
            culprit = 0,
            groups = listOf(
                EvidenceGroup("Instant hostel food", listOf("Maggi", "Poha", "Upma", "Oats"), 0),
                EvidenceGroup("Cricket shots", listOf("Sweep", "Hook", "Pull", "Drive"), 1),
                EvidenceGroup("Keyboard keys", listOf("Escape", "Shift", "Enter", "Home"), 2),
            ),
            clues = listOf(
                "🍜 The canteen boy: \"Anu ordered Maggi to the library at 1 AM. She never left her table.\"",
                "📸 Karthik posted a push-up video from the terrace at 1 AM. 10 friends are in it.",
                "🔑 The cupboard was opened with a key. Only the warden and the night guard have one, and the warden was away.",
            ),
            confession = "Ramu lowers his torch. \"My son is writing that exam tomorrow. " +
                "I've never had money for his tuition… I only wanted to help him.\" 🔦😢",
            bonus = FermiQuestion(
                prompt = "The thief ran from the ground floor to the terrace of the 6-floor hostel. How many steps?",
                answer = 120.0,
                unit = "steps",
                explanation = "One floor ≈ 3 m high, one step ≈ 15 cm, so about 20 steps per floor. 6 × 20 = 120 steps.",
                min = 20.0,
                max = 1000.0,
            ),
        ),
        CaseFile(
            title = "Diamonds on the Night Express",
            intro = "Somewhere between Hyderabad and Chennai, a diamond merchant wakes up… and his bag of diamonds is gone. 💎 " +
                "The train never stopped. The thief is still on board.",
            crime = "The diamond bag vanished from Coach S4 at 3 AM, while everyone was asleep. 🚂",
            suspects = listOf(
                Suspect("Ticket Checker", "🎫", "I was checking tickets in the other coaches all night."),
                Suspect("New Bride Meera", "👰", "I was fast asleep on my upper berth. I never woke up!"),
                Suspect("Chai Vendor Babu", "☕", "I was in the pantry car making chai. Chai, chai, chai!"),
            ),
            culprit = 1,
            groups = listOf(
                EvidenceGroup("Train classes", listOf("Sleeper", "General", "Chair Car", "AC"), 0),
                EvidenceGroup("Chai companions", listOf("Biscuit", "Rusk", "Samosa", "Pakora"), 1),
                EvidenceGroup("Famous detectives", listOf("Byomkesh", "Feluda", "Holmes", "Poirot"), 2),
            ),
            clues = listOf(
                "🎫 Passengers in 6 other coaches remember the ticket checker waking them up all night.",
                "☕ The pantry manager: \"Babu never left. He made 200 cups of chai that night!\"",
                "👟 Fresh footprints lead from the merchant's seat… straight up the ladder to an upper berth.",
            ),
            confession = "Meera smiles and opens her bangle box. The diamonds sparkle inside. " +
                "\"There was no wedding. My 'husband' is my partner. We do this on every train.\" 👰💎",
            bonus = FermiQuestion(
                prompt = "The train went about 700 km that night. How many times did one wheel turn?",
                answer = 240_000.0,
                unit = "turns",
                explanation = "A train wheel is about 0.9 m wide, so one turn ≈ 2.9 m. 700,000 m ÷ 2.9 m ≈ 240,000 turns.",
                min = 1_000.0,
                max = 10_000_000.0,
            ),
        ),
    )
}
