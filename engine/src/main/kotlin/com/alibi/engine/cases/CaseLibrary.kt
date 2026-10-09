package com.alibi.engine.cases

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
        // ---------------------------------------------------------------- Case 1
        CaseFile(
            title = "Death at the Sangeet",
            headlineSubject = "Sangeet murder",
            call = listOf(
                "Inspector {name}, Uncle Raj is dead at his nephew's wedding.",
                "400 guests, and nobody saw a thing. Find the killer before morning.",
            ),
            arrival = "The sangeet music has stopped. Everyone is waiting outside.",
            briefing = listOf(
                BriefingLine("🧍", "Victim", "Uncle Raj, 62. Rich, and the groom's uncle."),
                BriefingLine("🕛", "Time", "Around midnight"),
                BriefingLine("📍", "Place", "The kitchen"),
                BriefingLine("❓", "Cause", "Unknown. That's your job, Inspector.", unknown = true),
            ),
            place = "kitchen",
            examination = Examination(
                title = "Examine the body",
                objectEmoji = null,
                spots = listOf(
                    ExamSpot("Mouth", "No wounds anywhere. His lips smell bitter, like medicine.", 0.50f, 0.18f),
                    ExamSpot("Right hand", "He's holding the wrapper of a sweet.", 0.28f, 0.50f),
                    ExamSpot("Pocket", "A note: \"I've changed my will. All my money goes to the temple. – Raj\"", 0.58f, 0.64f),
                ),
                question = "How did Uncle Raj die?",
                options = listOf("🔪 Someone stabbed him", "🧪 Someone poisoned him", "🪜 He fell down"),
                answer = 1,
                why = "No wounds, bitter lips, and a sweet in his hand. Someone poisoned his sweet.",
            ),
            hideouts = listOf(
                Hideout("fridge", "🧊", "Fridge", 0.14f, 0.16f, "Only milk for the kheer."),
                Hideout("tea", "🫖", "Teapot", 0.40f, 0.22f, "Cold chai. Nothing else."),
                Hideout("spice", "🧂", "Spice rack", 0.64f, 0.22f),
                Hideout("window", "🪟", "Window", 0.88f, 0.14f, "Locked from inside."),
                Hideout("stove", "🍳", "Stove", 0.26f, 0.50f, "Burnt jalebi. The cook was busy."),
                Hideout("box", "📦", "Sweet boxes", 0.74f, 0.52f, "Gift boxes, all still sealed."),
                Hideout("chair", "🪑", "Chair", 0.50f, 0.62f, "Just a wobbly chair."),
                Hideout("basket", "🧺", "Onion basket", 0.14f, 0.82f, "Onions. Lots of onions."),
                Hideout("bin", "🗑️", "Dustbin", 0.44f, 0.86f),
                Hideout("bucket", "🪣", "Bucket", 0.66f, 0.80f, "Dirty mop water."),
                Hideout("door", "🚪", "Back door", 0.88f, 0.80f),
            ),
            clues = listOf(
                Clue(
                    at = "bin", emoji = "🟠", name = "Half-eaten laddu",
                    riddle = "Someone threw away the sweet that killed him. Look where the kitchen throws what it doesn't need.",
                    found = "Hidden under the peels. The wrapper has orange-brown **mehndi stains** on it.",
                    pointsTo = 0, why = "Aunty Kamala has mehndi on her hands. But did she poison it? Ask her later.",
                    card = "MEHNDI stains on the laddu wrapper",
                ),
                Clue(
                    at = "spice", emoji = "🕶️", name = "Sunglasses",
                    riddle = "To poison a sweet, you need something hot and powdery. The killer put something down while reaching for it.",
                    found = "A pair of **sunglasses**, left next to the chilli powder. At midnight!",
                    pointsTo = 2, why = "Cousin Vikram wears sunglasses, even at night.",
                    card = "SUNGLASSES left in the kitchen",
                ),
                Clue(
                    at = "door", emoji = "👣", name = "Footprints",
                    riddle = "The killer didn't leave through the party. Find the way out that guests never use.",
                    found = "Muddy footprints going out to where the **cars are parked**.",
                    pointsTo = 2, why = "Cousin Vikram drives a car, and says he was in the parking lot.",
                    card = "Footprints to the PARKING LOT",
                ),
            ),
            suspects = listOf(
                Suspect(
                    "Aunty Kamala", "👵", "Mehndi tent",
                    listOf(Trait("🌿", "Mehndi on her hands"), Trait("👓", "Reading glasses")),
                    "I was doing the bride's mehndi all night, beta!",
                    listOf(
                        "Haan, I gave Raj that laddu at 11 o'clock, with my mehndi hands! He was perfectly fine then.",
                        "Sunglasses? At my age I can barely see with my glasses!",
                        "Parking lot? I don't even know how to drive.",
                    ),
                ),
                Suspect(
                    "DJ Rocky", "🎧", "DJ booth",
                    listOf(Trait("🎧", "Always in headphones"), Trait("🛺", "Came by auto")),
                    "Bro, I never left my DJ booth. Ask the dancers!",
                    listOf(
                        "Mehndi? Do my hands look like a bride's, bro?",
                        "I wear headphones, bro, not sunglasses.",
                        "My car is at home, bro. I came by auto.",
                    ),
                ),
                Suspect(
                    "Cousin Vikram", "🧔", "Parking lot",
                    listOf(Trait("🕶️", "Wears sunglasses"), Trait("🚗", "Drives a car")),
                    "I was in the parking lot, on a long phone call.",
                    listOf("A laddu? I don't even eat sweets.", "", ""),
                    gotchas = mapOf(
                        1 to "Your sunglasses were in the kitchen, Vikram! Not in the parking lot.",
                        2 to "Your footprints go from the kitchen to the parking lot!",
                    ),
                ),
            ),
            culprit = 2,
            releaseLines = mapOf(
                0 to "The bride says Aunty Kamala was with her all night.",
                1 to "50 dancers saw DJ Rocky in his booth all night.",
            ),
            confession = "\"Uncle Raj was giving all his money to the temple. I only wanted my share.\"",
            escapeStory = "Cousin Vikram got away. Airport police caught him two days later. \"I only wanted my share of the money.\"",
        ),

        // ---------------------------------------------------------------- Case 2
        CaseFile(
            title = "The Hostel Midnight Heist",
            headlineSubject = "hostel paper heist",
            call = listOf(
                "Inspector {name}, tomorrow's exam paper was stolen from the boys' hostel.",
                "The gate was locked all night. The thief is still inside.",
            ),
            arrival = "Sleepy students are standing in the corridor in their pyjamas.",
            briefing = listOf(
                BriefingLine("📄", "Stolen", "The university exam paper"),
                BriefingLine("🕐", "Time", "Around 1 AM"),
                BriefingLine("📍", "Place", "The warden's office"),
                BriefingLine("❓", "How", "Unknown. That's your job, Inspector.", unknown = true),
            ),
            place = "office",
            examination = Examination(
                title = "Examine the cupboard",
                objectEmoji = "🗄️",
                spots = listOf(
                    ExamSpot("Lock", "Not broken. Not even scratched.", 0.50f, 0.30f),
                    ExamSpot("Shelf", "Only the exam paper is missing. The cash box wasn't touched.", 0.30f, 0.58f),
                    ExamSpot("Keyhole", "Fresh shiny scratches inside, like a key turned many times.", 0.70f, 0.58f),
                ),
                question = "How did the thief open the cupboard?",
                options = listOf("🔨 Broke the lock", "🔑 Used a key", "🪟 Came in through the window"),
                answer = 1,
                why = "The lock isn't broken, and the keyhole has fresh scratches. The thief had a key.",
            ),
            hideouts = listOf(
                Hideout("shelf", "📚", "Bookshelf", 0.14f, 0.16f, "Dusty old registers."),
                Hideout("clock", "🕰️", "Wall clock", 0.42f, 0.14f),
                Hideout("plant", "🪴", "Plant", 0.66f, 0.22f, "Just soil. Someone forgot to water it."),
                Hideout("window", "🪟", "Window", 0.88f, 0.14f, "Locked from inside."),
                Hideout("lamp", "💡", "Desk lamp", 0.24f, 0.50f, "Switched off. Cold."),
                Hideout("coat", "🧥", "Coat stand", 0.76f, 0.50f, "The warden's raincoat."),
                Hideout("chair", "🪑", "Chair", 0.50f, 0.62f),
                Hideout("broom", "🧹", "Broom", 0.14f, 0.82f, "Just a broom."),
                Hideout("bin", "🗑️", "Dustbin", 0.44f, 0.86f),
                Hideout("box", "📦", "Box", 0.66f, 0.80f, "Old answer sheets from last year."),
                Hideout("door", "🚪", "Door", 0.88f, 0.80f, "No footprints. Someone wiped the floor."),
            ),
            clues = listOf(
                Clue(
                    at = "bin", emoji = "🍜", name = "Empty Maggi packet",
                    riddle = "Someone had a midnight snack in here. Look where the room's rubbish goes.",
                    found = "A crumpled **Maggi packet**, still warm.",
                    pointsTo = 1, why = "Topper Anu loves Maggi. But was she here? Ask her later.",
                    card = "A MAGGI packet in the office",
                ),
                Clue(
                    at = "chair", emoji = "🔦", name = "Torch",
                    riddle = "The thief needed light to find the paper. It rolled under something you sit on.",
                    found = "A heavy metal **torch**, still switched on.",
                    pointsTo = 0, why = "Night Guard Ramu always carries a torch.",
                    card = "A TORCH under the warden's chair",
                ),
                Clue(
                    at = "clock", emoji = "🔑", name = "Key ring",
                    riddle = "Time tells the truth. Look behind the thing that ticks.",
                    found = "A big **ring of hostel keys**, hidden in a hurry.",
                    pointsTo = 0, why = "Night Guard Ramu carries all the hostel keys.",
                    card = "The hostel KEY RING behind the clock",
                ),
            ),
            suspects = listOf(
                Suspect(
                    "Night Guard Ramu", "👴", "Main gate",
                    listOf(Trait("🔑", "Carries all the hostel keys"), Trait("🔦", "Always has a torch")),
                    "I was at the main gate all night, sahib. Nobody gets past Ramu!",
                    listOf("Maggi? I eat roti, sahib.", "", ""),
                    gotchas = mapOf(
                        1 to "Your torch was under the warden's chair, Ramu!",
                        2 to "Only you carry the hostel keys, Ramu!",
                    ),
                ),
                Suspect(
                    "Topper Anu", "👩‍🎓", "Library",
                    listOf(Trait("🍜", "Eats Maggi every night"), Trait("📚", "Studies till 2 AM")),
                    "I was in the library, studying till 2 AM. Like always.",
                    listOf(
                        "I order Maggi every night! The canteen boy brings it to the library. Ask him.",
                        "A torch? The library has lights, Inspector.",
                        "Keys? I'm a student. I don't even have the library key.",
                    ),
                ),
                Suspect(
                    "Gym Bro Karthik", "💪", "Terrace",
                    listOf(Trait("🏋️", "Works out at night"), Trait("📱", "Films everything")),
                    "Bro, I was on the terrace doing push-ups. It's all on my phone!",
                    listOf(
                        "Bro, I eat protein, not Maggi.",
                        "I use my phone light, bro. Look, it's in my video.",
                        "I can't even find my own room key, bro.",
                    ),
                ),
            ),
            culprit = 0,
            releaseLines = mapOf(
                1 to "The canteen boy brought Anu's Maggi to the library at 1 AM.",
                2 to "Karthik's push-up video shows him on the terrace at 1 AM.",
            ),
            confession = "\"My son is writing that exam tomorrow. I never had money for his tuition. I only wanted to help him.\"",
            escapeStory = "Ramu left before sunrise. Police found him in his village two days later, the paper still unopened.",
        ),

        // ---------------------------------------------------------------- Case 3
        CaseFile(
            title = "Diamonds on the Night Express",
            headlineSubject = "Night Express diamond theft",
            call = listOf(
                "Inspector {name}, a merchant's diamonds were stolen on the Hyderabad–Chennai Express.",
                "The train hasn't stopped since. The thief is still on board.",
            ),
            arrival = "Passengers are peeking out of their curtains. The merchant is crying.",
            briefing = listOf(
                BriefingLine("💎", "Stolen", "A bag of diamonds, worth ₹5 crore"),
                BriefingLine("🕒", "Time", "Around 3 AM"),
                BriefingLine("📍", "Place", "Coach S4"),
                BriefingLine("❓", "How", "Unknown. That's your job, Inspector.", unknown = true),
            ),
            place = "coach",
            examination = Examination(
                title = "Examine the merchant's bag",
                objectEmoji = "👜",
                spots = listOf(
                    ExamSpot("Zip", "The zip is still closed.", 0.50f, 0.26f),
                    ExamSpot("Side", "A long, neat cut along the side.", 0.28f, 0.58f),
                    ExamSpot("Inside", "Empty. Not one diamond left.", 0.70f, 0.58f),
                ),
                question = "How did the thief open the bag?",
                options = listOf("🤏 Opened the zip", "🔪 Cut it with a blade", "🔐 Unlocked it"),
                answer = 1,
                why = "The zip is closed, but there's a neat cut. The thief used a blade.",
            ),
            hideouts = listOf(
                Hideout("window", "🪟", "Window", 0.14f, 0.16f, "Just dark fields outside."),
                Hideout("news", "📰", "Newspaper", 0.40f, 0.22f, "Yesterday's cricket scores."),
                Hideout("bottle", "🧴", "Water bottles", 0.64f, 0.22f, "Half-empty bottles."),
                Hideout("hanger", "🧥", "Coat hook", 0.88f, 0.14f, "A shawl, nothing inside."),
                Hideout("sink", "🚰", "Wash basin", 0.24f, 0.50f),
                Hideout("berth", "🛏️", "Lower berth", 0.74f, 0.52f, "A snoring uncle. Nothing else."),
                Hideout("ladder", "🪜", "Berth ladder", 0.50f, 0.62f),
                Hideout("cups", "🥤", "Chai cups", 0.14f, 0.82f, "Empty chai cups."),
                Hideout("luggage", "🎒", "Luggage", 0.44f, 0.86f),
                Hideout("bin", "🗑️", "Dustbin", 0.66f, 0.80f, "Banana peels and wrappers."),
                Hideout("door", "🚪", "Coach door", 0.88f, 0.80f, "Locked. The train is moving fast."),
            ),
            clues = listOf(
                Clue(
                    at = "sink", emoji = "🔪", name = "Small knife",
                    riddle = "The thief cut the bag with something sharp, then cleaned it. Look where people wash.",
                    found = "A small **knife that smells of ginger**.",
                    pointsTo = 2, why = "Chai Vendor Babu cuts ginger for his chai. But is it him? Ask him later.",
                    card = "A GINGER knife in the wash basin",
                ),
                Clue(
                    at = "ladder", emoji = "✨", name = "Glitter",
                    riddle = "After the theft, the thief climbed up to hide. Look at what you climb to reach the top.",
                    found = "Golden **wedding glitter** on every step.",
                    pointsTo = 1, why = "New Bride Meera wears a glittery wedding saree.",
                    card = "WEDDING GLITTER on the ladder",
                ),
                Clue(
                    at = "luggage", emoji = "💍", name = "Heavy bangle box",
                    riddle = "Something heavy must be hidden. Look where passengers keep their bags.",
                    found = "A **bangle box** that rattles like stones.",
                    pointsTo = 1, why = "New Bride Meera wears heavy bangles.",
                    card = "A rattling BANGLE BOX",
                ),
            ),
            suspects = listOf(
                Suspect(
                    "Ticket Checker", "👨‍💼", "Other coaches",
                    listOf(Trait("🎫", "Checks tickets all night"), Trait("🖊️", "Carries only a pen")),
                    "I was checking tickets in the other coaches all night.",
                    listOf(
                        "A knife? I carry a pen, sir, not a knife.",
                        "Glitter? On me? Never.",
                        "I don't wear bangles, sir.",
                    ),
                ),
                Suspect(
                    "New Bride Meera", "👰", "Upper berth",
                    listOf(Trait("✨", "Glittery wedding saree"), Trait("💍", "Wears heavy bangles")),
                    "I was fast asleep on my upper berth. I never woke up!",
                    listOf("A knife? I'm a bride, I don't even cook.", "", ""),
                    gotchas = mapOf(
                        1 to "Your saree glitter is all over the ladder, Meera!",
                        2 to "Bangles don't rattle like stones, Meera!",
                    ),
                ),
                Suspect(
                    "Chai Vendor Babu", "🧑‍🍳", "Pantry car",
                    listOf(Trait("☕", "Makes chai all night"), Trait("🫚", "Cuts ginger with a small knife")),
                    "I was in the pantry car making chai. Chai, chai, chai!",
                    listOf(
                        "That's my ginger knife! It went missing at 2 AM. Anyone could take it from the pantry.",
                        "Glitter? Only tea leaves on me, sir.",
                        "I can't even afford one bangle, sir.",
                    ),
                ),
            ),
            culprit = 1,
            releaseLines = mapOf(
                0 to "Passengers in six coaches saw the ticket checker all night.",
                2 to "The pantry manager says Babu made 200 cups of chai that night.",
            ),
            confession = "\"There was no wedding. My 'husband' is my partner. We do this on every train.\"",
            escapeStory = "Meera jumped off at the next station. She was caught selling a diamond the next day.",
        ),
    )
}
