package com.alibi.engine.cases

import com.alibi.engine.board.EvidenceGroup
import com.alibi.engine.fermi.FermiQuestion
import java.time.LocalDate

/**
 * The daily case schedule. Case #1 is on [LAUNCH_DAY]; after that it's one case per day.
 * When the authored cases run out they repeat, but with a new seed, so the logic puzzle is new.
 */
object CaseLibrary {
    val LAUNCH_DAY: Long = LocalDate.of(2026, 10, 8).toEpochDay()

    fun caseNumber(epochDay: Long): Int = (epochDay - LAUNCH_DAY + 1).coerceAtLeast(1).toInt()

    fun forDay(epochDay: Long): PlayableCase {
        val number = caseNumber(epochDay)
        val index = Math.floorMod(number - 1, cases.size)
        val round = (number - 1) / cases.size
        val file = cases[index]
        return PlayableCase.build(number, file, seed = file.logicSeed + round * 7919L)
    }

    val cases: List<CaseFile> = listOf(
        CaseFile(
            title = "Death at the Sangeet",
            intro = "Music, dancing, 400 guests… and at midnight, the groom's rich uncle is found dead. " +
                "Four people had a reason. Only one had the chance.",
            theme = CaseTheme(
                suspects = listOf(
                    person("Aunty Kamala", "👵"),
                    person("DJ Rocky", "🎧"),
                    person("Cousin Vikram", "🕶️"),
                    person("The Photographer", "📸"),
                ),
                weapons = listOf(
                    thing("Poisoned Laddu", "🟠"),
                    thing("Garland", "🌼"),
                    thing("Dandiya Stick", "🥢"),
                    thing("Cake Knife", "🔪"),
                ),
                rooms = listOf(
                    place("Mehndi Tent", "⛺"),
                    place("Dance Floor", "🪩", preposition = "on"),
                    place("Kitchen", "🍳"),
                    place("Parking Lot", "🚗"),
                ),
            ),
            groups = listOf(
                EvidenceGroup("Sweets at the buffet", listOf("Jalebi", "Laddu", "Barfi", "Rasgulla"), 0),
                EvidenceGroup("Classical dances", listOf("Kathak", "Odissi", "Manipuri", "Kuchipudi"), 1),
                EvidenceGroup("Things with strings", listOf("Kite", "Puppet", "Sitar", "Racket"), 2),
                EvidenceGroup("___ ring", listOf("Ear", "Nose", "Key", "Boxing"), 3),
            ),
            fermi = FermiQuestion(
                prompt = "The DJ played non-stop from 8 PM until the body was found at 1 AM. " +
                    "About how many songs did he play?",
                answer = 85.0,
                unit = "songs",
                explanation = "8 PM to 1 AM is 5 hours = 300 minutes. A party song is about 3.5 minutes. " +
                    "300 ÷ 3.5 ≈ 85 songs.",
            ),
            logicSeed = 1001L,
        ),
        CaseFile(
            title = "The Hostel Midnight Heist",
            intro = "The night before the big exam, the question paper vanished from the warden's locked cupboard. " +
                "The hostel was sealed. The thief is still inside.",
            theme = CaseTheme(
                suspects = listOf(
                    person("Warden Rao", "🧔"),
                    person("Topper Anu", "🤓"),
                    person("Gym Bro Karthik", "💪"),
                    person("The Night Guard", "🔦"),
                ),
                weapons = listOf(
                    thing("Master Key", "🗝️"),
                    thing("Hair Clip", "📎"),
                    thing("Bedsheet Rope", "🪢"),
                    thing("Screwdriver", "🪛"),
                ),
                rooms = listOf(
                    place("Mess Hall", "🍛"),
                    place("Terrace", "🌙", preposition = "on"),
                    place("Room 204", "🚪", ref = "Room 204"),
                    place("Library", "📚"),
                ),
            ),
            groups = listOf(
                EvidenceGroup("Instant hostel food", listOf("Maggi", "Poha", "Upma", "Oats"), 0),
                EvidenceGroup("Exam season words", listOf("Viva", "Backlog", "Syllabus", "Revision"), 1),
                EvidenceGroup("Cricket shots", listOf("Sweep", "Hook", "Pull", "Drive"), 2),
                EvidenceGroup("Keyboard keys", listOf("Escape", "Shift", "Enter", "Home"), 3),
            ),
            fermi = FermiQuestion(
                prompt = "The thief ran from the ground floor up to the terrace of the 6-storey hostel. " +
                    "About how many steps did they climb?",
                answer = 120.0,
                unit = "steps",
                explanation = "Each floor is about 3 m high and a step is about 15 cm, so roughly 20 steps per floor. " +
                    "6 floors × 20 ≈ 120 steps.",
            ),
            roomReveal = "The empty paper envelope was found {room}.",
            weaponReveal = "{weapon} was found next to the broken cupboard.",
            logicSeed = 2002L,
        ),
        CaseFile(
            title = "Murder on the Night Express",
            intro = "Somewhere between Hyderabad and Chennai, a diamond merchant never woke up. " +
                "The train didn't stop once. The killer is still on board.",
            theme = CaseTheme(
                suspects = listOf(
                    person("The Ticket Checker", "🎫"),
                    person("Colonel Sharma", "🎖️"),
                    person("The Chai Vendor", "☕"),
                    person("The Newlywed Bride", "👰"),
                ),
                weapons = listOf(
                    thing("Iron Chain", "⛓️"),
                    thing("Thermos", "🧴"),
                    thing("Pillow", "🛏️"),
                    thing("Pen Knife", "🗡️"),
                ),
                rooms = listOf(
                    place("Pantry Car", "🍲"),
                    place("Coach S4", "🚃", ref = "Coach S4"),
                    place("Upper Berth", "🪜", preposition = "on"),
                    place("Door Area", "🚪", preposition = "near"),
                ),
            ),
            groups = listOf(
                EvidenceGroup("Train classes", listOf("Sleeper", "General", "Chair Car", "AC"), 0),
                EvidenceGroup("Chai companions", listOf("Biscuit", "Rusk", "Samosa", "Pakora"), 1),
                EvidenceGroup("Fictional detectives", listOf("Byomkesh", "Feluda", "Holmes", "Poirot"), 2),
                EvidenceGroup("___line", listOf("Dead", "Time", "Head", "Punch"), 3),
            ),
            fermi = FermiQuestion(
                prompt = "The train covered about 700 km during the night. " +
                    "About how many times did one wheel of the coach turn?",
                answer = 240_000.0,
                unit = "turns",
                explanation = "A train wheel is about 0.9 m across, so one turn covers π × 0.9 ≈ 2.9 m. " +
                    "700,000 m ÷ 2.9 m ≈ 240,000 turns.",
            ),
            logicSeed = 3003L,
        ),
    )
}
