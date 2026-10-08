package com.alibi.engine.cases

import com.alibi.engine.board.EvidenceGroup
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
            headlineSubject = "Sangeet murder",
            call = "Inspector {name}, sorry to wake you. Uncle Raj is dead at his nephew's wedding sangeet. " +
                "400 guests, loud music, and nobody saw anything. The family is panicking. " +
                "I need my best detective. The car is waiting outside.",
            crime = "Uncle Raj was found dead in the kitchen at his nephew's sangeet.",
            scene = CrimeScene(
                place = "The kitchen",
                evidence = listOf(
                    SceneItem("🟠", 0.24f, 0.70f, "Half-eaten laddu", "A half-eaten laddu next to the body. Someone wanted him to eat it."),
                    SceneItem("🕶️", 0.66f, 0.34f, "Sunglasses mark", "A greasy sunglasses mark on the counter. Who wears sunglasses at midnight?"),
                    SceneItem("👣", 0.62f, 0.86f, "Footprints", "Footprints leading out of the back door, towards the parking lot."),
                ),
                decor = listOf(
                    SceneItem("🫖", 0.16f, 0.24f), SceneItem("🍳", 0.42f, 0.26f), SceneItem("🧂", 0.86f, 0.22f),
                    SceneItem("🪣", 0.80f, 0.60f), SceneItem("🪑", 0.38f, 0.58f), SceneItem("🧺", 0.12f, 0.88f),
                    SceneItem("🚪", 0.92f, 0.88f),
                ),
            ),
            groups = listOf(
                EvidenceGroup("Wedding sweets", listOf("Jalebi", "Laddu", "Barfi", "Rasgulla")),
                EvidenceGroup("Things with strings", listOf("Kite", "Puppet", "Sitar", "Racket")),
                EvidenceGroup("___ ring", listOf("Ear", "Nose", "Key", "Boxing")),
            ),
            decoys = listOf("Samosa", "Drum", "Bangle", "Ticket"),
            statements = listOf(
                Statement(
                    "🍬 The sweets cook",
                    "I'm a cook, I only remember sweet things! Find the 4 SWEETS on the wall (like gulab jamun) and I'll tell you what I saw.",
                    "🍬 Cook's statement",
                    "\"At midnight, a man wearing sunglasses came into my kitchen and asked for one laddu.\"",
                ),
                Statement(
                    "🎵 The dancers",
                    "We were dancing to music all night! Find 4 things that have STRINGS (like a guitar) and we'll talk.",
                    "🎵 Dancers' statement",
                    "50 dancers say DJ Rocky never left his DJ booth. The music never stopped once.",
                ),
                Statement(
                    "💍 The bride",
                    "Everything tonight is about rings! Find 4 words that make a new word when you add RING after them (like FINGER + RING = FINGER-RING).",
                    "💍 Bride's statement",
                    "\"Aunty Kamala was with me the whole time. My mehndi took 3 hours!\"",
                ),
            ),
            suspects = listOf(
                Suspect(
                    "Aunty Kamala", "👵",
                    "Arre beta, I was in the mehndi tent all night, doing the bride's mehndi!",
                    listOf("Why are you showing me this? I was with the bride the whole time.", "Beta, I'm 68. I can barely open a jar of pickle."),
                ),
                Suspect(
                    "DJ Rocky", "🎧",
                    "Bro, I never left my DJ booth. Ask anyone who was dancing!",
                    listOf("Bro, I was busy playing Kala Chashma. I didn't see anything.", "Can I go now? I have another wedding tomorrow, bro."),
                ),
                Suspect(
                    "Cousin Vikram", "🕶️",
                    "I was outside in the parking lot, on a long phone call. Business stuff.",
                    listOf("I told you, I was outside. Can I go now?", "Why would I care about any of this?"),
                ),
            ),
            culprit = 2,
            reactions = mapOf(
                0 to mapOf(2 to Reaction(Mood.NERVOUS, "S-sunglasses? Lots of people wear sunglasses… at midnight… indoors… 😰", "Vikram: sweating about the sunglasses man 💦")),
                1 to mapOf(1 to Reaction(Mood.RELIEVED, "Fifty witnesses, bro! I told you. 😎", "DJ Rocky: alibi confirmed ✓")),
                2 to mapOf(0 to Reaction(Mood.RELIEVED, "See? Ask the bride! I'm too old for murders, beta. 😌", "Aunty Kamala: alibi confirmed ✓")),
            ),
            releaseLines = mapOf(
                0 to "Inspector, the bride herself says Aunty Kamala was with her! Release her at once.",
                1 to "Inspector, DJ Rocky has 50 witnesses on the dance floor! Let him go.",
            ),
            confession = "Vikram slowly takes off his sunglasses. \"Uncle Raj was giving all his money to the temple. " +
                "Every rupee. I only wanted my share… so I put something in his laddu.\"",
            escapeStory = "Vikram slipped away while you were busy. Two days later, airport police caught him boarding " +
                "a flight to Dubai. He confessed: \"Uncle Raj was giving all his money to the temple. " +
                "I only wanted my share… so I put something in his laddu.\"",
            caughtQuote = "The sunglasses gave him away",
        ),
        CaseFile(
            title = "The Hostel Midnight Heist",
            headlineSubject = "hostel paper heist",
            call = "Inspector {name}, tomorrow's university exam paper has been stolen from the boys' hostel. " +
                "If it leaks, 5,000 students suffer. The gate was locked all night, so the thief is still inside. " +
                "Find them before sunrise.",
            crime = "The exam paper was stolen from the warden's locked cupboard at 1 AM.",
            scene = CrimeScene(
                place = "The warden's office",
                evidence = listOf(
                    SceneItem("🔑", 0.70f, 0.30f, "Unbroken lock", "The cupboard lock isn't broken. Someone opened it with a key."),
                    SceneItem("🔦", 0.22f, 0.66f, "Torch battery cover", "A torch battery cover on the floor. Who carries a torch at 1 AM?"),
                    SceneItem("📄", 0.58f, 0.84f, "Torn envelope", "A torn corner of the exam paper envelope, near the door."),
                ),
                decor = listOf(
                    SceneItem("🗄️", 0.18f, 0.22f), SceneItem("📚", 0.44f, 0.24f), SceneItem("🕰️", 0.88f, 0.20f),
                    SceneItem("🪑", 0.40f, 0.60f), SceneItem("🪴", 0.84f, 0.62f), SceneItem("🧹", 0.12f, 0.88f),
                    SceneItem("🚪", 0.92f, 0.88f),
                ),
            ),
            groups = listOf(
                EvidenceGroup("Instant hostel food", listOf("Maggi", "Poha", "Upma", "Oats")),
                EvidenceGroup("Cricket shots", listOf("Sweep", "Hook", "Pull", "Drive")),
                EvidenceGroup("Keyboard keys", listOf("Escape", "Shift", "Enter", "Home")),
            ),
            decoys = listOf("Chai", "Bat", "Mouse", "Pillow"),
            statements = listOf(
                Statement(
                    "🍜 The canteen boy",
                    "I only think about food, sir! Find 4 HOSTEL SNACKS you can make in 5 minutes (like bread omelette).",
                    "🍜 Canteen boy's statement",
                    "\"Anu ordered Maggi to the library at 1 AM. I delivered it. She never left her table.\"",
                ),
                Statement(
                    "📸 Karthik's friend",
                    "Bro, I only talk cricket. Find 4 CRICKET SHOTS (like a square cut) and I'll show you my phone.",
                    "📸 Instagram video",
                    "Karthik posted a push-up video from the terrace at 1 AM. 10 friends are in it.",
                ),
                Statement(
                    "🔑 The warden",
                    "I'm a computer teacher too. Find 4 KEYS on a computer keyboard (like Ctrl) and I'll tell you about the cupboard key.",
                    "🔑 Warden's statement",
                    "\"Only two people have the cupboard key: me and the night guard. I was at my sister's wedding.\"",
                ),
            ),
            suspects = listOf(
                Suspect(
                    "Night Guard Ramu", "🔦",
                    "I was at the main gate all night, sahib. Nobody gets past Ramu!",
                    listOf("I only guard the gate, sahib. I don't know anything about this.", "Sahib, my shift is over. Can I go home?"),
                ),
                Suspect(
                    "Topper Anu", "🤓",
                    "I was in the library, studying till 2 AM. Like always.",
                    listOf("Why would a topper need to steal the paper? I already know everything.", "Can we hurry? I have revision to do."),
                ),
                Suspect(
                    "Gym Bro Karthik", "💪",
                    "Bro, I was on the terrace doing push-ups. 500 of them!",
                    listOf("Bro, I don't even read papers. I lift.", "Is this going to take long? It's leg day."),
                ),
            ),
            culprit = 0,
            reactions = mapOf(
                0 to mapOf(1 to Reaction(Mood.RELIEVED, "Obviously. Maggi and Maths, every night. 🤓", "Anu: alibi confirmed ✓")),
                1 to mapOf(2 to Reaction(Mood.RELIEVED, "Bro, 500 push-ups, all on camera! 💪", "Karthik: alibi confirmed ✓")),
                2 to mapOf(0 to Reaction(Mood.NERVOUS, "K-key? Many people… the warden… I mean… sahib, I need some water… 😰", "Ramu: shaking about the key 💦")),
            ),
            releaseLines = mapOf(
                1 to "Inspector, the canteen boy delivered Maggi to Anu in the library! Let her go.",
                2 to "Inspector, Karthik's push-up video has 10 witnesses! Release him.",
            ),
            confession = "Ramu lowers his torch. \"My son is writing that exam tomorrow. " +
                "I never had money for his tuition… I only wanted to help him.\"",
            escapeStory = "Ramu left before sunrise. Police found him at his village two days later, the paper still unopened. " +
                "He confessed: \"My son is writing that exam. I never had money for his tuition… I only wanted to help him.\"",
            caughtQuote = "The key gave him away",
        ),
        CaseFile(
            title = "Diamonds on the Night Express",
            headlineSubject = "Night Express diamond theft",
            call = "Inspector {name}, a diamond merchant on the Hyderabad–Chennai Express just lost a bag of diamonds worth ₹5 crore. " +
                "The train hasn't stopped since. I've arranged for you to board at the next station. Hurry.",
            crime = "A bag of diamonds vanished from Coach S4 at 3 AM on the Night Express.",
            scene = CrimeScene(
                place = "Coach S4",
                evidence = listOf(
                    SceneItem("💎", 0.30f, 0.72f, "A tiny diamond", "One tiny diamond, dropped near the berth ladder."),
                    SceneItem("👟", 0.64f, 0.40f, "Wet footprints", "Wet footprints going up to an upper berth."),
                    SceneItem("💍", 0.70f, 0.84f, "Heavy bangle box", "A wedding bangle box, much heavier than it should be."),
                ),
                decor = listOf(
                    SceneItem("🧳", 0.16f, 0.24f), SceneItem("🪟", 0.46f, 0.20f), SceneItem("🛏️", 0.86f, 0.24f),
                    SceneItem("🪜", 0.40f, 0.60f), SceneItem("🥤", 0.84f, 0.60f), SceneItem("🧴", 0.12f, 0.88f),
                    SceneItem("🚪", 0.92f, 0.88f),
                ),
            ),
            groups = listOf(
                EvidenceGroup("Train classes", listOf("Sleeper", "General", "Chair Car", "AC")),
                EvidenceGroup("Chai companions", listOf("Biscuit", "Rusk", "Samosa", "Pakora")),
                EvidenceGroup("Famous detectives", listOf("Byomkesh", "Feluda", "Holmes", "Poirot")),
            ),
            decoys = listOf("Platform", "Lassi", "Inspector", "Luggage"),
            statements = listOf(
                Statement(
                    "🎫 The passengers",
                    "We're all angry about our tickets! Find 4 TRAIN CLASSES you can book (like First Class).",
                    "🎫 Passengers' statement",
                    "Passengers in 6 other coaches remember the ticket checker waking them up all night.",
                ),
                Statement(
                    "☕ The pantry manager",
                    "I serve chai all night. Find 4 snacks people eat WITH CHAI (like a bun) and I'll tell you about Babu.",
                    "☕ Pantry manager's statement",
                    "\"Babu never left the pantry. He made 200 cups of chai that night!\"",
                ),
                Statement(
                    "🔍 The coach attendant",
                    "I read mystery novels every night! Find 4 famous DETECTIVES (like Miss Marple) and I'll tell you what I saw.",
                    "🔍 Coach attendant's statement",
                    "\"I saw wet footprints going up the ladder… to the new bride's upper berth.\"",
                ),
            ),
            suspects = listOf(
                Suspect(
                    "Ticket Checker", "🎫",
                    "I was checking tickets in the other coaches all night. It's my job!",
                    listOf("I check tickets, not diamonds, sir.", "Do you have a ticket, by the way?"),
                ),
                Suspect(
                    "New Bride Meera", "👰",
                    "I was fast asleep on my upper berth. I never woke up!",
                    listOf("I don't know anything. I was sleeping.", "My husband will be worried. Can I go back?"),
                ),
                Suspect(
                    "Chai Vendor Babu", "☕",
                    "I was in the pantry car making chai. Chai, chai, chai!",
                    listOf("Chai, sir? Only ₹10.", "I only know chai, sir. Diamonds are too costly for me."),
                ),
            ),
            culprit = 1,
            reactions = mapOf(
                0 to mapOf(0 to Reaction(Mood.RELIEVED, "Six coaches of angry passengers can't be wrong! 🎫", "Ticket checker: alibi confirmed ✓")),
                1 to mapOf(2 to Reaction(Mood.RELIEVED, "Chai, chai, alibi! ☕😄", "Babu: alibi confirmed ✓")),
                2 to mapOf(1 to Reaction(Mood.NERVOUS, "F-footprints? I… I sleepwalk sometimes! Yes, I sleepwalk… 😰", "Meera: suddenly she sleepwalks 💦")),
            ),
            releaseLines = mapOf(
                0 to "Inspector, six coaches of passengers saw the ticket checker all night! Let him go.",
                2 to "Inspector, Babu made 200 cups of chai in the pantry! Release him.",
            ),
            confession = "Meera smiles and opens her bangle box. The diamonds sparkle inside. " +
                "\"There was no wedding. My 'husband' is my partner. We do this on every train.\"",
            escapeStory = "Meera jumped off at Nellore station. The next day she was caught selling a diamond at a jewellery shop. " +
                "She confessed: \"There was no wedding. My 'husband' is my partner. We do this on every train.\"",
            caughtQuote = "The footprints gave her away",
        ),
    )
}
