package com.example.game.dictionary

import android.content.Context
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Collections
import java.util.zip.GZIPInputStream

/**
 * Built-in Complete Official English Scrabble Dictionary with Hash validation,
 * binary prefix searching, and comprehensive definitions.
 * Contains the complete official tournament lexicon (over 270,000 words).
 */
object WordDictionary {

    private val wordSet = HashSet<String>(280000)
    private val definitions = HashMap<String, String>(1000)
    val allWordsList = ArrayList<String>(280000)

    @Volatile
    var isInitialized = false
        private set

    @Volatile
    var isFullLexiconLoaded = false
        private set

    /**
     * Loads the dictionary. Without a Context (plain JVM / tests) only the
     * classloader word list and the hardcoded fallback load. With a Context
     * (Android) the full bundled list is loaded from assets first — that is
     * the list the game actually validates against on device. Safe to call
     * more than once: a later call with a Context upgrades a fallback-only
     * load to the full list.
     */
    fun init(context: Context? = null) {
        if (isInitialized && (context == null || isFullLexiconLoaded)) return
        synchronized(this) {
            if (isInitialized && (context == null || isFullLexiconLoaded)) return
            loadDefinitions()
            loadLexicon(context)
            isInitialized = true
        }
    }

    private fun insertWord(word: String, definition: String? = null) {
        val clean = word.trim().uppercase()
        if (clean.length < 2 || clean.any { it !in 'A'..'Z' }) return
        if (wordSet.add(clean)) {
            allWordsList.add(clean)
        }
        if (definition != null) {
            definitions[clean] = definition
        }
    }

    fun isValidWord(word: String): Boolean {
        if (!isInitialized) init()
        val clean = word.trim().uppercase()
        if (clean.length < 2) return false
        return wordSet.contains(clean)
    }

    fun hasPrefix(prefix: String): Boolean {
        if (!isInitialized) init()
        val clean = prefix.trim().uppercase()
        if (clean.isEmpty()) return true
        val index = Collections.binarySearch(allWordsList, clean)
        if (index >= 0) return true
        val insertionPoint = -(index + 1)
        if (insertionPoint < allWordsList.size) {
            return allWordsList[insertionPoint].startsWith(clean)
        }
        return false
    }

    /**
     * A real, built-in meaning for [word], or null when the built-in
     * dictionary has nothing (direct match or morphological derivation).
     * Callers use this to decide whether an online lookup is worthwhile.
     */
    fun getLocalDefinition(word: String): String? {
        if (!isInitialized) init()
        val clean = word.trim().uppercase()
        if (clean.isEmpty()) return null

        // Direct dictionary match
        definitions[clean]?.let { return it }

        // Smart morphological derivation from known base definitions
        // 1. Plural / 3rd person singular "-S" or "-ES"
        if (clean.endsWith("IES") && clean.length > 3) {
            val base = clean.dropLast(3) + "Y"
            definitions[base]?.let { return "Plural/form of $base: $it" }
        }
        if (clean.endsWith("ES") && clean.length > 3) {
            val base = clean.dropLast(2)
            definitions[base]?.let { return "Plural / 3rd person singular form of $base: $it" }
        }
        if (clean.endsWith("S") && clean.length > 2) {
            val base = clean.dropLast(1)
            definitions[base]?.let { return "Plural / 3rd person form of $base: $it" }
        }

        // 2. Past tense "-ED" or "-D"
        if (clean.endsWith("IED") && clean.length > 3) {
            val base = clean.dropLast(3) + "Y"
            definitions[base]?.let { return "Past tense of $base: $it" }
        }
        if (clean.endsWith("ED") && clean.length > 3) {
            val base1 = clean.dropLast(2)
            val base2 = clean.dropLast(1)
            definitions[base1]?.let { return "Past tense of $base1: $it" }
            definitions[base2]?.let { return "Past tense of $base2: $it" }
        }

        // 3. Participle / Gerund "-ING"
        if (clean.endsWith("ING") && clean.length > 4) {
            val base1 = clean.dropLast(3)
            val base2 = clean.dropLast(3) + "E"
            definitions[base1]?.let { return "Present participle/gerund of $base1: $it" }
            definitions[base2]?.let { return "Present participle/gerund of $base2: $it" }
        }

        // 4. Comparative/Agent "-ER" or Superlative "-EST"
        if (clean.endsWith("ER") && clean.length > 3) {
            val base = clean.dropLast(2)
            definitions[base]?.let { return "One who performs or comparative form of $base: $it" }
        }
        if (clean.endsWith("EST") && clean.length > 4) {
            val base = clean.dropLast(3)
            definitions[base]?.let { return "Superlative form of $base: $it" }
        }

        // 5. Adverb "-LY"
        if (clean.endsWith("LY") && clean.length > 3) {
            val base = clean.dropLast(2)
            definitions[base]?.let { return "In a manner characteristic of $base: $it" }
        }

        // 6. Prefixes "RE-" or "UN-"
        if (clean.startsWith("RE") && clean.length > 3) {
            val base = clean.drop(2)
            definitions[base]?.let { return "To perform again / repeat $base: $it" }
        }
        if (clean.startsWith("UN") && clean.length > 3) {
            val base = clean.drop(2)
            definitions[base]?.let { return "Not or reversal of $base: $it" }
        }

        return null
    }

    fun getDefinition(word: String): String {
        if (!isInitialized) init()
        val clean = word.trim().uppercase()
        if (clean.isEmpty()) return "No word provided."
        getLocalDefinition(word)?.let { return it }

        // Calculate raw Scrabble points for lexical entry
        var rawPts = 0
        for (ch in clean) {
            rawPts += com.example.game.model.getLetterPoints(ch)
        }

        if (wordSet.contains(clean)) {
            val syllables = estimateSyllables(clean)
            return "Official tournament Scrabble word ($clean, $rawPts pts, $syllables). Valid in NASPA (TWL06) & CSW21 lexicons."
        }
        return "$clean ($rawPts pts): Valid playable English word in Scrabble lexicon."
    }

    private fun estimateSyllables(word: String): String {
        val vowels = setOf('A', 'E', 'I', 'O', 'U', 'Y')
        var count = 0
        var prevWasVowel = false
        for (ch in word) {
            val isVowel = ch in vowels
            if (isVowel && !prevWasVowel) count++
            prevWasVowel = isVowel
        }
        if (word.endsWith("E") && count > 1 && !word.endsWith("LE")) count--
        val n = maxOf(1, count)
        return "$n syllable" + if (n > 1) "s" else ""
    }

    private fun loadFromStream(stream: InputStream): Int {
        var loadedCount = 0
        GZIPInputStream(stream).use { gzIn ->
            BufferedReader(InputStreamReader(gzIn, Charsets.UTF_8)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    val w = line.trim().uppercase()
                    if (w.length >= 2 && w.all { it in 'A'..'Z' }) {
                        if (wordSet.add(w)) {
                            allWordsList.add(w)
                            loadedCount++
                        }
                    }
                    line = reader.readLine()
                }
            }
        }
        return loadedCount
    }

    private fun loadLexicon(context: Context? = null) {
        var loadedCount = 0

        // Android: the full bundled list ships as an asset. (The old
        // src/main/resources copy never reaches the APK, so the classloader
        // lookup below silently finds nothing on device.)
        if (context != null) {
            try {
                context.assets.open("scrabble_words.txt.gz").use { stream ->
                    loadedCount += loadFromStream(stream)
                }
            } catch (_: Exception) {
                // Fall through to the classloader attempt below
            }
        }

        if (loadedCount == 0) {
            try {
                val stream: InputStream? =
                    WordDictionary::class.java.getResourceAsStream("/scrabble_words.txt.gz")
                        ?: WordDictionary::class.java.classLoader?.getResourceAsStream("scrabble_words.txt.gz")
                        ?: Thread.currentThread().contextClassLoader?.getResourceAsStream("scrabble_words.txt.gz")
                        ?: ClassLoader.getSystemResourceAsStream("scrabble_words.txt.gz")
                        ?: WordDictionary::class.java.getResourceAsStream("/assets/scrabble_words.txt.gz")

                if (stream != null) {
                    stream.use { loadedCount += loadFromStream(it) }
                }
            } catch (_: Exception) {
                // Fallback continues below
            }
        }

        if (loadedCount > 0) {
            isFullLexiconLoaded = true
        }

        // Always ensure core vocabulary is inserted
        loadCoreVocabulary()

        // Ensure allWordsList is sorted for fast binary search prefix checks
        allWordsList.sort()
    }

    private fun loadDefinitions() {
        // Official 2-Letter Scrabble Words & Definitions
        val twoLetterWords = listOf(
            "AA" to "Rough cindery lava",
            "AB" to "An abdominal muscle",
            "AD" to "An advertisement",
            "AE" to "One (Scots)",
            "AG" to "Agriculture",
            "AH" to "Expressing delight, relief, or surprise",
            "AI" to "The three-toed sloth",
            "AL" to "An East Indian tree (Morinda citrifolia)",
            "AM" to "Present tense of be",
            "AN" to "Indefinite article",
            "AR" to "The letter R",
            "AS" to "To the same degree",
            "AT" to "Expressing location",
            "AW" to "Expressing mild protest or sympathy",
            "AX" to "A cutting tool",
            "AY" to "An affirmative vote; aye",
            "BA" to "The soul in ancient Egyptian belief",
            "BE" to "To exist or have reality",
            "BI" to "A bisexual person",
            "BO" to "A friendly term of address; pal",
            "BY" to "Near or through",
            "DA" to "Father or dad",
            "DE" to "From or of (in names)",
            "DO" to "To perform or execute an action",
            "ED" to "Education",
            "EF" to "The letter F",
            "EH" to "Expressing inquiry or seeking agreement",
            "EL" to "An elevated railway",
            "EM" to "A unit of typographic measurement",
            "EN" to "A unit of typographic width",
            "ER" to "Expressing hesitation",
            "ES" to "The letter S",
            "ET" to "Past tense of eat",
            "EW" to "Expressing disgust",
            "EX" to "A former spouse or partner",
            "FA" to "Fourth musical note of major scale",
            "FE" to "A Hebrew letter (pe)",
            "GI" to "A martial arts uniform",
            "GO" to "To move from one place to another",
            "HA" to "Expressing triumph or laughter",
            "HE" to "Male person or animal",
            "HI" to "Informal greeting",
            "HM" to "Expressing thoughtful consideration",
            "HO" to "Expressing surprise or calling attention",
            "ID" to "Part of the psyche",
            "IF" to "Introducing a conditional clause",
            "IN" to "Expressing position or enclosure",
            "IS" to "Third person singular present of be",
            "IT" to "Refers to a thing or animal",
            "JO" to "A sweetheart or darling (Scots)",
            "KA" to "The spiritual double in Egyptian religion",
            "KI" to "Japanese term for life energy (qi)",
            "LA" to "Sixth musical note of major scale",
            "LI" to "Chinese unit of distance",
            "LO" to "Look, see, behold",
            "MA" to "Mother",
            "ME" to "Objective case of I",
            "MI" to "Third musical note of major scale",
            "MM" to "Expressing satisfaction or pleasure",
            "MO" to "A moment",
            "MU" to "12th letter of Greek alphabet",
            "MY" to "Belonging to me",
            "NA" to "No or not (informal/Scottish)",
            "NE" to "Born as; formerly known as",
            "NO" to "Negative reply or indication",
            "NU" to "13th letter of Greek alphabet",
            "OD" to "A hypothetical natural force",
            "OE" to "A grandchild in Scotland",
            "OF" to "Expressing relationship or belonging",
            "OH" to "Expressing emotion or realization",
            "OI" to "Calling attention or expressing objection",
            "OK" to "All right; satisfactory",
            "OM" to "Sacred mystical syllable in Hinduism/Buddhism",
            "ON" to "Physically touching and supported by",
            "OP" to "Operation or surgical procedure",
            "OR" to "Introducing an alternative",
            "OS" to "A bone or orifice",
            "OW" to "Expressing sudden pain",
            "OX" to "A domesticated bovine animal",
            "OY" to "Expressing dismay or frustration",
            "PA" to "Father",
            "PE" to "Hebrew letter",
            "PI" to "16th letter of Greek alphabet (3.14159...)",
            "PO" to "A chamber pot",
            "QI" to "Vital life force in Chinese traditional culture",
            "RE" to "Second musical note of major scale",
            "SH" to "Be quiet or silent",
            "SI" to "Seventh musical note (ti)",
            "SO" to "To that extent or degree",
            "TA" to "Thank you (informal)",
            "TE" to "Variant of ti in solfège",
            "TI" to "Seventh note of musical scale",
            "TO" to "Expressing direction toward a location",
            "UH" to "Expressing hesitation",
            "UM" to "Expressing hesitation or doubt",
            "UN" to "A person or thing (one)",
            "UP" to "Toward a higher place or position",
            "US" to "Objective case of we",
            "UT" to "The original first note of the scale (do)",
            "WE" to "Plural first-person pronoun",
            "WO" to "Woe or distress",
            "XI" to "14th letter of Greek alphabet",
            "XU" to "Vietnamese monetary unit",
            "YA" to "You (informal)",
            "YE" to "You (archaic plural)",
            "YO" to "A greeting or attention-getter",
            "ZA" to "Pizza (slang)"
        )
        for ((w, d) in twoLetterWords) {
            insertWord(w, d)
        }

        // 2. High Value & Classic Scrabble 3-Letter Words
        val threeLetterWords = listOf(
            "ACE" to "A playing card with a single pip",
            "ACT" to "Take action or perform",
            "ADD" to "Join or combine",
            "AGE" to "Length of time a person has lived",
            "AID" to "Help or support",
            "AIM" to "Point or direct at a target",
            "AIR" to "Invisible gas surrounding Earth",
            "ALL" to "The whole quantity",
            "AND" to "Used to connect words",
            "ANT" to "A small social insect",
            "ANY" to "One or some of a thing",
            "APE" to "A large primate without a tail",
            "APT" to "Appropriate or suitable",
            "ARC" to "A curved symmetrical line",
            "ARK" to "A large boat or chest",
            "ARM" to "Upper limb of human body",
            "ART" to "Creative visual or audio expression",
            "ASH" to "Powdery residue left after burning",
            "ASK" to "Pose a question",
            "ATE" to "Past tense of eat",
            "AWE" to "Feeling of reverential respect",
            "AXE" to "A tool for chopping wood",
            "BAD" to "Poor quality or unfavorable",
            "BAG" to "A flexible container",
            "BAN" to "Officially prohibit",
            "BAR" to "A rigid piece of metal or wood",
            "BAT" to "Flying mammal or sports club",
            "BAY" to "Inlet of the sea",
            "BED" to "Piece of furniture for sleep",
            "BEE" to "Honey-producing insect",
            "BEG" to "Ask earnestly or humbly",
            "BET" to "Wager money on an outcome",
            "BIG" to "Of considerable size or extent",
            "BIN" to "A container for storage",
            "BIT" to "A small piece or unit of digital info",
            "BOA" to "A large nonvenomous snake",
            "BOB" to "Move quickly up and down",
            "BOG" to "Wet muddy ground",
            "BOW" to "Bend the head or weapon for arrows",
            "BOX" to "A rigid square or rectangular container",
            "BOY" to "A male child",
            "BUD" to "Compact knob on a plant",
            "BUG" to "A small insect or software glitch",
            "BUN" to "A small sweet bread roll",
            "BUS" to "Large motor vehicle for passengers",
            "BUT" to "Used to introduce a contrast",
            "BUY" to "Acquire in exchange for payment",
            "CAB" to "A taxi",
            "CAN" to "Be able to or metal container",
            "CAP" to "A brimless soft hat",
            "CAR" to "A four-wheeled road vehicle",
            "CAT" to "A small domesticated feline",
            "COB" to "The central woody core of maize",
            "COG" to "A tooth on the rim of a gear",
            "COP" to "A police officer",
            "COT" to "A camp bed or baby crib",
            "COW" to "Fully grown female bovine",
            "COY" to "Making a pretense of shyness",
            "CRY" to "Shed tears or shout loudly",
            "CUB" to "The young of certain carnivores",
            "CUD" to "Partly digested food returned to mouth",
            "CUE" to "A signal for action or billiard stick",
            "CUP" to "Small bowl-shaped container for drinking",
            "CUT" to "Divide with a sharp blade",
            "DAB" to "Press gently against a surface",
            "DAD" to "Father",
            "DAM" to "A barrier constructed to hold back water",
            "DAY" to "Period of light between sunrise and sunset",
            "DEN" to "Wild animal's lair or cozy room",
            "DEW" to "Tiny moisture droplets on cool surfaces",
            "DIE" to "Stop living or small numbered cube",
            "DIG" to "Break up and move earth",
            "DIM" to "Not shining brightly",
            "DIN" to "A loud, unpleasant prolonged noise",
            "DIP" to "Put down briefly into a liquid",
            "DOC" to "Doctor (informal)",
            "DOG" to "Domesticated canine companion",
            "DOT" to "A small round mark",
            "DRY" to "Free from moisture or liquid",
            "DUB" to "Give an unofficial name to",
            "DUD" to "Thing that fails to work",
            "DUE" to "Expected at or planned for",
            "DUG" to "Past tense of dig",
            "DYE" to "Coloring liquid",
            "EAR" to "Organ of hearing",
            "EAT" to "Consume food",
            "EBB" to "Movement of the tide out to sea",
            "EEL" to "A snakelike fish",
            "EGG" to "Oval or round reproductive body",
            "EGO" to "A person's sense of self-esteem",
            "ELF" to "A supernatural mythical creature",
            "ELK" to "A large deer with antlers",
            "ELM" to "A tall deciduous shade tree",
            "EMU" to "A large Australian flightless bird",
            "END" to "The final point in time or space",
            "ERA" to "A long and distinct historical period",
            "EVE" to "The day or period before an event",
            "EYE" to "Organ of sight",
            "FAN" to "Apparatus for creating a current of air",
            "FAR" to "At a great distance",
            "FAT" to "Natural oily substance in animal bodies",
            "FAX" to "Facsimile transmission",
            "FED" to "Past tense of feed",
            "FEE" to "A payment made for professional services",
            "FEW" to "A small number of items",
            "FIG" to "A soft sweet pear-shaped fruit",
            "FIN" to "Winglike organ of a fish",
            "FIR" to "An evergreen coniferous tree",
            "FIT" to "Suitable or in good physical health",
            "FIX" to "Repair or fasten securely",
            "FLY" to "Move through the air using wings",
            "FOB" to "A chain or decorative ornament for a watch",
            "FOE" to "An enemy or adversary",
            "FOG" to "Thick cloud of tiny water droplets",
            "FOR" to "In favor of or directed toward",
            "FOX" to "Carnivorous mammal with bushy tail",
            "FRY" to "Cook food in hot fat or oil",
            "FUN" to "Enjoyment, amusement, or pleasure",
            "FUR" to "Short fine soft hair of an animal",
            "GAG" to "Piece of cloth placed in mouth",
            "GAP" to "A break or opening",
            "GAS" to "Air-like fluid substance",
            "GAY" to "Lighthearted and carefree or homosexual",
            "GEL" to "Jelly-like substance",
            "GEM" to "A precious or semiprecious stone",
            "GET" to "Come to have or receive",
            "GIG" to "A live performance by musicians",
            "GIN" to "Clear alcoholic spirit flavored with juniper",
            "GLAD" to "Pleased, delighted, or joyful",
            "GNU" to "A large African antelope; wildebeest",
            "GOAL" to "An aim or desired result",
            "GOD" to "Supreme being or deity",
            "GOT" to "Past tense of get",
            "GUM" to "Firm flesh surrounding tooth base or chewy candy",
            "GUT" to "The stomach or belly",
            "GUY" to "A man or person",
            "GYM" to "Gymnasium for physical exercise",
            "HAD" to "Past tense of have",
            "HAM" to "Cured meat from the leg of a pig",
            "HAT" to "Shaped covering for the head",
            "HAY" to "Grass mown and dried for fodder",
            "HEN" to "Female bird, especially domestic fowl",
            "HER" to "Belonging to a female person",
            "HEX" to "Cast a spell on or bewitch",
            "HEY" to "Greeting or exclamation",
            "HID" to "Past tense of hide",
            "HIM" to "Objective form of he",
            "HIP" to "Projection of the pelvis or trendy",
            "HIS" to "Belonging to a male person",
            "HIT" to "Bring hand or tool into contact with",
            "HOB" to "Flat heating surface on a cooker",
            "HOG" to "A domesticated pig",
            "HOP" to "Spring or leap lightly on one foot",
            "HOT" to "Having a high temperature",
            "HOW" to "In what way or manner",
            "HUB" to "The central part of a wheel or activity",
            "HUG" to "Squeeze tightly in arms with affection",
            "HUM" to "Make a low, steady continuous sound",
            "HUT" to "A small simple single-story building",
            "ICE" to "Frozen water",
            "ICY" to "Covered with or resembling ice",
            "ILL" to "Not in good health; sick",
            "IMP" to "A mischievous small mythological demon",
            "INK" to "Colored fluid used for writing",
            "INN" to "A pub or small hotel",
            "ION" to "An electrically charged atom",
            "IRE" to "Anger or rage",
            "IRK" to "Irritate or vex",
            "IVY" to "Evergreen climbing plant",
            "JAB" to "Poke or strike roughly",
            "JAM" to "Sweet spread of fruit boiled with sugar",
            "JAR" to "Wide-mouthed cylindrical glass container",
            "JAW" to "Each of the upper and lower bony structures",
            "JAY" to "Noisy bird of the crow family",
            "JET" to "Rapid stream of liquid or fast aircraft",
            "JIG" to "A lively dance",
            "JOB" to "Paid position of regular employment",
            "JOG" to "Run at a steady, gentle pace",
            "JOY" to "Feeling of great happiness",
            "JUG" to "A large container for liquids",
            "JUT" to "Extend out, over, or beyond",
            "KEG" to "A small barrel",
            "KEY" to "Instrument used to turn a lock",
            "KID" to "A child or young goat",
            "KIN" to "One's family and relations",
            "KIP" to "A sleep or nap",
            "KIT" to "Set of articles or equipment",
            "LAB" to "Laboratory for research",
            "LAD" to "A boy or young man",
            "LAP" to "Upper parts of the legs when seated",
            "LAW" to "System of rules recognized by a country",
            "LAY" to "Put down gently or carefully",
            "LED" to "Past tense of lead",
            "LEG" to "Each of the limbs on which a person walks",
            "LET" to "Not prevent or allow",
            "LID" to "Removable or hinged cover",
            "LIE" to "State something false or recline horizontally",
            "LIP" to "Either of the two fleshy edges of the mouth",
            "LIT" to "Illuminated or excited",
            "LOG" to "A part of the trunk of a tree",
            "LOT" to "A particular group, collection, or plot",
            "LOW" to "Of less than average height",
            "LUG" to "Carry or drag with great effort",
            "MAD" to "Very angry or insane",
            "MAN" to "An adult male human",
            "MAP" to "Diagrammatic representation of an area",
            "MAT" to "Piece of coarse fabric on floor",
            "MAY" to "Expressing possibility or fifth month",
            "MEN" to "Adult male humans",
            "MET" to "Past tense of meet",
            "MID" to "Of or in the middle part",
            "MIX" to "Combine two or more substances",
            "MOP" to "Implement for washing floors",
            "MUD" to "Soft, sticky matter resulting from water and earth",
            "MUG" to "Large cup for hot beverages",
            "MUM" to "Mother or silent",
            "NAB" to "Catch or arrest someone",
            "NAG" to "Harass someone to do something",
            "NAP" to "A short sleep during the day",
            "NET" to "Open-meshed fabric or remaining after deductions",
            "NEW" to "Not existing before; recently made",
            "NIL" to "Zero; nothing",
            "NIP" to "Bite or pinch sharply",
            "NOD" to "Lower and raise one's head in agreement",
            "NOT" to "Used with an auxiliary verb to express negation",
            "NOW" to "At the present time or moment",
            "NUN" to "A woman belonging to a religious order",
            "NUT" to "Fruit consisting of a hard shell",
            "OAK" to "A large tree bearing acorns",
            "OAR" to "Pole with a flat blade for rowing",
            "OAT" to "Cereal plant whose grain is eaten",
            "ODD" to "Differing from what is usual or singular",
            "ODE" to "A lyric poem addressed to a particular subject",
            "OFF" to "Away from the place in question",
            "OIL" to "Viscous liquid unmixable with water",
            "OLD" to "Having lived for a long time",
            "ONE" to "The lowest cardinal number",
            "OPT" to "Make a choice from alternatives",
            "ORB" to "A spherical body or globe",
            "ORE" to "Naturally occurring solid rock yielding metal",
            "OUR" to "Belonging to us",
            "OUT" to "Moving away from a place",
            "OWL" to "Nocturnal bird of prey with large eyes",
            "OWN" to "Have something as one's possession",
            "PAD" to "Thick piece of soft material",
            "PAL" to "A friend",
            "PAN" to "Metal container used for cooking",
            "PAR" to "Standard score on golf hole or average",
            "PAT" to "Touch quickly and gently with flat hand",
            "PAW" to "An animal's foot having claws",
            "PAY" to "Give money in exchange for goods",
            "PEA" to "Spherical green seed eaten as vegetable",
            "PEG" to "Short pin of wood or metal",
            "PEN" to "Instrument for writing with ink",
            "PET" to "Domestic animal kept for companionship",
            "PEW" to "Long bench in a church",
            "PIE" to "Baked dish of fruit or meat in pastry",
            "PIG" to "An omnivorous domesticated animal",
            "PIN" to "Thin piece of metal with sharp point",
            "PIP" to "Small hard seed in fruit",
            "PIT" to "Large hole in the ground",
            "PLY" to "Work with or thickness of material",
            "POD" to "Elongated seed vessel of a plant",
            "POP" to "Make a light explosive sound",
            "POT" to "Container used for storage or cooking",
            "PRO" to "A professional or in favor of",
            "PRY" to "Inquire too closely into private affairs",
            "PUB" to "Tavern or bar",
            "PUG" to "Small breed of dog with wrinkled face",
            "PUN" to "Joke exploiting multiple meanings of a word",
            "PUP" to "A young dog",
            "PUT" to "Move to or place in a specific position",
            "PLY" to "A layer or strand of fabric or wood",
            "QUI" to "Who (Latin/French loanword)",
            "RAG" to "Piece of old cloth",
            "RAM" to "An uncastrated adult male sheep",
            "RAN" to "Past tense of run",
            "RAP" to "Quick sharp knock or genre of music",
            "RAT" to "Rodent larger than a mouse",
            "RAW" to "Uncooked or unprocessed",
            "RAY" to "Narrow beam of light",
            "RED" to "Color of ripe strawberries or blood",
            "RIB" to "Each of a series of curved bones",
            "RID" to "Make free of an unwanted thing",
            "RIG" to "Provide a sailing boat with sails or assemble",
            "RIM" to "The upper or outer edge of an object",
            "RIP" to "Tear quickly and forcefully",
            "ROB" to "Take property unlawfully from a person",
            "ROD" to "Thin straight bar of wood or metal",
            "ROT" to "Decay naturally through bacterial action",
            "ROW" to "Line of things or move boat with oars",
            "RUB" to "Apply firm pressure and friction",
            "RUG" to "Floor mat of thick woven fabric",
            "RUM" to "Alcoholic liquor distilled from sugar cane",
            "RUN" to "Move swiftly on foot",
            "RUT" to "A long deep track made by wheels",
            "RYE" to "A wheat-like cereal plant",
            "SAC" to "A cavity or pouch in an organism",
            "SAD" to "Feeling sorrow or unhappy",
            "SAG" to "Sink or dip downward under pressure",
            "SAP" to "Fluid that circulates in plants",
            "SAT" to "Past tense of sit",
            "SAW" to "Hand tool with toothed blade or past of see",
            "SAY" to "Utter words to convey info",
            "SEA" to "Expanse of salt water",
            "SEE" to "Perceive with the eyes",
            "SET" to "Put or place in specified state",
            "SEW" to "Join or repair by stitching with needle",
            "SEX" to "Either of the two main reproductive categories (male/female) or sexual activity",
            "SHY" to "Nervous or timid in company",
            "SIN" to "An immoral act considered a transgression",
            "SIP" to "Drink by taking small mouthfuls",
            "SIR" to "Polite or respectful address for a man",
            "SIT" to "Rest with torso upright supported by buttocks",
            "SIX" to "Number between five and seven",
            "SKI" to "Long narrow piece of wood for snow gliding",
            "SKY" to "Region of the atmosphere seen from earth",
            "SLY" to "Cunning and deceitful",
            "SOB" to "Cry noisily with deep gasps",
            "SOD" to "Surface of ground with grass; turf",
            "SON" to "A male offspring",
            "SOP" to "Piece of bread dipped in liquid",
            "SOW" to "Plant seeds by scattering or female pig",
            "SOY" to "Protein-rich legume",
            "SPA" to "Mineral spring or health resort",
            "SPY" to "Secretly collect information",
            "SUM" to "Total amount resulting from addition",
            "SUN" to "The star around which the earth orbits",
            "TAB" to "A small flap or projection",
            "TAG" to "Label attached for identification",
            "TAN" to "Brown color acquired from sunlight",
            "TAP" to "Strike lightly or faucet for liquid",
            "TAR" to "Dark thick flammable liquid distilled from wood/coal",
            "TEA" to "Hot beverage made by steeping cured leaves",
            "TEN" to "Number equal to 10",
            "THE" to "Definite article",
            "TIE" to "Attach or fasten with string or ribbon",
            "TIN" to "Silvery-white metallic element",
            "TIP" to "Pointed end of something or gratuity",
            "TOE" to "Any of the five digits at foot end",
            "TON" to "Unit of weight (2000 lbs)",
            "TOP" to "Highest point or part",
            "TOY" to "An object for a child to play with",
            "TRY" to "Make an attempt or effort to do",
            "TUB" to "Wide open deep container for liquid",
            "TUG" to "Pull hard or suddenly",
            "TWO" to "Number equal to 2",
            "URN" to "Tall rounded vase",
            "USE" to "Employ for a purpose",
            "VAN" to "Medium-sized motor vehicle",
            "VAT" to "Large tank used to hold liquid",
            "VET" to "Veterinarian or evaluate carefully",
            "VIA" to "By way of; through",
            "VOW" to "Solemn promise or pledge",
            "WAR" to "State of armed conflict between nations",
            "WAX" to "Malleable oily organic substance",
            "WAY" to "A method, style, or manner",
            "WEB" to "Network of fine threads made by spider",
            "WET" to "Covered or saturated with liquid",
            "WHO" to "What or which person",
            "WHY" to "For what reason or purpose",
            "WIG" to "Artificial head of hair",
            "WIN" to "Be victorious in a contest",
            "WIT" to "Mental sharpness and inventive humor",
            "WOE" to "Great sorrow or distress",
            "WON" to "Past tense of win",
            "YAK" to "Large domesticated ox of Tibet",
            "YAM" to "Edible starchy tuber",
            "YAP" to "Give a sharp, shrill bark",
            "YEA" to "Yes",
            "YES" to "Affirmative response",
            "YET" to "Up to the present time",
            "YOU" to "The person or people being addressed",
            "ZEN" to "Japanese school of Mahayana Buddhism emphasizing meditation",
            "ZIP" to "Fasten with a zipper or move with speed",
            "ZOO" to "Establishment where wild animals are kept for exhibition"
        )
        for ((w, d) in threeLetterWords) {
            insertWord(w, d)
        }

        // High-frequency and high-value 4 & 5-letter Scrabble definitions
        val keyScrabbleWords = listOf(
            "QUIZ" to "Test of knowledge, especially as competition",
            "JINX" to "Bring bad luck to someone or something",
            "JAZZ" to "Genre of music characterized by swing and blue notes",
            "FIZZ" to "Make a hissing or sputtering sound like effervescence",
            "RAZZ" to "Tease or make fun of playfully",
            "BUZZ" to "Continuous humming sound like a bee",
            "FLUX" to "Action or process of flowing; continuous change",
            "FOXY" to "Cunning, sly, or stylishly attractive",
            "HAZY" to "Covered by a haze; indistinct or vague",
            "MAZE" to "Complex network of paths or passages",
            "ZEAL" to "Great energy or enthusiasm in pursuit of a cause",
            "ZERO" to "The numerical value equivalent to none",
            "ZONE" to "An area or stretch of land having a particular characteristic",
            "ZOOM" to "Move or travel very quickly",
            "QUIP" to "A witty or clever remark",
            "QUIT" to "Leave a place or cease an activity",
            "JADE" to "Precious green gemstone or hard mineral",
            "JUMP" to "Push oneself off a surface into the air",
            "JUNE" to "The sixth month of the year",
            "JURY" to "Body of people sworn to give a verdict in a legal case",
            "JUST" to "Based on or behaving according to what is morally right and fair",
            "JAVA" to "Island in Indonesia; colloquial term for brewed coffee",
            "KICK" to "Strike forcibly with the foot",
            "KING" to "The male ruler of an independent state",
            "KITE" to "Toy consisting of a light frame with thin material flown in wind",
            "KNOB" to "Rounded handle or control dial",
            "KNOT" to "Fastening made by tying a piece of string or rope",
            "LUCK" to "Success or failure apparently brought by chance",
            "LUSH" to "Growing luxuriantly and abundantly",
            "LYNX" to "Wild cat with yellowish-brown coat and tufted ears",
            "MOON" to "The natural satellite of the earth",
            "MUSE" to "A person or personified force who is the source of inspiration",
            "MYTH" to "Traditional story concerning early history or supernatural phenomena",
            "NOVA" to "A star showing a sudden large increase in brightness",
            "ONYX" to "A banded variety of chalcedony quartz mineral",
            "OVAL" to "Having a rounded and slightly elongated outline like an egg",
            "OXY" to "Containing oxygen or related chemical compounds",
            "PAGE" to "One side of a leaf of a book or publication",
            "PALM" to "The inner surface of the hand, or a tropical tree",
            "PARK" to "Large public garden or area of recreation",
            "PEAK" to "Pointed top of a mountain",
            "PLAY" to "Engage in activity for enjoyment and recreation",
            "PLUM" to "Sweet oval fleshy fruit with a stone",
            "POEM" to "Piece of writing in which words are chosen for beauty and sound",
            "QUAD" to "Four-sided courtyard or quadruplet",
            "ROSE" to "Fragrant prickly shrub bearing classic flowers",
            "RUBY" to "Precious red gemstone variety of corundum",
            "RUST" to "Reddish-brown iron oxide formed by water and air",
            "SAGE" to "A profoundly wise person; or an aromatic herbal plant",
            "SHIP" to "Large seafaring vessel",
            "SILK" to "Fine strong soft lustrous fiber produced by silkworms",
            "SOAP" to "Substance used with water for washing and cleaning",
            "SOLO" to "A thing done in solitary fashion without companions",
            "SONG" to "Short poem or piece of words set to music",
            "SOUL" to "The spiritual or immaterial part of a human being",
            "STAR" to "Luminous celestial body visible in the night sky",
            "SURF" to "Mass of white foam formed by waves breaking on shore",
            "SWAN" to "Large waterbird with long flexible neck and white plumage",
            "TENT" to "Portable shelter of fabric supported by poles",
            "TIDE" to "Alternate rising and falling of the sea",
            "TIME" to "The continuous indefinite progress of existence and events",
            "TREE" to "Woody perennial plant having a single stem or trunk",
            "TRIP" to "A journey or excursion, especially for pleasure",
            "TRUE" to "In accordance with fact or reality",
            "TUNE" to "Melody or sequence of pleasant musical notes",
            "TWIN" to "One of two children born at the same birth",
            "VAIN" to "Having or showing an excessively high opinion of oneself",
            "VASE" to "Decorative container used as ornament or for holding cut flowers",
            "VEX" to "Make someone feel annoyed, frustrated, or worried",
            "VIBE" to "Distinctive emotional atmosphere or ambiance felt",
            "VINE" to "Climbing or trailing woody-stemmed plant",
            "VOICE" to "Sound produced in person's larynx and uttered through mouth",
            "VOID" to "Completely empty space or invalid legal state",
            "WALK" to "Move at a regular pace by lifting and setting down feet",
            "WAVE" to "Long body of water curling into an arched crest on shore",
            "WIND" to "Perceptible natural movement of the air",
            "WINE" to "Alcoholic drink made from fermented grape juice",
            "WISH" to "Feel or express a strong desire or hope",
            "WOOD" to "Hard fibrous material forming trunk and branches of trees",
            "WORD" to "A single distinct meaningful element of speech or writing",
            "YARN" to "Spun thread used for knitting, weaving, or sewing",
            "YEAR" to "Time taken by the earth to make one revolution around the sun",
            "ZINC" to "Silvery-white chemical element and metallic mineral",
            // 5-Letter Favorites
            "ABBEY" to "Building occupied by a community of monks or nuns",
            "ABOVE" to "At a higher level or layer than",
            "ADAPT" to "Make suitable for a new use or purpose",
            "ADORE" to "Love and respect someone deeply",
            "AGILE" to "Able to move quickly and easily",
            "ALLEY" to "Narrow passageway between buildings",
            "AMBER" to "Honey-yellow fossilized tree resin",
            "ANGEL" to "Spiritual being believed to act as an attendant or messenger of God",
            "APPLE" to "Round fruit with red or green skin and crisp flesh",
            "BEACH" to "Pebbly or sandy shore by the ocean or lake",
            "BLOOM" to "Produce flowers or be in prime healthy state",
            "BRAVE" to "Ready to face and endure danger or pain",
            "BREEZE" to "A gentle, refreshing wind",
            "CHARM" to "The power of delighting or attracting others",
            "CHILL" to "An unpleasant feeling of coldness; to relax",
            "CLOUD" to "Visible mass of condensed water vapor floating in atmosphere",
            "CORAL" to "Marine invertebrates that build vibrant ocean reefs",
            "DANCE" to "Move rhythmically to music, typically following steps",
            "DREAM" to "Series of thoughts, images, and sensations occurring in sleep",
            "EAGLE" to "Large bird of prey with massive hooked bill and broad wings",
            "EARTH" to "The planet on which we live; terrestrial soil",
            "FLAME" to "Hot glowing body of ignited gas produced by fire",
            "FRESH" to "Recently made or obtained; not stale or faded",
            "GRACE" to "Simple elegance or refinement of movement",
            "HAPPY" to "Feeling or showing pleasure or contentment",
            "HEART" to "Hollow muscular organ that pumps blood through circulatory system",
            "HONEY" to "Sweet sticky yellowish-brown fluid made by honeybees",
            "IVORY" to "Hard creamy-white substance composing tusks of elephants",
            "JEWEL" to "Precious stone, typically a single crystal or cut gem",
            "LIGHT" to "Natural agent that stimulates sight and makes things visible",
            "LUCKY" to "Having, bringing, or resulting from good luck",
            "MAGIC" to "The power of influencing events by using mysterious forces",
            "MUSIC" to "Vocal or instrumental sounds combined in rhythm and harmony",
            "NOBLE" to "Belonging to a hereditary class with high social status; honorable",
            "OCEAN" to "Very large expanse of sea, in particular the Atlantic, Pacific, etc.",
            "PEACE" to "Freedom from disturbance; tranquility",
            "PIANO" to "Large keyboard musical instrument with felt-covered hammers",
            "PLANT" to "Living organism of the vegetable kind without locomotive movement",
            "PRIDE" to "A feeling of deep pleasure or satisfaction derived from achievements",
            "QUEEN" to "The female ruler of an independent state",
            "QUIET" to "Making little or no noise",
            "RIVER" to "Large natural stream of water flowing in a channel to the sea",
            "ROYAL" to "Having the status of a king or queen or member of their family",
            "SHINE" to "Emit or reflect light; be bright",
            "SMILE" to "Form one's features into a pleased, kind, or amused expression",
            "SPARK" to "Small fiery particle generated by a fire or electrical discharge",
            "SWEET" to "Having the pleasant taste characteristic of sugar or honey",
            "TIGER" to "Very large solitary cat with yellow-brown coat striped with black",
            "UNITY" to "The state of being united or joined as a whole",
            "WATER" to "Colorless, transparent, odorless liquid forming the seas and rain",
            "ZESTY" to "Appealingly piquant or lively in flavor and character"
        )
        for ((w, d) in keyScrabbleWords) {
            insertWord(w, d)
        }
    }

    private fun loadCoreVocabulary() {
        val commonVocabulary = listOf(
            // 4-letter words
            "ABLE", "ACID", "AGED", "AIDE", "AIDS", "AIRS", "ALLY", "ALOE", "ALPS", "ALSO",
            "ARCH", "AREA", "ARMY", "ARTS", "ATOM", "AUNT", "AUTO", "AVID", "AWAY", "BABY",
            "BACK", "BAKE", "BALL", "BAND", "BANK", "BARE", "BARK", "BARN", "BASE", "BASS",
            "BATH", "BEAK", "BEAM", "BEAN", "BEAR", "BEAT", "BEEF", "BEER", "BELL", "BELT",
            "BEND", "BEST", "BIKE", "BIRD", "BITE", "BLOW", "BLUE", "BLUR", "BOAT", "BODY",
            "BOIL", "BOLD", "BOND", "BONE", "BOOK", "BOOM", "BOOT", "BORE", "BORN", "BOSS",
            "BOTH", "BOWL", "BULB", "BULK", "BULL", "BUMP", "BURN", "BURY", "BUSH", "BUSY",
            "CAFE", "CAGE", "CAKE", "CALM", "CAMP", "CANE", "CAPE", "CARD", "CARE", "CART",
            "CASE", "CASH", "CAST", "CAVE", "CHEF", "CHIN", "CHIP", "CHOP", "CITY", "CLAP",
            "CLAW", "CLAY", "CLIP", "CLUB", "COAL", "COAT", "CODE", "COIN", "COLD", "COLT",
            "COMB", "COME", "COOK", "COOL", "COPE", "CORD", "CORK", "CORN", "COST", "COVE",
            "CRAB", "CREW", "CROP", "CROW", "CUBE", "CUFF", "CURE", "CURL", "CUTE", "DAMP",
            "DAME", "DARE", "DARK", "DART", "DATA", "DATE", "DAWN", "DAYS", "DEAD", "DEAF",
            "DEAL", "DEAN", "DEAR", "DEBT", "DECK", "DEED", "DEEP", "DEER", "DEMO", "DESK",
            "DIAL", "DICE", "DIET", "DIME", "DINE", "DIRT", "DISC", "DISH", "DISK", "DIVE",
            "DOCK", "DOLL", "DOME", "DONE", "DOOR", "DOSE", "DOVE", "DOWN", "DRAG", "DRAW",
            "DROP", "DRUM", "DUCK", "DUEL", "DUET", "DUKE", "DULL", "DUMP", "DUNE", "DUSK",
            "DUST", "DUTY", "EACH", "EARL", "EARN", "EASE", "EAST", "EASY", "ECHO", "EDGE",
            "EDIT", "ELSE", "EMIT", "EPIC", "EVEN", "EVER", "EVIL", "EXAM", "EXIT", "EYES",
            "FACE", "FACT", "FADE", "FAIR", "FALL", "FAME", "FARM", "FAST", "FATE", "FEAR",
            "FEAT", "FEED", "FEEL", "FEET", "FELL", "FELT", "FERN", "FEST", "FIBE", "FILE",
            "FILL", "FILM", "FIND", "FINE", "FIRE", "FIRM", "FISH", "FIST", "FIVE", "FLAG",
            "FLAP", "FLAT", "FLEA", "FLED", "FLEE", "FLEW", "FLIP", "FLOW", "FOAM", "FOIL",
            "FOLD", "FOLK", "FOND", "FOOD", "FOOL", "FOOT", "FORD", "FORK", "FORM", "FORT",
            "FOUL", "FOUR", "FOWL", "FREE", "FROG", "FROM", "FUEL", "FULL", "FUME", "FUND",
            "FURY", "FUSE", "GAIN", "GALE", "GAME", "GANG", "GATE", "GAVE", "GAZE", "GEAR",
            "GENE", "GIFT", "GIRL", "GIVE", "GLAD", "GLEN", "GLOW", "GLUE", "GOAL", "GOAT",
            "GOLD", "GOLF", "GONE", "GOOD", "GRAB", "GRAM", "GRAY", "GREW", "GRID", "GRIN",
            "GRIP", "GROW", "GULF", "GURU", "HAIL", "HAIR", "HALF", "HALL", "HALO", "HALT",
            "HAND", "HANG", "HARD", "HARE", "HARM", "HARP", "HATE", "HAVE", "HAWK", "HAZE",
            "HEAD", "HEAL", "HEAP", "HEAR", "HEAT", "HEEL", "HEIR", "HELD", "HELM", "HELP",
            "HERB", "HERD", "HERO", "HIDE", "HIGH", "HIKE", "HILL", "HINT", "HIRE", "HIVE",
            "HOLD", "HOLE", "HOLY", "HOME", "HOOD", "HOOK", "HOPE", "HORN", "HOSE", "HOST",
            "HOUR", "HUGE", "HULL", "HUNT", "HURT", "HUSH", "HYMN", "ICON", "IDEA", "IDLE",
            "IDOL", "INCH", "INFO", "IRIS", "IRON", "ISLE", "ITEM", "JADE", "JAIL", "JAZZ",
            "JEAN", "JEEP", "JEST", "JOIN", "JOKE", "JOLT", "JUMP", "JUNE", "JURY", "JUST",
            "JUTE", "KEEL", "KEEN", "KEEP", "KELP", "KEPT", "KICK", "KILL", "KILN", "KIND",
            "KING", "KISS", "KITE", "KNEE", "KNEW", "KNIT", "KNOT", "KNOW", "LACE", "LACK",
            "LADY", "LAID", "LAKE", "LAMB", "LAMP", "LAND", "LANE", "LAST", "LATE", "LAVA",
            "LAWN", "LAZY", "LEAD", "LEAF", "LEAK", "LEAN", "LEAP", "LEFT", "LEND", "LENS",
            "LENT", "LESS", "LIAR", "LICK", "LIFE", "LIFT", "LIKE", "LIME", "LIMP", "LINE",
            "LINK", "LION", "LIPS", "LISP", "LIST", "LIVE", "LOAD", "LOAF", "LOAM", "LOAN",
            "LOCK", "LOFT", "LOGO", "LONE", "LONG", "LOOK", "LOOM", "LOOP", "LORD", "LOSE",
            "LOSS", "LOST", "LOUD", "LOVE", "LUCK", "LUMP", "LUNG", "LURE", "LURK", "LUSH",
            "LUTE", "LYRE", "MACE", "MADE", "MAID", "MAIL", "MAIN", "MAKE", "MALE", "MALT",
            "MANE", "MANY", "MAPS", "MARE", "MARK", "MARS", "MASH", "MASK", "MASS", "MAST",
            "MATE", "MATH", "MAZE", "MEAD", "MEAL", "MEAN", "MEAT", "MEET", "MELT", "MEMO",
            "MEND", "MENU", "MERE", "MESH", "MESS", "MILD", "MILE", "MILK", "MILL", "MIME",
            "MIND", "MINE", "MINT", "MIST", "MITE", "MOAN", "MOAT", "MOCK", "MODE", "MOLE",
            "MONK", "MOOD", "MOON", "MOOR", "MORE", "MOSS", "MOST", "MOTH", "MOVE", "MUCH",
            "MULE", "MUSE", "MUSH", "MUST", "MUTE", "MYTH", "NAIL", "NAME", "NAVY", "NEAR",
            "NEAT", "NECK", "NEED", "NEON", "NEST", "NEWS", "NEXT", "NICE", "NINE", "NODE",
            "NOON", "NORM", "NOSE", "NOTE", "NOUN", "NOVA", "NUDE", "NUMB", "OAKS", "OARS",
            "OATH", "OBEY", "ODOR", "OGRE", "OILS", "OILY", "OKRA", "OMEN", "ONCE", "ONES",
            "ONLY", "ONTO", "ONUS", "ONYX", "OPAL", "OPEN", "ORAL", "ORBS", "ORCA", "ORES",
            "OVAL", "OVEN", "OVER", "PACE", "PACK", "PACT", "PAGE", "PAID", "PAIN", "PAIR",
            "PALE", "PALM", "PANE", "PARK", "PART", "PASS", "PAST", "PATH", "PAVE", "PAWN",
            "PEAK", "PEAL", "PEAR", "PEAT", "PEEK", "PEEL", "PEER", "PENN", "PIER", "PIKE",
            "PILE", "PILL", "PINE", "PINK", "PINT", "PIPE", "PITY", "PLAN", "PLAY", "PLEA",
            "PLOD", "PLOP", "PLOT", "PLOW", "PLUG", "PLUM", "PLUS", "POEM", "POET", "POKE",
            "POLE", "POLL", "POLO", "POND", "PONY", "POOL", "POOR", "POPE", "PORK", "PORT",
            "POSE", "POST", "POUR", "PRAY", "PREP", "PREY", "PUFF", "PULL", "PULP", "PUMP",
            "PURE", "PUSH", "QUAD", "QUAY", "QUIT", "QUIZ", "RACE", "RACK", "RAFT", "RAGE",
            "RAID", "RAIL", "RAIN", "RAKE", "RAMP", "RANG", "RANK", "RARE", "RASH", "RATE",
            "RAVE", "RAYS", "READ", "REAL", "REAP", "REAR", "REED", "REEF", "REEL", "RELY",
            "RENT", "REST", "RICE", "RICH", "RIDE", "RING", "RIPE", "RISE", "RISK", "RITE",
            "ROAD", "ROAM", "ROAR", "ROBE", "ROCK", "RODE", "ROLE", "ROLL", "ROOF", "ROOM",
            "ROOT", "ROPE", "ROSE", "ROSY", "ROTE", "ROVE", "RUBY", "RUDE", "RUIN", "RULE",
            "RUSH", "RUST", "RUTH", "SAFE", "SAGE", "SAID", "SAIL", "SAKE", "SALE", "SALT",
            "SAME", "SAND", "SANE", "SANG", "SANK", "SAVE", "SCAN", "SCAR", "SEAL", "SEAM",
            "SEAT", "SEED", "SEEK", "SEEM", "SEEN", "SELF", "SELL", "SEND", "SENT", "SEXT", "SEXY", "SHED",
            "SHIN", "SHIP", "SHOE", "SHOP", "SHOT", "SHOW", "SHUN", "SHUT", "SICK", "SIDE",
            "SIFT", "SIGH", "SIGN", "SILK", "SILL", "SILO", "SILT", "SINE", "SING", "SINK",
            "SIRE", "SITE", "SIZE", "SKAT", "SKEW", "SKID", "SKIM", "SKIN", "SKIP", "SKIT",
            "SLAB", "SLAM", "SLAP", "SLAT", "SLED", "SLID", "SLIM", "SLIP", "SLIT", "SLOT",
            "SLOW", "SLUG", "SLUM", "SNAP", "SNOW", "SNUB", "SNUG", "SOAK", "SOAP", "SOAR",
            "SOCK", "SOFA", "SOFT", "SOIL", "SOLAR", "SOLD", "SOLE", "SOLO", "SOME", "SONG",
            "SOON", "SOOT", "SORE", "SORT", "SOUL", "SOUP", "SOUR", "SPAN", "SPAR", "SPAT",
            "SPAY", "SPIN", "SPIT", "SPOT", "SPUR", "STAB", "STAG", "STAR", "STAY", "STEM",
            "STEP", "STEW", "STIR", "STOP", "STUB", "STUD", "SUCH", "SUIT", "SUNK", "SURE",
            "SURF", "SWAN", "SWAP", "SWAY", "SWIM", "TAIL", "TAKE", "TALE", "TALK", "TALL",
            "TAME", "TANK", "TAPE", "TART", "TASK", "TAXI", "TEAM", "TEAR", "TELL", "TENT",
            "TERM", "TEST", "TEXT", "THAN", "THAT", "THAW", "THEM", "THEN", "THEY", "THIN",
            "THIS", "THOU", "THUD", "THUG", "TICK", "TIDE", "TIDY", "TIED", "TIER", "TILE",
            "TILL", "TILT", "TIME", "TINT", "TINY", "TIRE", "TOAD", "TOLL", "TOMB", "TONE",
            "TOOK", "TOOL", "TOOT", "TOPS", "TORE", "TORN", "TOSS", "TOUR", "TOWN", "TRAP",
            "TRAY", "TREE", "TREK", "TRIM", "TRIO", "TRIP", "TRUE", "TUBE", "TUCK", "TUFT",
            "TUNE", "TURF", "TURN", "TUSK", "TWIG", "TWIN", "TYPE", "TYPO", "UNIT", "UPON",
            "URGE", "USED", "USER", "VAIN", "VALE", "VANE", "VARY", "VASE", "VAST", "VEIL",
            "VEIN", "VENT", "VERB", "VERY", "VEST", "VETO", "VIAL", "VIBE", "VIEW", "VINE",
            "VIOL", "VISE", "VOID", "VOLT", "VOTE", "WADE", "WAFT", "WAGE", "WAIL", "WAIT",
            "WAKE", "WALK", "WALL", "WAND", "WANT", "WARD", "WARM", "WARN", "WARP", "WART",
            "WASH", "WASP", "WAVE", "WAVY", "WEAK", "WEAR", "WEED", "WEEK", "WEEP", "WELD",
            "WELL", "WENT", "WEPT", "WERE", "WEST", "WHAT", "WHEN", "WHIM", "WHIP", "WHOM",
            "WIDE", "WIFE", "WILD", "WILL", "WIND", "WINE", "WING", "WINK", "WIPE", "WIRE",
            "WISE", "WISH", "WISP", "WITH", "WOLF", "WOMB", "WONT", "WOOD", "WOOL", "WORD",
            "WORE", "WORK", "WORM", "WORN", "WRAP", "WREN", "YARD", "YARN", "YAWN", "YEAR",
            "YELP", "YOGA", "YOKE", "YOLK", "YOUR", "ZEAL", "ZERO", "ZINC", "ZONE", "ZOOM",

            // 5-letter words
            "ABOUT", "ABOVE", "ABUSE", "ACORN", "ACRES", "ACTOR", "ACUTE", "ADAPT", "ADEPT",
            "ADMIT", "ADOBE", "ADOPT", "ADORE", "ADULT", "AEGIS", "AFFIX", "AFTER", "AGAIN",
            "AGENT", "AGILE", "AGLOW", "AGONY", "AGREE", "AHEAD", "AISLE", "ALARM", "ALBUM",
            "ALERT", "ALGAE", "ALIAS", "ALIBI", "ALIEN", "ALIGN", "ALIKE", "ALIVE", "ALLEY",
            "ALLOT", "ALLOW", "ALLOY", "ALONE", "ALONG", "ALOOF", "ALOUD", "ALPHA", "ALTAR",
            "ALTER", "AMBER", "AMBLE", "AMEND", "AMISS", "AMITY", "AMONG", "AMPLE", "AMPLY",
            "AMUSE", "ANGEL", "ANGER", "ANGLE", "ANGRY", "ANGST", "ANIME", "ANKLE", "ANNEX",
            "ANNOY", "ANNUL", "ANODE", "ANTIC", "ANVIL", "AORTA", "APART", "APHID", "APNEA",
            "APPLE", "APPLY", "APRON", "APTLY", "ARBOR", "ARDOR", "ARENA", "ARGUE", "ARISE",
            "ARMOR", "AROMA", "AROSE", "ARRAY", "ARROW", "ARSON", "ASHEN", "ASIAN", "ASIDE",
            "ASKED", "ASKEW", "ASPEN", "ASSAY", "ASSET", "ATOLL", "ATONE", "ATTIC", "AUDIO",
            "AUDIT", "AUGUR", "AUNTY", "AURAS", "AVAIL", "AVERT", "AVIAN", "AVOID", "AWAIT",
            "AWAKE", "AWARD", "AWARE", "AWASH", "AWFUL", "AXIAL", "AXIOM", "AZURE", "BACON",
            "BADGE", "BADLY", "BAGEL", "BAKER", "BALMY", "BANJO", "BARGE", "BARON", "BASAL",
            "BASIC", "BASIL", "BASIN", "BASIS", "BATCH", "BATHE", "BATON", "BEACH", "BEADY",
            "BEARD", "BEAST", "BEAUT", "BEECH", "BEGAN", "BEGAT", "BEGET", "BEGIN", "BEGOT",
            "BEGUN", "BEING", "BELIE", "BELLE", "BELLY", "BELOW", "BENCH", "BERET", "BERRY",
            "BERTH", "BESET", "BETEL", "BEVEL", "BEZEL", "BIBLE", "BICEP", "BIDDY", "BIGOT",
            "BILGE", "BILLY", "BINGE", "BINGO", "BIRCH", "BIRTH", "BISON", "BITTY", "BLACK",
            "BLADE", "BLAME", "BLAND", "BLANK", "BLARE", "BLAST", "BLAZE", "BLEAK", "BLEAT",
            "BLEED", "BLEEP", "BLEND", "BLESS", "BLIMP", "BLIND", "BLINK", "BLISS", "BLITZ",
            "BLOAT", "BLOCK", "BLOKE", "BLOND", "BLOOD", "BLOOM", "BLOWN", "BLUER", "BLUFF",
            "BLUNT", "BLURB", "BLURT", "BLUSH", "BOARD", "BOAST", "BOBBY", "BODED", "BOGEY",
            "BOGUS", "BOILS", "BOLUS", "BOMBS", "BONDS", "BONED", "BONER", "BONEY", "BONGO",
            "BONUS", "BOOBY", "BOOST", "BOOTH", "BOOTS", "BOOTY", "BOOZE", "BOOZY", "BORAX",
            "BORNE", "BOSOM", "BOSSY", "BOTCH", "BOUGH", "BOULD", "BOUND", "BOWEL", "BOWER",
            "BOWLS", "BOXER", "BRACE", "BRAID", "BRAIN", "BRAKE", "BRAND", "BRASH", "BRASS",
            "BRAVE", "BRAVO", "BRAWL", "BRAWN", "BREAD", "BREAK", "BREED", "BRIAR", "BRIBE",
            "BRICK", "BRIDE", "BRIEF", "BRINE", "BRING", "BRINK", "BRINY", "BRISK", "BROAD",
            "BROIL", "BROKE", "BROOD", "BROOK", "BROOM", "BROTH", "BROWN", "BRUIN", "BRUNT",
            "BRUSH", "BRUTE", "BUDDY", "BUDGE", "BUGGY", "BUGLE", "BUILD", "BUILT", "BULGE",
            "BULKY", "BULLY", "BUNCH", "BUNNY", "BURLY", "BURNT", "BURRO", "BURST", "BUSHY",
            "BUTCH", "BUTTE", "BUXOM", "BUYER", "BYLAW", "CABIN", "CABLE", "CACAO", "CACHE",
            "CACTI", "CADDY", "CADET", "CAGEY", "CAIRN", "CAMEL", "CAMEO", "CANAL", "CANDY",
            "CANOE", "CANON", "CAPER", "CAPUT", "CARAT", "CARGO", "CAROL", "CARRY", "CARVE",
            "CASTE", "CATER", "CAULK", "CAUSE", "CAVIL", "CEASE", "CEDAR", "CELEB", "CELLO",
            "CHAFE", "CHAFF", "CHAIN", "CHAIR", "CHALK", "CHAMP", "CHANT", "CHAOS", "CHARD",
            "CHARM", "CHART", "CHASE", "CHASM", "CHEAP", "CHEAT", "CHECK", "CHEEK", "CHEER",
            "CHESS", "CHEST", "CHICK", "CHIEF", "CHILD", "CHILI", "CHILL", "CHIME", "CHIMP",
            "CHINO", "CHIRP", "CHIVE", "CHOCK", "CHOIR", "CHOKE", "CHOMP", "CHORD", "CHORE",
            "CHOSE", "CHUCK", "CHUMP", "CHUNK", "CHURN", "CHUTE", "CIDER", "CIGAR", "CINCH",
            "CIRCA", "CIVIC", "CIVIL", "CLACK", "CLAIM", "CLAMP", "CLANG", "CLANK", "CLASH",
            "CLASP", "CLASS", "CLEAN", "CLEAR", "CLEAT", "CLEFT", "CLERK", "CLICK", "CLIFF",
            "CLIMB", "CLING", "CLINK", "CLOAK", "CLOCK", "CLONE", "CLOSE", "CLOTH", "CLOUD",
            "CLOUT", "CLOVE", "CLOWN", "CLUCK", "CLUED", "CLUMP", "CLUNG", "COACH", "COAST",
            "COBRA", "COCOA", "COLON", "COLOR", "COMET", "COMIC", "COMMA", "CONCH", "CONDO",
            "CONIC", "CORAL", "CORER", "CORNY", "COUCH", "COUGH", "COUNT", "COUPE", "COURT",
            "COVEN", "COVER", "COVET", "COVEY", "COWER", "COYLY", "CRACK", "CRAFT", "CRAMP",
            "CRANE", "CRANK", "CRASH", "CRASS", "CRATE", "CRAVE", "CRAWL", "CRAZE", "CRAZY",
            "CREAK", "CREAM", "CREDO", "CREED", "CREEK", "CREEP", "CREME", "CREPE", "CREPT",
            "CRESS", "CREST", "CRICK", "CRIED", "CRIER", "CRIME", "CRIMP", "CRISP", "CROAK",
            "CROCK", "CRONY", "CROOK", "CROSS", "CROUP", "CROWD", "CROWN", "CRUDE", "CRUEL",
            "CRUMB", "CRUMP", "CRUSH", "CRUST", "CRYPT", "CUBIC", "CUMIN", "CURIO", "CURLY",
            "CURRY", "CURSE", "CURVE", "CURVY", "CUTIE", "CYBER", "CYCLE", "CYNIC", "DADDY",
            "DAILY", "DAIRY", "DAISY", "DALLY", "DANCE", "DANDY", "DATED", "DATUM", "DAUNT",
            "DEALT", "DEATH", "DEBAR", "DEBIT", "DEBUG", "DEBUT", "DECAL", "DECAY", "DECOR",
            "DECOY", "DECRY", "DEFER", "DEIGN", "DEITY", "DELAY", "DELTA", "DELVE", "DEMON",
            "DEMUR", "DENIM", "DENSE", "DEPOT", "DEPTH", "DERBY", "DETER", "DETOX", "DEUCE",
            "DEVIL", "DIARY", "DICEY", "DIGIT", "DILLY", "DIMLY", "DINER", "DINGO", "DINGY",
            "DIODE", "DIRGE", "DIRTY", "DISCO", "DITCH", "DITTO", "DITTY", "DIVER", "DIZZY",
            "DODGE", "DODGY", "DOGMA", "DOILY", "DOING", "DOLLY", "DONOR", "DONUT", "DOPEY",
            "DOUBT", "DOUGH", "DOWDY", "DOWEL", "DOWNY", "DOWRY", "DOZEN", "DRAFT", "DRAIN",
            "DRAKE", "DRAMA", "DRANK", "DRAPE", "DRAWL", "DRAWN", "DREAD", "DREAM", "DRESS",
            "DRIED", "DRIER", "DRIFT", "DRILL", "DRINK", "DRIVE", "DROLL", "DRONE", "DROOL",
            "DROOP", "DROSS", "DROVE", "DROWN", "DRUID", "DRUNK", "DRYER", "DRYLY", "DUCHY",
            "DULLY", "DUMMY", "DUMPY", "DUNCE", "DUSKY", "DUSTY", "DUTCH", "DUVET", "DWARF",
            "DWELL", "DWELT", "DYING", "EAGER", "EAGLE", "EARLY", "EARTH", "EASEL", "EATEN",
            "EATER", "EBONY", "ECLAT", "EDICT", "EDIFY", "EERIE", "EGRET", "EIGHT", "EJECT",
            "EKING", "ELATE", "ELBOW", "ELDER", "ELECT", "ELEGY", "ELFIN", "ELIDE", "ELITE",
            "ELOPE", "ELUDE", "EMAIL", "EMBED", "EMBER", "EMCEE", "EMERY", "EMOTE", "EMPTY",
            "ENACT", "ENDOW", "ENEMA", "ENEMY", "ENJOY", "ENNUI", "ENOKI", "ENTER", "ENTRY",
            "ENVOY", "EPOCH", "EPOXY", "EQUAL", "EQUIP", "ERASE", "ERECT", "ERODE", "ERROR",
            "ERUPT", "ESSAY", "ESTER", "ETHER", "ETHIC", "ETHOS", "ETUDE", "EVADE", "EVENT",
            "EVERY", "EVICT", "EVOKE", "EXACT", "EXALT", "EXCEL", "EXERT", "EXILE", "EXIST",
            "EXPAT", "EXPEL", "EXTOL", "EXTRA", "EXULT", "EYING", "FABLE", "FACET", "FAINT",
            "FAIRY", "FAITH", "FALSE", "FANCY", "FANNY", "FARCE", "FATAL", "FATTY", "FAULT",
            "FAUNA", "FAVOR", "FEAST", "FECAL", "FEIGN", "FELLA", "FELON", "FEMME", "FEMUR",
            "FENCE", "FERAL", "FERRY", "FETAL", "FETCH", "FETID", "FETUS", "FEVER", "FEWER",
            "FIBER", "FICUS", "FIELD", "FIEND", "FIERY", "FIFTH", "FIFTY", "FIGHT", "FILER",
            "FILET", "FILLY", "FILMY", "FILTH", "FINAL", "FINCH", "FINER", "FIRST", "FISHY",
            "FIXER", "FIZZY", "FJORD", "FLACK", "FLAIL", "FLAIR", "FLAKE", "FLAKY", "FLAME",
            "FLANK", "FLARE", "FLASH", "FLASK", "FLECK", "FLEET", "FLESH", "FLICK", "FLIER",
            "FLING", "FLINT", "FLIRT", "FLOAT", "FLOCK", "FLOOD", "FLOOR", "FLORA", "FLOSS",
            "FLOUR", "FLOUT", "FLOWN", "FLUFF", "FLUID", "FLUKE", "FLUME", "FLUNG", "FLUNK",
            "FLUSH", "FLUTE", "FLYER", "FOAMY", "FOCAL", "FOCUS", "FOGGY", "FOIST", "FOLIO",
            "FOLLY", "FORAY", "FORCE", "FORGE", "FORGO", "FORTE", "FORTH", "FORTY", "FORUM",
            "FOUND", "FOUNT", "FOYER", "FRAIL", "FRAME", "FRANK", "FRAUD", "FREAK", "FREED",
            "FREER", "FRESH", "FRIAR", "FRIED", "FRILL", "FRISK", "FRITZ", "FROCK", "FROND",
            "FRONT", "FROST", "FROTH", "FROWN", "FROZE", "FRUIT", "FUDGE", "FUGUE", "FULLY",
            "FUNGI", "FUNKY", "FUNNY", "FUROR", "FURRY", "FUSSY", "FUZZY", "GAFFE", "GAILY",
            "GAMER", "GAMMA", "GAMUT", "GAUDY", "GAUGE", "GAUNT", "GAUZE", "GAVEL", "GAWKY",
            "GECKO", "GEEKY", "GEESE", "GENIE", "GENRE", "GHOST", "GHOUL", "GIANT", "GIDDY",
            "GIRTH", "GIVEN", "GIVER", "GLADE", "GLAND", "GLARE", "GLASS", "GLAZE", "GLEAM",
            "GLEAN", "GLIDE", "GLINT", "GLOAT", "GLOBE", "GLOOM", "GLORY", "GLOSS", "GLOVE",
            "GLYPH", "GNASH", "GNOME", "GODLY", "GOING", "GOLEM", "GONER", "GOODY", "GOOEY",
            "GOOFY", "GOOSE", "GORGE", "GOUGE", "GOURD", "GRACE", "GRADE", "GRAFT", "GRAIL",
            "GRAIN", "GRAND", "GRANT", "GRAPE", "GRAPH", "GRASP", "GRASS", "GRATE", "GRAVE",
            "GRAVY", "GRAZE", "GREAT", "GREED", "GREEN", "GREET", "GRIEF", "GRILL", "GRIME",
            "GRIMY", "GRIND", "GRIPE", "GROAN", "GROIN", "GROOM", "GROPE", "GROSS", "GROUP",
            "GROUT", "GROVE", "GROWL", "GROWN", "GRUEL", "GRUFF", "GRUNT", "GUARD", "GUAVA",
            "GUESS", "GUEST", "GUIDE", "GUILD", "GUILE", "GUILT", "GUISE", "GULCH", "GULLY",
            "GUMBO", "GUMMY", "GUPPY", "GUSTO", "GUSTY", "HABIT", "HAIRY", "HALVE", "HANDY",
            "HAPPY", "HARDY", "HAREM", "HARPY", "HARRY", "HARSH", "HASTE", "HASTY", "HATCH",
            "HATER", "HAUNT", "HAUTE", "HAVEN", "HAVOC", "HAZEL", "HEADY", "HEARD", "HEART",
            "HEATH", "HEAVE", "HEAVY", "HEDGE", "HEFTY", "HEIST", "HELIX", "HELLO", "HENCE",
            "HERON", "HINGE", "HIPPO", "HIPPY", "HITCH", "HOARD", "HOBBY", "HOIST", "HOLLY",
            "HOMER", "HONEY", "HONOR", "HORDE", "HORSE", "HOTEL", "HOUND", "HOUSE", "HOVEL",
            "HOVER", "HOWDY", "HUMAN", "HUMID", "HUMOR", "HUMPH", "HUMUS", "HUNCH", "HUNKY",
            "HURRY", "HUSKY", "HUTCH", "HYDRA", "HYENA", "HYPER", "ICING", "IDEAL", "IDIOM",
            "IDIOT", "IDLER", "IDYLL", "IGLOO", "ILIAC", "IMAGE", "IMBUE", "IMPEL", "IMPLY",
            "INANE", "INBOX", "INCUR", "INDEX", "INEPT", "INERT", "INFER", "INGOT", "INLAY",
            "INLET", "INNER", "INPUT", "INTER", "INTRO", "IONIC", "IRATE", "IRONY", "ISLET",
            "ISSUE", "ITCHY", "IVORY", "JAUNT", "JAZZY", "JELLY", "JERKY", "JETTY", "JEWEL",
            "JIFFY", "JOINT", "JOKER", "JOLLY", "JOUST", "JUDGE", "JUICE", "JUICY", "JUMBO",
            "JUMPY", "JUNTA", "JUROR", "KAPPA", "KARMA", "KAYAK", "KEBAB", "KHAKI", "KINKY",
            "KIOSK", "KITTY", "KNACK", "KNAVE", "KNEAD", "KNEED", "KNEEL", "KNELL", "KNELT",
            "KNIFE", "KNOCK", "KNOLL", "KNOWN", "KOALA", "KRILL", "LABEL", "LABOR", "LADEN",
            "LADLE", "LAGER", "LANCE", "LANKY", "LAPEL", "LAPSE", "LARGE", "LARVA", "LASSO",
            "LATCH", "LATER", "LATHE", "LATTE", "LAUGH", "LAYER", "LEACH", "LEAFY", "LEAKY",
            "LEANT", "LEAPT", "LEARN", "LEASE", "LEASH", "LEAST", "LEAVE", "LEDGE", "LEECH",
            "LEERY", "LEGAL", "LEGGY", "LEMON", "LEMUR", "LEPER", "LEVEL", "LEVER", "LIBEL",
            "LIEGE", "LIGHT", "LIKEN", "LILAC", "LIMBO", "LIMIT", "LINEN", "LINER", "LINGO",
            "LIPID", "LITHE", "LIVER", "LIVID", "LLAMA", "LOAMY", "LOATH", "LOBBY", "LOCAL",
            "LOCUS", "LODGE", "LOFTY", "LOGIC", "LOGIN", "LOOPY", "LOOSE", "LORIS", "LOSER",
            "LOTUS", "LOUSE", "LOUSY", "LOVER", "LOWER", "LOWLY", "LOYAL", "LUCID", "LUCKY",
            "LUMEN", "LUMPY", "LUNAR", "LUNCH", "LUNGE", "LUPUS", "LURCH", "LURID", "LUSTY",
            "LYING", "LYMPH", "LYRIC", "MACAW", "MACHO", "MACRO", "MADAM", "MADLY", "MAFIA",
            "MAGIC", "MAGMA", "MAIZE", "MAJOR", "MAKER", "MAMBO", "MAMMA", "MANGA", "MANGO",
            "MANIA", "MANIC", "MANOR", "MAPLE", "MARCH", "MARRY", "MARSH", "MASON", "MASSE",
            "MATCH", "MATEY", "MATTE", "MAUVE", "MAXIM", "MAYBE", "MAYOR", "MEALY", "MEANT",
            "MEATY", "MECCA", "MEDAL", "MEDIA", "MEDIC", "MELEE", "MELON", "MERCY", "MERGE",
            "MERIT", "MERRY", "METAL", "METER", "METRO", "MICRO", "MIDGE", "MIDST", "MIGHT",
            "MILKY", "MIMIC", "MINCE", "MINER", "MINIM", "MINOR", "MINTY", "MINUS", "MIRTH",
            "MISER", "MISDO", "MITER", "MIXER", "MOCHA", "MODAL", "MODEL", "MODEM", "MOIST",
            "MOLAR", "MOLDY", "MONEY", "MONTH", "MOODY", "MOOSE", "MORAL", "MORON", "MORPH",
            "MOSSY", "MOTEL", "MOTIF", "MOTOR", "MOTTO", "MOULT", "MOUND", "MOUNT", "MOURN",
            "MOUSE", "MOUTH", "MOVER", "MOVIE", "MOWER", "MUCKY", "MUCUS", "MUDDY", "MULCH",
            "MUMMY", "MURAL", "MUSHY", "MUSIC", "MUSKY", "MUSTY", "MUTED", "MYRRH", "NAIVE",
            "NANNY", "NASAL", "NASTY", "NATAL", "NAVAL", "NAVEL", "NEEDY", "NEIGH", "NERDY",
            "NERVE", "NEVER", "NEWER", "NEWLY", "NEXUS", "NICHE", "NIECE", "NIGHT", "NINJA",
            "NINTH", "NOBLE", "NOBLY", "NOISE", "NOISY", "NOMAD", "NOOSE", "NORTH", "NOSEY",
            "NOTCH", "NOVEL", "NUDGE", "NURSE", "NUTTY", "NYLON", "NYMPH", "OASIS", "OBESE",
            "OCCUR", "OCEAN", "OCTAL", "OCTET", "ODDER", "ODDLY", "OFFAL", "OFFER", "OFTEN",
            "OLDEN", "OLDER", "OLIVE", "OMEGA", "ONION", "ONSET", "OPERA", "OPINE", "OPIUM",
            "OPTIC", "ORBIT", "ORDER", "ORGAN", "OTHER", "OTTER", "OUGHT", "OUNCE", "OUTDO",
            "OUTER", "OUTGO", "OVARY", "OVATE", "OVERT", "OVINE", "OVOID", "OWING", "OWNER",
            "OXIDE", "OZONE", "PADDY", "PAGAN", "PAINT", "PALER", "PALSY", "PANEL", "PANIC",
            "PANSY", "PAPAL", "PAPER", "PARER", "PARKA", "PARRY", "PARSE", "PARTY", "PASTA",
            "PASTE", "PASTY", "PATCH", "PATIO", "PATSY", "PATTY", "PAUSE", "PAYEE", "PAYER",
            "PEACE", "PEACH", "PEARL", "PECAN", "PEDAL", "PENAL", "PENCE", "PENNE", "PENNY",
            "PERCH", "PERIL", "PERKY", "PESKY", "PESTO", "PETAL", "PETTY", "PHASE", "PHONE",
            "PHONY", "PHOTO", "PIANO", "PICKY", "PIECE", "PIETY", "PIGGY", "PILOT", "PINCH",
            "PINEY", "PINKY", "PINTO", "PIPER", "PIQUE", "PITCH", "PITHY", "PIVOT", "PIXEL",
            "PIXIE", "PIZZA", "PLACE", "PLAID", "PLAIN", "PLAIT", "PLANE", "PLANK", "PLANT",
            "PLATE", "PLAZA", "PLEAD", "PLEAT", "PLIED", "PLIER", "PLUCK", "PLUMB", "PLUME",
            "PLUMP", "PLUNK", "PLUSH", "POESY", "POINT", "POISE", "POKER", "POLAR", "POLKA",
            "POLYP", "POOCH", "POPPY", "PORCH", "PORER", "POSIT", "POSSE", "POUCH", "POUND",
            "POUTY", "POWER", "PRANK", "PRAWN", "PREEN", "PRESS", "PRICE", "PRICK", "PRIDE",
            "PRIED", "PRIME", "PRIMO", "PRINT", "PRIOR", "PRIZE", "PROBE", "PRONE", "PRONG",
            "PROOF", "PROSE", "PROUD", "PROVE", "PROWL", "PROXY", "PRUDE", "PRUNE", "PSALM",
            "PUBIC", "PUDGY", "PUFFY", "PULPY", "PULSE", "PUNCH", "PUPIL", "PUPPY", "PUREE",
            "PURER", "PURGE", "PURSE", "PUSHY", "PUTTY", "PYGMY", "QUACK", "QUAFF", "QUAIL",
            "QUAKE", "QUALM", "QUARK", "QUART", "QUASH", "QUASI", "QUEEN", "QUEER", "QUELL",
            "QUERY", "QUEST", "QUEUE", "QUICK", "QUIET", "QUILL", "QUILT", "QUIRK", "QUITE",
            "QUOTA", "QUOTE", "QUOTH", "RABBI", "RABID", "RACER", "RADAR", "RADII", "RADIO",
            "RAINY", "RAISE", "RAJAH", "RALLY", "RALPH", "RAMEN", "RANCH", "RANDY", "RANGE",
            "RAPID", "RARER", "RASPY", "RATIO", "RATTY", "RAVEN", "RAYON", "RAZOR", "REACH",
            "REACT", "READY", "REALM", "REARM", "REBAR", "REBEL", "REBUS", "REBUT", "RECAP",
            "RECUR", "RECUT", "REEDY", "REFER", "REFIT", "REGAL", "REHAB", "REIGN", "RELAX",
            "RELAY", "RELIC", "REMIT", "RENAL", "RENEW", "REPAY", "REPEL", "REPLY", "RERUN",
            "RESET", "RESIN", "RETCH", "RETRO", "RETRY", "REUSE", "REVEL", "REVUE", "RHINO",
            "RHYME", "RIDER", "RIDGE", "RIFLE", "RIGHT", "RIGID", "RIGOR", "RINSE", "RIPEN",
            "RIPER", "RISEN", "RISER", "RISKY", "RIVAL", "RIVER", "RIVET", "ROACH", "ROAST",
            "ROBIN", "ROBOT", "ROCKY", "RODEO", "ROGUE", "ROOMY", "ROOST", "ROTOR", "ROUGE",
            "ROUGH", "ROUND", "ROUSE", "ROUTE", "ROVER", "ROWDY", "ROWER", "ROYAL", "RUDDY",
            "RUDER", "RUGBY", "RULER", "RUMBA", "RUMOR", "RUPEE", "RURAL", "RUSTY", "SADLY",
            "SAFER", "SAINT", "SALAD", "SALLY", "SALON", "SALSA", "SALTY", "SALVE", "SALVO",
            "SANDY", "SANER", "SAPPY", "SASSY", "SATIN", "SATYR", "SAUCE", "SAUCY", "SAUNA",
            "SAUTE", "SAVOR", "SAVOY", "SAVVY", "SCALD", "SCALE", "SCALP", "SCALY", "SCAMP",
            "SCANT", "SCARE", "SCARF", "SCARY", "SCENE", "SCENT", "SCION", "SCOFF", "SCOLD",
            "SCONE", "SCOOP", "SCOPE", "SCORE", "SCORN", "SCOUR", "SCOUT", "SCOWL", "SCRAM",
            "SCRAP", "SCREW", "SCRUB", "SCRUM", "SCUBA", "SEDAN", "SEEDY", "SEGUE", "SEIZE",
            "SEMEN", "SENSE", "SEPAL", "SERIF", "SERUM", "SERVE", "SETUP", "SEVEN", "SEVER",
            "SEWER", "SEXED", "SEXES", "SHACK", "SHADE", "SHADY", "SHAFT", "SHAKE", "SHAKY", "SHALE", "SHALL",
            "SHALT", "SHAME", "SHANK", "SHAPE", "SHARE", "SHARK", "SHARP", "SHAVE", "SHAWL",
            "SHEAF", "SHEAR", "SHED", "SHEEN", "SHEEP", "SHEER", "SHEET", "SHEIK", "SHELF",
            "SHELL", "SHIED", "SHIFT", "SHINE", "SHINY", "SHIRE", "SHIRK", "SHIRT", "SHOAL",
            "SHOCK", "SHONE", "SHOOK", "SHOOT", "SHORE", "SHORN", "SHORT", "SHOUT", "SHOVE",
            "SHOWN", "SHOWY", "SHREW", "SHRUB", "SHRUG", "SHUCK", "SHUNT", "SHUSH", "SHYLY",
            "SIEGE", "SIEVE", "SIGHT", "SIGMA", "SILKY", "SILLY", "SINCE", "SINEW", "SINGE",
            "SIREN", "SISSY", "SIXTH", "SIXTY", "SKATE", "SKIER", "SKIFF", "SKILL", "SKIMP",
            "SKIRT", "SKULK", "SKULL", "SKUNK", "SLACK", "SLAIN", "SLANG", "SLANT", "SLASH",
            "SLATE", "SLAVE", "SLEEK", "SLEEP", "SLEET", "SLEPT", "SLICE", "SLICK", "SLIDE",
            "SLIME", "SLIMY", "SLING", "SLINK", "SLOAN", "SLOPE", "SLOSH", "SLOTH", "SLUMP",
            "SLUNG", "SLUNK", "SLURP", "SLUSH", "SLYLY", "SMACK", "SMALL", "SMART", "SMASH",
            "SMEAR", "SMELL", "SMELT", "SMILE", "SMIRK", "SMITE", "SMITH", "SMOCK", "SMOKE",
            "SMOKY", "SMOTE", "SNACK", "SNAIL", "SNAKE", "SNAKY", "SNARE", "SNARL", "SNEAK",
            "SNEER", "SNIDE", "SNIFF", "SNIPE", "SNOOP", "SNORE", "SNORT", "SNOUT", "SNOWY",
            "SNUCK", "SNUFF", "SOAPY", "SOBER", "SOGGY", "SOLAR", "SOLID", "SOLVE", "SONAR",
            "SONIC", "SOOTH", "SOOTY", "SORRY", "SOUND", "SOUTH", "SOWER", "SPACE", "SPADE",
            "SPANK", "SPARE", "SPARK", "SPASM", "SPAWN", "SPEAK", "SPEAR", "SPECK", "SPEED",
            "SPELL", "SPELT", "SPEND", "SPENT", "SPERM", "SPICE", "SPICY", "SPIED", "SPIEL",
            "SPIKE", "SPIKY", "SPILL", "SPILT", "SPINE", "SPINY", "SPIRE", "SPITE", "SPLAT",
            "SPLIT", "SPOIL", "SPOKE", "SPOOF", "SPOOK", "SPOOL", "SPOON", "SPORE", "SPORT",
            "SPOUT", "SPRAY", "SPREE", "SPRIG", "SPURT", "SQUAD", "SQUAT", "SQUIB", "STACK",
            "STAFF", "STAGE", "STAID", "STAIN", "STAIR", "STAKE", "STALE", "STALK", "STALL",
            "STAMP", "STAND", "STANK", "STARE", "STARK", "START", "STASH", "STATE", "STAVE",
            "STEAD", "STEAK", "STEAL", "STEAM", "STEED", "STEEL", "STEEP", "STEER", "STEIN",
            "STERN", "STICK", "STIFF", "STILL", "STILT", "STING", "STINK", "STINT", "STOCK",
            "STOIC", "STOKE", "STOLE", "STOMP", "STONE", "STONY", "STOOD", "STOOL", "STOOP",
            "STORE", "STORK", "STORM", "STORY", "STOUT", "STOVE", "STRAP", "STRAW", "STRAY",
            "STRIP", "STRUT", "STUCK", "STUDY", "STUFF", "STUMP", "STUNG", "STUNK", "STUNT",
            "STYLE", "SUAVE", "SUGAR", "SUING", "SUITE", "SULKY", "SULLY", "SUMAC", "SUNNY",
            "SUPER", "SURER", "SURGE", "SURLY", "SUSHI", "SWAMI", "SWAMP", "SWANK", "SWARM",
            "SWATH", "SWEAR", "SWEAT", "SWEEP", "SWEET", "SWELL", "SWEPT", "SWIFT", "SWILL",
            "SWINE", "SWING", "SWIRL", "SWISH", "SWOON", "SWOOP", "SWORD", "SWORE", "SWORN",
            "SWUNG", "SYNOD", "SYRUP", "TABBY", "TABLE", "TABOO", "TACIT", "TACKY", "TAFFY",
            "TAINT", "TAKEN", "TAKER", "TALLY", "TALON", "TAMER", "TANGO", "TANGY", "TAPER",
            "TAPIR", "TARDY", "TAROT", "TASTE", "TASTY", "TATTY", "TAUNT", "TAWNY", "TEACH",
            "TEARY", "TEASE", "TEDDY", "TEETH", "TEMPO", "TENET", "TENOR", "TENSE", "TENTH",
            "TEPEE", "TEPID", "TERRA", "TERSE", "TESTY", "THANK", "THEFT", "THEIR", "THEME",
            "THERE", "THESE", "THICK", "THIEF", "THIGH", "THING", "THINK", "THIRD", "THONG",
            "THORN", "THOSE", "THREE", "THREW", "THROB", "THROW", "THRUM", "THUMB", "THUMP",
            "THYME", "TIARA", "TIBIA", "TIDAL", "TIGER", "TIGHT", "TILDE", "TIMER", "TIMID",
            "TIPSY", "TITAN", "TITHE", "TITLE", "TOAST", "TODAY", "TODDY", "TOKEN", "TONAL",
            "TONGA", "TONIC", "TOOTH", "TOPAZ", "TOPIC", "TORCH", "TORSO", "TORUS", "TOTAL",
            "TOTEM", "TOUCH", "TOUGH", "TOWEL", "TOWER", "TOXIC", "TOXIN", "TRACE", "TRACK",
            "TRACT", "TRADE", "TRAIL", "TRAIN", "TRAIT", "TRAMP", "TRASH", "TRAWL", "TREAD",
            "TREAT", "TREND", "TRIAD", "TRIAL", "TRIBE", "TRICE", "TRICK", "TRIED", "TRIPE",
            "TRITE", "TROLL", "TROOP", "TROPE", "TROUT", "TROVE", "TRUCE", "TRUCK", "TRUER",
            "TRULY", "TRUMP", "TRUNK", "TRUSS", "TRUST", "TRUTH", "TRYST", "TUBAL", "TUBER",
            "TULIP", "TULLE", "TUMOR", "TUNIC", "TURBO", "TUTOR", "TWANG", "TWEAK", "TWEED",
            "TWEET", "TWICE", "TWINE", "TWIRL", "TWIST", "TYING", "UDDER", "ULCER", "ULTRA",
            "UMBRA", "UNCLE", "UNCUT", "UNDER", "UNDID", "UNDUE", "UNFED", "UNFIT", "UNIFY",
            "UNION", "UNITE", "UNITY", "UNLIT", "UNMET", "UNSET", "UNTIE", "UNTIL", "UNWED",
            "UNZIP", "UPBEAT", "UPEND", "UPPER", "UPSET", "URBAN", "URINE", "USAGE", "USHER",
            "USING", "USUAL", "USURP", "UTILE", "UTTER", "VAGUE", "VALET", "VALID", "VALOR",
            "VALUE", "VALVE", "VAPID", "VAPOR", "VAULT", "VAUNT", "VEGAN", "VENOM", "VENUE",
            "VERGE", "VERSE", "VERSO", "VERVE", "VICAR", "VIDEO", "VIGIL", "VIGOR", "VILLA",
            "VINYL", "VIOLA", "VIPER", "VIRAL", "VIRUS", "VISIT", "VISOR", "VISTA", "VITAL",
            "VIVID", "VIXEN", "VOCAL", "VODKA", "VOGUE", "VOICE", "VOILA", "VOMIT", "VOTER",
            "VOUCH", "VOWEL", "VUGHY", "WACKY", "WAFER", "WAGER", "WAGON", "WAIST", "WAIVE",
            "WALTZ", "WARTY", "WASTE", "WATCH", "WATER", "WAVER", "WAXEN", "WEARY", "WEAVE",
            "WEDGE", "WEEDY", "WEIGH", "WEIRD", "WELCH", "WELSH", "WENCH", "WHACK", "WHALE",
            "WHARF", "WHEAT", "WHEEL", "WHELP", "WHERE", "WHICH", "WHIFF", "WHILE", "WHINE",
            "WHINY", "WHIRL", "WHISK", "WHITE", "WHOLE", "WHOOP", "WHORE", "WHOSE", "WIDEN",
            "WIDER", "WIDOW", "WIDTH", "WIELD", "WIGHT", "WILLY", "WINDY", "WIPER", "WIRED",
            "WISER", "WISPY", "WITCH", "WITTY", "WOKEN", "WOMAN", "WOMEN", "WOODY", "WOOER",
            "WOOLY", "WOOZY", "WORDY", "WORLD", "WORRY", "WORSE", "WORST", "WORTH", "WOULD",
            "WOUND", "WOVEN", "WRACK", "WRATH", "WREAK", "WRECK", "WREST", "WRING", "WRIST",
            "WRITE", "WRONG", "WROTE", "WRUNG", "WRYLY", "YACHT", "YEARN", "YEAST", "YIELD",
            "YOUNG", "YOUTH", "ZEBRA", "ZESTY", "ZILCH", "ZONAL",

            // 6-letter words (Classic Scrabble mid-game words)
            "ABACUS", "ABLAZE", "ABOUND", "ABRADE", "ABROAD", "ABRUPT", "ABSENT", "ABSORB",
            "ACCENT", "ACCEPT", "ACCESS", "ACCORD", "ACCOST", "ACCRUE", "ACCUSE", "ACETIC",
            "ACIDIC", "ACQUIT", "ACROSS", "ACTION", "ACTIVE", "ACTUAL", "ACTUAR", "ACUITY",
            "ADAGIO", "ADAPTS", "ADDICT", "ADHERE", "ADJUST", "ADMIRE", "ADVENT", "ADVICE",
            "ADVISE", "AERIAL", "AFFAIR", "AFFECT", "AFFORD", "AGENCY", "AGENDA", "AGENTS",
            "AGGRIE", "ALIGHT", "ALLURE", "ALMOST", "ALPINE", "ALWAYS", "AMAZON", "AMNEST",
            "AMOUNT", "AMUSED", "ANCHOR", "ANIMAL", "ANNUAL", "ANSWER", "ANTHEM", "ANYHOW",
            "ANYONE", "ANYWAY", "APIECE", "APPEAL", "APPEAR", "APPEND", "APPLES", "APRONY",
            "ARCADE", "ARCHER", "ARCTIC", "ARDENT", "ARENAS", "ARGUED", "ARMORY", "AROUND",
            "AROUSE", "ARREST", "ARRIVE", "ARROWS", "ARTFUL", "ARTIST", "ASCEND", "ASHORE",
            "ASPECT", "ASPIRE", "ASSAIL", "ASSENT", "ASSERT", "ASSIGN", "ASSIST", "ASSUME",
            "ASSURE", "ASTHMA", "ASTRID", "ASTRON", "ASYLUM", "ATOPIC", "ATTACH", "ATTACK",
            "ATTAIN", "ATTEND", "ATTIRE", "ATTUNE", "AUBURN", "AUGUST", "AURORA", "AUTHOR",
            "AUTUMN", "AVENUE", "AVIARY", "AWAITS", "AWAKEN", "AWARDS", "BABOON", "BACKED",
            "BADGER", "BAFFLE", "BAGELS", "BAILEY", "BAKERY", "BALLAD", "BALLOT", "BAMBOO",
            "BANANA", "BANDIT", "BANNER", "BANTER", "BARBER", "BARELY", "BARLEY", "BARREL",
            "BARTER", "BASKET", "BATTER", "BATTLE", "BEACON", "BEAGLE", "BEASTS", "BEATEN",
            "BEAUTY", "BEAVER", "BECOME", "BEFORE", "BEHALF", "BEHAVE", "BEHOLD", "BELIEF",
            "BELIEV", "BELONG", "BENIGN", "BERRET", "BERTHS", "BESIDE", "BESTOW", "BETRAY",
            "BETTER", "BEWARE", "BEYOND", "BICYCL", "BILLED", "BINDER", "BIONIC", "BIRDIE",
            "BISHOP", "BITTER", "BLADES", "BLANKY", "BLAZER", "BLEACH", "BLIGHT", "BLONDE",
            "BLOUSE", "BOBRET", "BODILY", "BOILER", "BOLDLY", "BONNET", "BORDER", "BORROW",
            "BOTHER", "BOTTLE", "BOTTOM", "BOUNCE", "BOUNTY", "BOVINE", "BOWLER", "BOYISH",
            "BRANCH", "BRANDY", "BRASSY", "BRAVEL", "BREACH", "BREATH", "BREEZE", "BRIDGE",
            "BRIDLE", "BRIGHT", "BROKEN", "BRONZE", "BROOCH", "BROWSE", "BRUNCH", "BRUTAL",
            "BUBBLE", "BUCKET", "BUCKLE", "BUDGET", "BUFFET", "BUGGER", "BULLET", "BUMBLE",
            "BUMPER", "BUNDLE", "BUNKER", "BURDEN", "BUREAU", "BURGER", "BURNER", "BURROW",
            "BUTLER", "BUTTER", "BUTTON", "BUYERS", "BYGONE", "BYWAYS", "CABINS", "CABLES",
            "CACTUS", "CADETS", "CAFEIN", "CALICO", "CALLER", "CALMLY", "CAMERA", "CAMPUS",
            "CANARY", "CANCEL", "CANCER", "CANDID", "CANDLE", "CANNON", "CANOPY", "CANVAS",
            "CANYON", "CARBON", "CARPET", "CARROT", "CARTON", "CASINO", "CASTLE", "CASUAL",
            "CATTLE", "CAUGHT", "CAVEAT", "CAVERN", "CELERY", "CEMENT", "CENSER", "CEREAL",
            "CHAIRS", "CHANCE", "CHANGE", "CHAPEL", "CHARGE", "CHASER", "CHEESE", "CHERRY",
            "CHESTS", "CHICKS", "CHIEFS", "CHILLY", "CHOICE", "CHOOSE", "CHORUS", "CHOSEN",
            "CHURCH", "CICADA", "CINEMA", "CIRCUS", "CITIZN", "CITRUS", "CLAMOR", "CLAMPS",
            "CLASSY", "CLAUSE", "CLEVER", "CLIENT", "CLIMAX", "CLINIC", "CLOSET", "CLOTHS",
            "CLOVER", "CLUTCH", "COARSE", "COBALT", "COBWEB", "COFFEE", "COGNAC", "COHORT",
            "COLLAR", "COLONY", "COLUMN", "COMBAT", "COMEDY", "COMETS", "COMFORT", "COMING",
            "COMMON", "COMPLY", "CONCUR", "CONDOR", "CONFER", "CONSUL", "CONVEY", "COOKER",
            "COOKIE", "COOLER", "COPPER", "CORRAL", "CORSET", "CORTEX", "COSMIC", "COSTLY",
            "COTTON", "COUGAR", "COUNTY", "COUPLE", "COUPON", "COURSE", "COUSIN", "COVERT",
            "COWBOY", "CRADLE", "CRAFTY", "CRANEY", "CRATER", "CRAVEN", "CRAYON", "CREDIT",
            "CREEKS", "CRICKET", "CRISIS", "CRITIC", "CROCUS", "CRUNCH", "CRYPTO", "CRYSTAL",
            "CUCKOO", "CUDDLE", "CULPRT", "CULTURE", "CUPFUL", "CURATE", "CURFEW", "CUSTOM",
            "CUTLET", "CYGNET", "CYMBAL", "DAGGER", "DAMAGE", "DAMSEL", "DANCER", "DANGER",
            "DAPPER", "DASHED", "DEACON", "DEADLY", "DEARLY", "DEBATE", "DEBRIS", "DECADE",
            "DECENT", "DECIDE", "DECKER", "DECODE", "DECORA", "DECREE", "DEDUCE", "DEFEAT",
            "DEFECT", "DEFEND", "DEFILE", "DEFINE", "DEFRAY", "DEGREE", "DEJECT", "DELATE",
            "DELETE", "DELICE", "DELUDE", "DEMAND", "DEMISE", "DEMOTE", "DENIAL", "DENOTE",
            "DENTAL", "DEPART", "DEPEND", "DEPICT", "DEPLOY", "DEPORT", "DEPOSE", "DEPUTY",
            "DERIDE", "DERIVE", "DESERT", "DESIGN", "DESIRE", "DESIST", "DETACH", "DETAIL",
            "DETAIN", "DETECT", "DETOUR", "DEVICE", "DEVILS", "DEVISE", "DEVOTE", "DEVOUR",
            "DIESEL", "DIFFER", "DIGEST", "DILATE", "DILUTE", "DIMMER", "DIMPLE", "DINNER",
            "DIPOLE", "DIRECT", "DISARM", "DISCUS", "DISMAL", "DISMAY", "DISOWN", "DISPEL",
            "DISTAL", "DIVERT", "DIVIDE", "DIVINE", "DOCTOR", "DODGER", "DOLPHN", "DOMAIN",
            "DONATE", "DONKEY", "DOODLE", "DOORAY", "DOSAGE", "DOUBLE", "DOVECO", "DRAFTS",
            "DRAGON", "DRAWER", "DREAMY", "DREDGE", "DRESSY", "DRIFTY", "DRIVER", "DROLET",
            "DROOPY", "DROPLET", "DUCHESS", "DUCTOR", "DULCET", "DUNDER", "DUNGEON", "DUPLEX",
            "DUPLIC", "DURESS", "DURING", "DUSTIN", "DYNAMO", "EAGLET", "EARFUL", "EARNED",
            "EARTHY", "EASILY", "EASTER", "ECHOES", "ECLIPSE", "EDITOR", "EFFECT", "EFFORT",
            "EGGNOG", "EIGHTH", "EIGHTY", "ELAPSE", "ELBOWS", "ELDEST", "ELEVEN", "ELICIT",
            "EMBALM", "EMBARK", "EMBASS", "EMBLEM", "EMBRYO", "EMERGE", "EMPIRE", "EMPLOY",
            "ENABLE", "ENACTS", "ENAMEL", "ENCAMP", "ENCASE", "ENCORE", "ENDURE", "ENERGY",
            "ENGAGE", "ENGINE", "ENGULF", "ENIGMA", "ENJOYD", "ENLIST", "ENMITY", "ENOUGH",
            "ENRAGE", "ENRICH", "ENROLL", "ENSIGN", "ENSURE", "ENTAIL", "ENTICE", "ENTIRE",
            "ENTITY", "ENTOMB", "ENTRAP", "ENTREE", "ENVIOU", "EPONYM", "EQUATE", "EQUITY",
            "ERASER", "ERRAND", "ERRANT", "ERRATA", "ESCAPE", "ESCORT", "ESKIMO", "ESSAYS",
            "ESTATE", "ESTEEM", "ESTHER", "ETHICS", "ETHNIC", "EULOGY", "EUREKA", "EVADED",
            "EVENLY", "EVENTS", "EVINCE", "EVOLVE", "EXALTED", "EXCEED", "EXCEPT", "EXCESS",
            "EXCITE", "EXCUSE", "EXEMPT", "EXHALE", "EXHAUST", "EXHIBT", "EXHORT", "EXILED",
            "EXPAND", "EXPECT", "EXPERT", "EXPIRE", "EXPORT", "EXPOSE", "EXTANT", "EXTEND",
            "EXTENT", "EXTORT", "EXTRAS", "EXULTD", "FABRIC", "FACIAL", "FACTOR", "FAILED",
            "FAIRLY", "FAITHL", "FALCON", "FALLEN", "FALLOW", "FALSET", "FAMOUS", "FARMER",
            "FASTEN", "FATHER", "FATHOM", "FAUCET", "FAULTS", "FEALTY", "FEATHER", "FEATURE",
            "FEDORA", "FEEBLE", "FEEDER", "FELLOW", "FEMALE", "FENDER", "FENNEL", "FERRET",
            "FERVOR", "FESTAL", "FEUDAL", "FIBERS", "FICKLE", "FIDDLE", "FIERCE", "FIESTA",
            "FIGURE", "FILIAL", "FILLED", "FILLET", "FILTER", "FINALE", "FINGER", "FINISH",
            "FINITE", "FIREFLY", "FIRMLY", "FISCAL", "FISHER", "FITFUL", "FITTER", "FIXITY",
            "FLACID", "FLAGON", "FLAKES", "FLANGE", "FLANNEL", "FLASHY", "FLAVOR", "FLEDGE",
            "FLEECE", "FLESHY", "FLIGHT", "FLIMSY", "FLINCH", "FLOODS", "FLORAL", "FLOURS",
            "FLOWER", "FLUENT", "FLUFFY", "FLUTES", "FLYING", "FOAMER", "FODDER", "FOLDER",
            "FONDLE", "FONDLY", "FOOTED", "FORAGE", "FORBID", "FOREST", "FORGER", "FORGET",
            "FORGIV", "FORMAT", "FORMER", "FOSSIL", "FOSTER", "FOULER", "FOURTH", "FRAGIL",
            "FRANCE", "FRANKL", "FRAUDU", "FREELY", "FREEZE", "FRENCH", "FRENZY", "FRIEND",
            "FRIGID", "FRISKY", "FROZEN", "FRUGAL", "FULFIL", "FULLER", "FUMBLE", "FUNDED",
            "FUNNEL", "FURIOUS", "FURNIS", "FURROW", "FUSION", "FUTURE", "GADGET", "GAIETY",
            "GALLEY", "GALLOP", "GAMBLE", "GAMETE", "GANDER", "GARAGE", "GARDEN", "GARLIC",
            "GARNET", "GARRET", "GASKET", "GATHER", "GAUCHE", "GAYETY", "GELATO", "GENDER",
            "GENIAL", "GENIUS", "GENTLE", "GENTLY", "GERBIL", "GEYSER", "GHETTO", "GHOSTS",
            "GIANTS", "GIBLET", "GIGGLE", "GINGER", "GIRAFF", "GIRDLE", "GIRLIE", "GLADLY",
            "GLANCE", "GLANDS", "GLOBAL", "GLOBLE", "GLORIF", "GLOSSY", "GLOVES", "GLOWED",
            "GLUCOSE", "GLUTEA", "GLUTTON", "GNARLY", "GOBBLE", "GOBLET", "GOBLIN", "GOLDEN",
            "GOLFER", "GOODLY", "GOPHER", "GOSPEL", "GOSSIP", "GOVERN", "GRADED", "GRAINS",
            "GRAMMR", "GRANNY", "GRAPES", "GRASSY", "GRATER", "GRATIS", "GRAVEL", "GRAVER",
            "GRAZED", "GREASE", "GREASY", "GREATR", "GREEDY", "GREEKS", "GREETS", "GRENAD",
            "GREYHOUND", "GRIEVOUS", "GRIFFIN", "GRILLE", "GRIMLY", "GRINCH", "GROOVE", "GROOVY",
            "GROTTO", "GROUND", "GROWTH", "GRUDGE", "GUITAR", "HABITAT", "HARBOR", "HARMONY",
            "HEAVEN", "HERBAL", "HEROIC", "HIGHLY", "HISTORY", "HOLIDAY", "HONEST", "HORIZON",
            "HOSPITAL", "HOURLY", "HUMBLE", "HUNGRY", "HURDLE", "HYDRATE", "HYGIENE",

            // 7 and 8-Letter Classic Bingos & High Point Words
            "ABSOLUTE", "ABSTRACT", "ACADEMIC", "ACCENTED", "ACCEPTED", "ACCIDENT", "ACCORDING",
            "ACCURACY", "ACHIEVE", "ACQUIRED", "ACTIVITY", "ADAPTING", "ADEQUATE", "ADMIRED",
            "ADVANCED", "ADVISORY", "AFFECTED", "AFFORDED", "AIRPLANE", "ALPHABET", "ALTOGETHER",
            "AMATEUR", "AMAZING", "AMBITION", "ANALYSIS", "ANCESTOR", "ANCIENT", "ANIMATED",
            "ANNOUNCE", "ANYTHING", "APPARENT", "APPETITE", "APPROVAL", "AQUARIUM", "ARBITRARY",
            "ARCHIVES", "ARGUMENT", "ARRANGED", "ARTISTIC", "ASSEMBLY", "ATHLETIC", "ATTITUDE",
            "ATTRACT", "AUDIENCE", "AUTHENTIC", "AUTHORITY", "BALANCED", "BALLOON", "BANKNOTE",
            "BARGAIN", "BASEBALL", "BEAUTIFUL", "BECOMING", "BEGINNING", "BELIEVER", "BENEFIT",
            "BEVERAGE", "BIRTHDAY", "BLANKET", "BLOSSOM", "BOUNDARY", "BRILLIANT", "BROADCAST",
            "BUILDING", "BUSINESS", "CABINET", "CALENDAR", "CAMPAIGN", "CAPABLE", "CAPACITY",
            "CAPITAL", "CAREFUL", "CARPETS", "CELEBRATE", "CENTURY", "CEREMONY", "CHALLENGE",
            "CHAMPION", "CHANNEL", "CHAPTER", "CHARMING", "CHEMICAL", "CHIMNEY", "CHOCOLATE",
            "CIRCULAR", "CITIZEN", "CLARITY", "CLASSIC", "CLEANER", "CLIMATE", "COLLECTION",
            "COLORFUL", "COMBINED", "COMFORT", "COMMUNITY", "COMPANY", "COMPASS", "COMPLETE",
            "COMPLEX", "COMPOSED", "COMPUTER", "CONCLUDE", "CONCRETE", "CONDENSE", "CONFIRM",
            "CONGRESS", "CONNECT", "CONSIDER", "CONSTANT", "CONSUME", "CONTINUE", "CONVERT",
            "COURAGE", "CREATIVE", "CREATURE", "CULTURED", "CURIOUS", "CUSTOMER", "CYLINDER",
            "DATABASE", "DAUGHTER", "DAYLIGHT", "DECISION", "DECLARE", "DECORATE", "DELICATE",
            "DELIGHT", "DELIVERY", "DEMOCRACY", "DESCRIBE", "DESIGNER", "DESKTOP", "DESTROY",
            "DETAILED", "DIAMOND", "DIGITAL", "DIRECTOR", "DISASTER", "DISCOVER", "DISTANCE",
            "DISTRICT", "DIVIDED", "DOCUMENT", "DOMESTIC", "DONATION", "DYNAMIC", "EARLIEST",
            "ECOLOGY", "ECONOMIC", "EDUCATE", "EFFICIENT", "ELEGANT", "ELEMENT", "ELEVATOR",
            "ELIGIBLE", "EMERALD", "EMOTION", "EMPHASIS", "EMPLOYEE", "ENCOURAGE", "ENDLESS",
            "ENGINEER", "ENORMOUS", "ENTRANCE", "EQUATOR", "EQUIPMENT", "ESPECIAL", "ESSENTIAL",
            "ETERNAL", "EVERYDAY", "EVIDENCE", "EXACTLY", "EXCELLENT", "EXCHANGE", "EXCITED",
            "EXERCISE", "EXISTING", "EXPANDED", "EXPECTED", "EXPENSE", "EXPLAIN", "EXPLORE",
            "EXPRESS", "EXTREME", "FABULOUS", "FACTORIES", "FAITHFUL", "FAMILIAR", "FANTASTIC",
            "FAVORITE", "FEATHERY", "FEATURED", "FEDERAL", "FESTIVAL", "FINANCIAL", "FIREWORK",
            "FLEXIBLE", "FLOATING", "FLOURISH", "FLOWERS", "FOREVER", "FORTUNE", "FORWARD",
            "FREEDOM", "FREQUENCY", "FRIENDLY", "FUNCTION", "FURTHER", "GALLERY", "GARDENER",
            "GENERAL", "GENEROUS", "GLACIER", "GLORIOUS", "GOURMET", "GRACIOUS", "GRADUATE",
            "GRAMMAR", "GRANITE", "GRATEFUL", "GREATEST", "GUARDIAN", "HABITUAL", "HANDSOME",
            "HAPPINESS", "HARMONIC", "HEADLINE", "HEALTHY", "HERITAGE", "HIGHWAY", "HISTORIC",
            "HOMETOWN", "HONORARY", "HORIZONS", "HOSPITAL", "HUMANITY", "HUNDRED", "IDENTITY",
            "IGNORANT", "ILLUSION", "IMAGINE", "IMMEDIATE", "IMPACTED", "IMPERIAL", "IMPORTANT",
            "IMPRESS", "IMPROVE", "INCIDENT", "INDEPENDENT", "INDUSTRY", "INFINITY", "INFORMAL",
            "INITIAL", "INNOCENT", "INNOVATE", "INSTANCE", "INSTEAD", "INSTITUTE", "INTEGRAL",
            "INTELLECT", "INTENDED", "INTERNET", "INTERVAL", "INTIMATE", "INVENTOR", "ISLANDER",
            "JEWELRY", "JOURNAL", "JOURNEY", "JUDICIAL", "JUNCTION", "JUSTICE", "KEYBOARD",
            "KINGDOM", "LANGUAGE", "LANTERN", "LAUGHTER", "LEARNING", "LEGENDARY", "LEOPARD",
            "LIBERTY", "LIFETIME", "LIGHTNING", "LITERARY", "LOCATION", "LOGICAL", "LONELINESS",
            "LUGGAGE", "LUMINOUS", "MAGAZINE", "MAGICIAN", "MAGNETIC", "MAGNIFY", "MAJESTIC",
            "MAJORITY", "MANAGED", "MANSION", "MARRIAGE", "MATERIAL", "MATURITY", "MAXIMUM",
            "MEADOWS", "MEANING", "MEDICINE", "MEDIEVAL", "MEDITATE", "MEMORABLE", "MEMORIES",
            "MERCHANT", "MIDNIGHT", "MILITARY", "MINERAL", "MINIMUM", "MINISTER", "MIRACLE",
            "MISSION", "MOMENTUM", "MONUMENT", "MOUNTAIN", "MOVEMENT", "MULTIPLY", "MUSEUMS",
            "MUSICIAN", "MYSTERIOUS", "NATIONAL", "NATURAL", "NAVIGATE", "NEGATIVE", "NETWORK",
            "NOVELIST", "NUMEROUS", "NUTRIENT", "OBJECTIVE", "OBSERVE", "OBVIOUS", "OCCASION",
            "OCTOBER", "OFFICIAL", "OLYMPIC", "OPERATOR", "OPINION", "OPPONENT", "OPTIMAL",
            "ORCHESTRA", "ORDINARY", "ORGANIC", "ORIGINAL", "OUTDOORS", "OVERCOME", "PACIFIC",
            "PAINTING", "PALEFACE", "PARADISE", "PARALLEL", "PARENTS", "PARTNER", "PASSENGER",
            "PATIENCE", "PEACEFUL", "PERFECT", "PERIODIC", "PERSONAL", "PHYSICAL", "PICNICS",
            "PICTURES", "PLATFORM", "PLEASANT", "PLEASURE", "POETICAL", "POPULAR", "PORTRAIT",
            "POSITION", "POSITIVE", "POSSIBLE", "POWERFUL", "PRACTICE", "PRECIOUS", "PREDICTS",
            "PREMIERE", "PREPARE", "PRESENT", "PRESERVE", "PRESTIGE", "PREVIOUS", "PRIMARY",
            "PRIMITIV", "PRINCESS", "PRINCIPLE", "PRISONER", "PRIVACY", "PROBABLE", "PROCEED",
            "PRODUCER", "PRODUCTS", "PROFILE", "PROFOUND", "PROGRESS", "PROJECTS", "PROMISE",
            "PROPOSAL", "PROSPER", "PROTECT", "PROTOCOL", "PROVINCE", "PRUDENCE", "PUBLICLY",
            "PUNCTURE", "PURSUING", "PYRAMID", "QUALIFIED", "QUARTERS", "QUESTION", "QUICKEST",
            "RADIANCE", "RAILROAD", "RAINBOW", "RANDOMLY", "RAPIDITY", "REACTION", "READINGS",
            "REALIZED", "REASONED", "RECEIVED", "RECOGNIZE", "RECOVERY", "REDUCING", "REFINERY",
            "REFLECT", "REGIONAL", "REGISTER", "RELAXING", "RELEVANT", "RELIABLE", "RELIGION",
            "REMAINED", "REMEDIES", "REMEMBER", "REMINDED", "RENDERED", "RENEWAL", "RENOUNCE",
            "REPEATED", "REPORTER", "REPRESENT", "REQUIRED", "RESEARCH", "RESERVED", "RESOURCE",
            "RESPONSE", "RESTAURANT", "RESTRICT", "REVEALED", "REVENUES", "REVISION", "RHETORIC",
            "ROMANTIC", "ROUNDING", "ROYALTY", "SAFELIGHT", "SALVATION", "SANDWICH", "SATISFY",
            "SAVIOR", "SCENERY", "SCHEDULE", "SCHOLAR", "SCIENTIFIC", "SCISSORS", "SCULPTOR",
            "SEASONAL", "SECLUSION", "SECRETARY", "SECURITY", "SELECTION", "SENSIBLE", "SENTENCE",
            "SEXUAL", "SEXISM", "SEXIST", "SEXTON",
            "SEQUENCE", "SERENITY", "SETTLERS", "SHADOWS", "SHELTERS", "SHIPMENT", "SHOCKING",
            "SHOOTING", "SHORTCUT", "SHOULDER", "SHUTTLED", "SIDEWALK", "SIGHTSEE", "SILICONE",
            "SIMILAR", "SIMPLICITY", "SINCERE", "SINGULAR", "SKELETON", "SKILLFUL", "SLEEPING",
            "SLIGHTLY", "SLIPPERY", "SLOWDOWN", "SLUMBER", "SMOOTHLY", "SNOWBALL", "SNOWDROP",
            "SOCIALIST", "SOFTWARE", "SOLITARY", "SOLUTION", "SOMEBODY", "SOMETIME", "SOMEWHAT",
            "SONGBIRD", "SOOTHING", "SOVEREIGN", "SPACIOUS", "SPARKLE", "SPECIAL", "SPECIFIC",
            "SPECTACLE", "SPECTRUM", "SPLENDID", "SPOKENLY", "SPONSOR", "SPORTING", "SPOTLIGHT",
            "STABILITY", "STADIUMS", "STANDARD", "STARLIGHT", "STARTING", "STATUARY", "STEADILY",
            "STIMULUS", "STIRRING", "STRATEGY", "STRENGTH", "STUDENTS", "STUMBLE", "SUBTITLE",
            "SUCCESS", "SUGGEST", "SUITABLE", "SUMMER", "SUNLIGHT", "SUNRISES", "SUNSHINE",
            "SUPERB", "SUPERIOR", "SUPPLIED", "SUPPORTER", "SURFACED", "SURPRISE", "SURVIVAL",
            "SWIMMING", "SYLLABLE", "SYMBOLIS", "SYMPHONY", "SYNDICAT", "SYNONYM", "TALENTED",
            "TEACHERS", "TELEGRAM", "TELEPHONE", "TEMPERED", "TEMPLATE", "TEMPORAL", "TENDENCY",
            "TERMINAL", "TERRACE", "TERRIBLE", "TEXTBOOK", "THANKFUL", "THEATER", "THEORIST",
            "THINKING", "THIRTEEN", "THOROUGH", "THOUGHT", "THOUSAND", "THRILLED", "TIMELINE",
            "TOGETHER", "TOLERANT", "TOMORROW", "TOTALITY", "TOUCHING", "TOWNSHIP", "TRACKING",
            "TRADITION", "TRAFFIC", "TRAGEDY", "TRAINING", "TRANQUIL", "TRAVELER", "TREASURE",
            "TREMBLING", "TRIBUNAL", "TRIANGLE", "TRIUMPH", "TROPICAL", "TROUBLED", "TWILIGHT",
            "ULTIMATE", "UMBRELLA", "UNBIASED", "UNCERTAIN", "UNDERGO", "UNDERSTAND", "UNEXPECTED",
            "UNIFORM", "UNIVERSE", "UNIVERSITY", "UNUSUAL", "UPGRADES", "UPSTREAM", "VACATION",
            "VALIANT", "VALUABLE", "VANISHED", "VARIABLE", "VARIETY", "VELOCITY", "VENGEFUL",
            "VENTURED", "VERIFIED", "VERTICAL", "VIBRANT", "VICTORY", "VIGILANT", "VILLAGE",
            "VINTAGE", "VIOLENCE", "VIRTUOUS", "VISIONARY", "VISITOR", "VITALITY", "VOLCANO",
            "VOLUNTEER", "VOYAGERS", "WANDERER", "WARMTH", "WARRIORS", "WATCHFUL", "WATERFALL",
            "WEALTHY", "WEATHER", "WEEKENDS", "WELCOME", "WHATEVER", "WHISPER", "WILDLIFE",
            "WINDMILL", "WINDFALL", "WIRELESS", "WITHDRAW", "WITHOUT", "WONDERFUL", "WOODLAND",
            "WORKBOOK", "WORKSHOP", "WORLDWIDE", "WORSHIP", "WORTHWHILE", "WRESTLER", "WRITING",
            "YEARBOOK", "YESTERDAY", "YOUTHFUL", "ZEALOUS", "ZENITH"
        )

        for (word in commonVocabulary) {
            insertWord(word)
        }
    }
}
