package com.bestfriends.beachbingo.feature.raetsel

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.min

// ── Typen ─────────────────────────────────────────────────────────────────────

enum class WwLetterStatus { CORRECT, PRESENT, ABSENT, EMPTY, TYPING }

data class WwDifficultyConfig(
    val wordLength: Int,
    val maxGuesses: Int,
    val hardMode: Boolean,
    val label: String,
    val description: String,
)

val WW_CONFIG: Map<String, WwDifficultyConfig> = mapOf(
    "leicht"  to WwDifficultyConfig(4, 7, false, "Leicht",  "4 Buchstaben · 7 Versuche"),
    "mittel"  to WwDifficultyConfig(5, 6, false, "Mittel",  "5 Buchstaben · 6 Versuche"),
    "schwer"  to WwDifficultyConfig(5, 5, false, "Schwer",  "5 Buchstaben · 5 Versuche"),
    "experte" to WwDifficultyConfig(6, 5, true,  "Experte", "6 Buchstaben · 5 Versuche · Hard Mode"),
)

val WW_DIFFICULTIES = listOf("leicht", "mittel", "schwer", "experte")

data class WwStats(
    val played: Int = 0,
    val won: Int = 0,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val distribution: MutableList<Int> = mutableListOf(),
    val lastPlayedDate: String = "",
    val lastDailyDate: String = "",
    val dailyPlayed: Int = 0,
    val dailyWon: Int = 0,
    val dailyCurrentStreak: Int = 0,
    val dailyMaxStreak: Int = 0,
    val dailyDistribution: MutableList<Int> = mutableListOf(),
)

data class WwInitState(
    val targetWord: String,
    val guesses: List<String>,
    val currentInput: String,
    val gameStatus: String,
    val elapsedSeconds: Int,
)

// ── Wortlisten ─────────────────────────────────────────────────────────────────

private val WORDS_4_TARGET: List<String> = listOf(
    "HAUS","BIER","BUCH","GLAS","BAUM","BOOT","DACH","FELS","FLUT","GRAS",
    "HAHN","HOLZ","HUND","KÄSE","KORB","LOCH","LÖWE","MEER","MOND","MOOS",
    "NETZ","OBST","PFAD","RAND","RING","ROHR","SAAL","SAFT","SAND","SEIL",
    "SOFA","SOHN","TEER","TIER","TUCH","TURM","WAND","WEIN","WELT","WURM",
    "WALD","ZEIT","ZELT","MÖWE","DÜNE","TANG","WATT","STEG","KAHN","GOLD",
    "HORN","KRUG","LAND","LAUB","MAUS","MEHL","NEST","NUSS","RABE","ROSE",
    "ROST","RUND","RUTE","SIEG","TAKT","TANZ","TANK","VASE","WERK","WEHR",
    "ZAHN","ZEUG","ZINK","ZINN","ZOLL","REIF","REIS","RAST","RAUM","MAHL",
    "LIED","KEIL","KERN","KINN","GANG","GARN","GAST","FILM","DUFT","DOSE",
    "DORF","BOCK","ARZT","ALGE","ADER","AFFE","BALL","BAND","BORD","BROT",
    "BURG","DORN","EBBE","ERBE","ERDE","FACH","FARN","FEST","FLOH","FLUG",
    "FORM","GIFT","GLUT","GOLF","GRAB","GURT","HAFF","HARM","HECK","HEMD",
    "HERD","HOSE","HÖHE","HUHN","JADE","JOCH","KALK","LAGE","LÄRM","LAUF",
    "LECK","LEHM","LEID","LEIM","LOHN","LUST","MARK","MAST","MOOR","MORD",
    "NAHT","NARR","OPER","PAAR","PLAN","RANG","RIFF","RIND","ROCK","RUHE",
    "RUNE","SALZ","SANG","SATZ","SOLD","UFER","URNE","VIEH","VIER","VOLK",
    "VOLT","WECK","WEST","KIES","KLEE","KNIE","KOCH","LORE","MATT","GULP",
    "HAUE","ESSE","ELLE","ÄHRE","ATEM","BACH","BISS","CAFE","JADE","SPAT",
)

private val WORDS_4_GUESS_EXTRA: List<String> = listOf(
    "ACHT","ALLE","ALSO","AMOK","ANNO","ARME","AUGE","AUTO","BARE","BAFF",
    "EBEN","ECKE","EGAL","ELAN","ELCH","ELFE","EPOS","ERST","EURO","EWIG",
    "FEIN","FETT","FIEL","FREI","FRON","FUGE","FUNK",
    "GEIL","GIPS","GROB","GROG","GUSS","GUTE","HAAR","HAFT","HAST",
    "HEHL","HEIL","HEIM","HELD","HELL","HERR","HIEB","HOCH",
    "HOLD","HORT","JAGT","JEDE","JENE","KAHL","KALT",
    "KARG","KEHR","KERL","KLAR","KLUB","KOKS","KOPF",
    "KOST","KÜHL","KÜHN","LAST","LAUT","LEER","LEIB","LENZ","LESE",
    "LOGE","LOST","LUFT","LUMP","MADE",
    "MANN","MILD","MIST","MODE","MUND",
    "NABE","NACH","NASE","NETT",
    "NORD","NORM","OASE","ODER","OFEN","OHNE","OPAL","PACK",
    "PAKT","PASS","PEST","PIER","PILS","POCH","POSE","PULK","PULS",
    "RAPS","RAUB","RAUE","RECK","REDE","REGE",
    "RIES","RISK","RITZ","ROSS","RUBE","RÜCK",
    "RUSS","SAFE","SAGE","SAME","SARG","SAUM",
    "SEHN","SEHR","SEID","SEIN","SEKT","SINN","SOLL","SPUK","SPUR","STAB","STAR",
    "TAGE","TAKT","TALG","TEST","TIEF","TIPP",
    "TORF","TRAB","TRAN","TREU","TROG","TÜLL",
    "ULME","UNKE","VELO",
    "VIEL","VISA","WABE","WADE","WAGE","WARM","WART","WEIS",
    "WERG","YARD","ZEHN","ZORN",
)

private val WORDS_5_TARGET: List<String> = listOf(
    "STEIN","NACHT","LEBEN","LIEBE","JUNGE","KRANK","TISCH","FISCH","STUHL",
    "BRIEF","SCHON","IMMER","TIERE","KRONE","HÄUSE","SCHAU","HEISS","BODEN",
    "ABEND","AHORN","ALARM","ANGEL","ANGST","ANKER","ATLAS","ATOLL","AUTOR",
    "BRAND","BRISE","BUCHT","BUSEN","DEICH","DELTA","DRUCK","DÜNEN","EBENE",
    "EIMER","EISEN","ENGEL","ERNTE","FADEN","FAHNE","FALKE","FARBE","FASER",
    "FEUER","FJORD","FLECK","FLORA","FLUSS","FORST","FROST","FUCHS","GABEL",
    "GASSE","GEIST","GESTE","GLANZ","GLEIS","GLÜCK","GRUBE","GRUND","HAFEN",
    "HAKEN","HARFE","HEIDE","HIRTE","HÖHLE","HÜGEL","HÜTTE","INSEL","JACKE",
    "JÄGER","KABEL","KANAL","KANTE","KARTE","KATZE","KEGEL","KETTE","KLANG",
    "KLIFF","KLOTZ","KNALL","KNOPF","KOBRA","KRAKE","KRÄHE","KREBS","KREUZ",
    "KRISE","KÜSTE","LACHS","LAUNE","LEDER","LEHRE","LICHT","LINIE","LINSE",
    "LISTE","LITER","LOTSE","LUNGE","MAGMA","MÄHNE","MARKT","MATTE","MEILE",
    "MESSE","MILCH","MINZE","MÖBEL","MÖHRE","MOTTE","MULDE","MÜHLE","NABEL",
    "NADEL","NARBE","NEBEL","NEFFE","NIERE","NOTEN","OCHSE","OTTER","PANDA",
    "PAUKE","PERLE","PFAHL","PFEIL","PFERD","PISTE","PLATZ","PREIS","PROBE",
    "QUARK","RACHE","RATTE","REGEN","REIHE","REISE","RIESE","RINDE","RUDER",
    "SAITE","SALAT","SALON","SÄULE","SCHAF","SCHAL","SCHUH","SEGEL","SONDE",
    "SPATZ","SPIEL","SPORT","STAAT","STAHL","STAMM","STAND","STAUB","STERN",
    "STICH","STIEL","STIRN","STOCK","STROM","STUBE","STÜCK","STURM","STUTE",
    "SUPPE","TAFEL","TANNE","TAUBE","TIGER","TINTE","TRAUM","TREUE","TRICK",
    "TRITT","TRUHE","TULPE","ÜBUNG","VATER","VENUS","VOGEL","WAFFE","WALZE",
    "WANNE","WANZE","WARZE","WELLE","WENDE","WITWE","WOLKE","WÜRZE","WURST",
    "ZANGE","ZEILE","ZINNE","ZUCHT","ZWECK","BRUCH","DRAHT","DRANG","GROLL",
    "GRIFF","HAUCH","HEFTE","KEHLE","KEULE","KIEME","KOGGE","KRÖTE","LAICH",
    "LERCH","LUCHS","SÄBEL","SPREU","SPALT","STANK","STARK","STOPP","STEIL",
    "UMZUG","UNRAT","BÜHNE","TRUPP","FINTE","HOLME","MALVE","ASSEL","LEMUR",
    "TAPIR","VIOLA","VISUM","OCKER","SPORE",
)

private val WORDS_5_GUESS_EXTRA: List<String> = listOf(
    "ABGAS","ADLER","AHNEN","ALAND","ALPEN","AMSEL","ANFUG",
    "ANMUT","ANTIK","APFEL","ARTEN","ASTER","ATMEN","BARKE",
    "BASAR","BELEG","BETON","BIRKE","BLÄUE","BLECH","BLICK","BLOCK","BLUME",
    "BLUSE","BOLZE","BORKE","BOXER","BULLE","BUSSE","CLOWN",
    "DATEI","DATUM","DECKE","DEKAN","DEPOT","DOLCH",
    "DRECK","DROGE","EICHE","EILIG","EINST","ELEND",
    "FALTE","FEIND","FIBEL","FLAIR",
    "FLAUE","FLIRT","FLOSS","FOHLE","FORKE","FOYER","FRAGE",
    "FRECH","FREMD","FUGEN","FÜRST","GAFFE",
    "GAMMA","GARDE","GEBOT","GEIER","GELEE",
    "GENUG","GIESS","GLATT","GNADE","GOLEM",
    "GREIS","GRIFF","GRIMM","GRIND","GROSS",
    "GRUFT","GUMMI","GUSTO","HAFER","HALME",
    "HASEL","HEIST","HELME","HERTZ","HIEBE",
    "HINZU","HIRSE","HOLME","HONIG","HUMUS","HÜPFE","HURRA","HYDRA",
    "IDIOT","IKONE","IRREN","IRRIG",
    "JOKER","JOLLE","JUNGS","KAFFE",
    "KALTE","KAMIN","KAPOK","KARGO","KARST","KASSE",
    "KAUEN","KEHRT","KEILE","KELCH",
    "KERNE","KERZE","KIEPE","KIPPE","KIWIS","KLAGE","KLARE","KLEBE",
    "KLEID","KLEIN","KLOPS","KLUFT","KNABE","KNAST","KNIFF",
    "KOMBI","KOPIE","KORSO","KRAUL",
    "KREIS","KRILL","KRIMI","KRONE","KUPFE","KUREN",
    "LACHE","LACKE","LAHME","LAMPE","LARVE","LATTE","LAUFE",
    "LEBER","LECKT","LEUTE","LIANE","LICHT","LINDE","LIPPE",
    "LOCKE","LODEN","LÜCKE","LUMPE",
    "MAIRE","MALER","MANGE","MARKE","MÄUSE","MECKE",
    "MEERE","MENGE","MEUTE","MIETE","MOLCH","MÜCKE",
    "MUMIE","MÜNZE","MÜTZE","NATUR","NIXIE",
    "OCKER","ORGEL","OZEAN","PAPPE","PATER","PAUSE","PELZE",
    "PFOTE","PILZE","PINIE","PLANE","PLAZA","PLOTZ",
    "POKAL","POLKA","POSSE","PRELL","PRIMA",
    "PUMPE","PUPPE","QUELL","QUALE",
    "RASTE","RECKE","REGEN","REIBE","REMIS","RENNE","RESTE",
    "RETTE","RIEBE","RINNE","RIPPE","RITZE","ROBBE",
    "ROLLE","ROSEN","RÜCKE","RUMPF","RUPFE","SAMEN",
    "SARGE","SAUGE","SCHAL","SCHAM","SCHAR","SEHNE","SEIFE",
    "SENSE","SERVE","SETZE","SIEBE","SINNE","SKALA",
    "SOHLE","SORGE","SPANN","SPARE","SPÄTE","SPECK","SPEER","SPORE",
    "SPOTT","SPUND","STECK","STEGE","STEIN","STICH","STIFT",
    "STOPP","STREU","STUNK","STUMM",
    "TABAK","TAGES","TAUFE","TEMPO","TISCH","TOBEN",
    "TRÄNE","TRECK","TRETE","TRINK","TRUHE",
    "TULPE","TURNE","ÜBEL","ULMER",
    "VARAN","VERSE","VIELE","VIPER","VLIES",
    "WACHT","WAHLE","WAPPE","WEBEN","WEDEL","WEGEN",
    "WEIDE","WEILE","WENIG","WERBE","WINKE",
    "WOLLE","WORTE","WÜSTE","ZÄHNE","ZANKE","ZECHE","ZEDER",
    "ZIELE","ZIMTE","ZOPFE","ZUNGE","ZWANG","ZWECK","ZWEIG",
)

private val WORDS_6_TARGET: List<String> = listOf(
    "ARBEIT","BAMBUS","BRONZE","BRUDER","BRÜCKE","BÜFFEL","BUNKER","BÜRSTE",
    "DELFIN","ELSTER","FALTER","FELSEN","FICHTE","FISCHE","FLAUTE","FLIEGE",
    "FLOSSE","FROSCH","GARTEN","GESANG","GESETZ","GEWEHR","GIPFEL","GROTTE",
    "HAMMER","HELFER","HERZEN","HIRSCH","HÖCKER","HUMMEL","HUNGER","INSELN",
    "KATZEN","KESSEL","KIEFER","KLIPPE","KNECHT","KNOLLE","KOBOLD","KOFFER",
    "KÖRPER","KRABBE","KRÄFTE","KRIPPE","KUTTER","LAGUNE","LERCHE","LÖCHER",
    "MANDEL","MANGEL","MARDER","MISTEL","MÖHREN","MOLCHE","MÖRSER","MÜCKEN",
    "MÜHLEN","MUSCHEL","MUSTER","NATTER","OCHSEN","OSTERN","PALMEN","PANZER",
    "PAPIER","PERLEN","PINSEL","PLATTE","PRANKE","RACHEN","RECHEN","REIHER",
    "RIEMEN","ROBBEN","RÜCKEN","SEUCHE","SILBER","SOMMER","SPEISE","SPOREN",
    "SPROSS","STÄMME","STRAND","STRICH","STUNDE","TAIFUN","TEMPEL","TEUFEL",
    "TRÄGER","TRESOR","URLAUB","VESPER","WAPPEN","WASSER","WEIZEN","WELLEN",
    "WIESEN","WIRBEL","WUNDER","WURZEL","WÜSTEN","ZEIGER","ZIRKEL","ZITHER",
    "ZUCKER","ZUNDER","SALBEI","SCHIFF","SCHNEE","SCHLAF","SCHELM","WALZER",
    "VÖLKER","SATURN","ZOMBIE","PRIMEL","NIMBUS","TAUBEN","TINTEN","FLUTEN",
    "LÜFTEN","BARSCH","ADVENT","AMBOSS","DEHNEN","DROGEN","BRIEFE","ARKADE",
    "DAHLIE","DICHTE","DÜNUNG","EISBÄR","FORMEL","FRAUEN",
    "FRISCH","BLÜTEN","MATTEN","MASERN","BACKEN",
)

private val WORDS_6_GUESS_EXTRA: List<String> = listOf(
    "ABFALL","AKTION","ANKERN","ANTEIL","APFELN",
    "BÄCKER","BAGGER","BALKON","BASTEI","BAUTEN","BECKEN","BEEREN",
    "BELLEN","BEREIT","BIRKEN","BITTER","BLASER","BLAUER",
    "BOCKIG","BORGEN","BORSTE","BRATEN","BREITE","BUHNEN",
    "BÜSCHE","DAMMEN","DARBEN","DAUERN","DECKEL","DENKEN",
    "DIESEL","DIENEN","DONNER","DORNEN","DÖRFER","DRÜCKE","DUNKEL","DÜSTER",
    "EBENEN","EICHEN","EINZEL","ENGELN",
    "FAKTOR","FÄUSTE","FÄHREN","FALLEN","FALTEN","FARBEN","FASERN",
    "FEIGEN","FELDER","FERSEN","FEUERN","FINGER",
    "FLADEN","FLÄCHE","FLUCHT","FOLGEN",
    "FRÖSTE","FRUCHT","FUHREN","FÜLLEN","FUNKEN","FUTTER","GÄNGEN",
    "GARBEN","GARNEN","GATTER","GEBÄCK","GEBIET","GEBÜHR",
    "GEGNER","GEHWEG","GELTEN","GENUSS","GEPÄCK",
    "GERÖLL","GERSTE","GERUCH","GEWALT","GEWAND",
    "GLASER","GLEISE","GLÜHEN","GNADEN","GÖTTER","GRÄSER","GRAUEN",
    "GRENZE","GRIFFE","GRÜBEL","GULDEN","GÜRTEL",
    "HAFTEN","HALLEN","HALTEN","HÄNGEN","HARREN",
    "HASSEN","HAUSEN","HECKEN","HELDEN","HELFEN","HEMMEN",
    "HENKEL","HERDEN","HERREN","HESSEN","HINTEN","HOCKER",
    "HOFFEN","HORTEN","HÜLSEN","IMPFEN","JUNGEN",
    "KAFFER","KÄHNEN","KAPPEN","KÄSTEN","KAUFEN","KELCHE",
    "KETTEN","KILLEN","KINDER","KISTEN","KLÄGER","KLÄREN","KLOPFE",
    "KNACKE","KNARRE","KNOTEN","KOCHEN","KOSTEN","KRALLEN","KRONEN",
    "KÜHLEN","KUNDEN","LAPPEN","LASERN","LAUFEN","LAUGEN",
    "LAUBEN","LAUTEN","LEEREN","LEIERN","LEINEN","LENDEN",
    "LEUCHTE","LICHTEN","LIEBEN","LINIEN","LÖSUNG","LUDERN",
    "MACHEN","MAHNEN","MALERN","MASSEN","MEEREN",
    "MENGEN","MESSEN","MIETEN","MORGEN","MÜNZEN",
    "NARBEN","NETZEN","NIETEN","NORDEN","NUTZER","ÖFFNEN","ORDNEN",
    "PECHEN","PELZEN","PFUNDE","PILGER","PINNEN",
    "PLANEN","POSERN","PUTZEN","RANGEN","RANKEN","RASTEN",
    "RATTEN","RECHTE","REGELN","RENNEN","RINNEN","RÖSTEN",
    "RUDERN","RÜHREN","SALBEN","SANKEN","SAUGEN","SCHAFE",
    "SCHERZ","SCHIFF","SCHIRM","SCHLAF","SELTEN","SENKEN",
    "SINKEN","SIPPEN","SITZEN","SONNEN","SPALTE","SPANNE","SPAREN",
    "SPERRE","SPIELE","SPINNE","STAUNEN","STEGEN","STEINE",
    "STELLT","STEMME","STOFFE","STRECK",
    "STRICH","STÜCKE","STUFEN","STÜRME","SUCHEN","SUMPFE","SURFEN",
    "TAKTIK","TANKEN","TANNEN","TAUCHE","TESTEN","THEMEN","TIPPEN",
    "TÖPFER","TRAGEN","TRAUFE","TREFFEN","TREIBEN","TRETEN","TRUPPE",
    "TÜRME","TURNEN","VÖLLIG","VORRAT","WACHEN","WÄHLEN",
    "WANDEL","WARTEN","WASCHEN","WEDELN","WEINEN","WEISEN","WENDEN",
    "WERFEN","WINDEN","WISSEN","WOHNEN","WÖLBEN","WOLKEN","ZÄHLEN",
    "ZEIGEN","ZIEHEN","ZIELEN","ZIMMER","ZÖGERN","ZOLLEN","ZÜCHTEN",
)

// ── Wortlisten-Zugriff ─────────────────────────────────────────────────────────

fun getWwTargets(difficulty: String): List<String> {
    val len = WW_CONFIG[difficulty]?.wordLength ?: 5
    return when (len) {
        4    -> WORDS_4_TARGET
        5    -> WORDS_5_TARGET
        else -> WORDS_6_TARGET
    }
}

private fun getWwGuessPool(difficulty: String): List<String> {
    val len = WW_CONFIG[difficulty]?.wordLength ?: 5
    return when (len) {
        4    -> WORDS_4_TARGET + WORDS_4_GUESS_EXTRA
        5    -> WORDS_5_TARGET + WORDS_5_GUESS_EXTRA
        else -> WORDS_6_TARGET + WORDS_6_GUESS_EXTRA
    }
}

// ── Kernlogik ─────────────────────────────────────────────────────────────────

fun getDailyWwWord(difficulty: String): Pair<String, String> {
    val dateStr = LocalDate.now().toString()
    var h = 0
    for (c in dateStr) h = h * 31 + c.code
    val offset = when (difficulty) { "leicht" -> 0; "mittel" -> 1000; "schwer" -> 2000; "experte" -> 3000; else -> 0 }
    val targets = getWwTargets(difficulty)
    val idx = (abs(h.toLong() + offset.toLong()) % targets.size).toInt()
    return Pair(targets[idx], dateStr)
}

fun getWwRandomWord(difficulty: String): String {
    val targets = getWwTargets(difficulty)
    return targets[(Math.random() * targets.size).toInt()]
}

fun isValidWwGuess(word: String, difficulty: String): Boolean {
    return word.uppercase() in getWwGuessPool(difficulty)
}

fun computeWwStatuses(guess: String, target: String): List<WwLetterStatus> {
    val g = guess.uppercase()
    val t = target.uppercase()
    val result = MutableList(g.length) { WwLetterStatus.ABSENT }
    val remaining = mutableMapOf<Char, Int>()
    for (i in t.indices) {
        if (g.getOrNull(i) != t[i]) remaining[t[i]] = (remaining[t[i]] ?: 0) + 1
    }
    for (i in g.indices) {
        if (i < t.length && g[i] == t[i]) result[i] = WwLetterStatus.CORRECT
    }
    for (i in g.indices) {
        if (result[i] == WwLetterStatus.CORRECT) continue
        val ch = g[i]
        val rem = remaining[ch] ?: 0
        if (rem > 0) {
            result[i] = WwLetterStatus.PRESENT
            remaining[ch] = rem - 1
        }
    }
    return result
}

fun computeWwKeyStatuses(guesses: List<String>, target: String): Map<Char, WwLetterStatus> {
    val priority = mapOf(
        WwLetterStatus.CORRECT to 3, WwLetterStatus.PRESENT to 2,
        WwLetterStatus.ABSENT to 1, WwLetterStatus.EMPTY to 0, WwLetterStatus.TYPING to 0,
    )
    val result = mutableMapOf<Char, WwLetterStatus>()
    for (guess in guesses) {
        val statuses = computeWwStatuses(guess, target)
        for (i in guess.indices) {
            val ch = guess[i]
            val st = statuses[i]
            val cur = result[ch]
            if (cur == null || (priority[st] ?: 0) > (priority[cur] ?: 0)) result[ch] = st
        }
    }
    return result
}

fun validateWwHardMode(newGuess: String, previousGuesses: List<String>, target: String): String? {
    if (previousGuesses.isEmpty()) return null
    val g = newGuess.uppercase()
    for (prev in previousGuesses) {
        val statuses = computeWwStatuses(prev, target)
        for (i in prev.indices) {
            if (statuses[i] == WwLetterStatus.CORRECT && g.getOrNull(i) != prev[i]) {
                return "Position ${i + 1} muss \"${prev[i]}\" sein (gruener Buchstabe)."
            }
        }
        for (i in prev.indices) {
            if (statuses[i] == WwLetterStatus.PRESENT && !g.contains(prev[i])) {
                return "Das Wort muss den Buchstaben \"${prev[i]}\" enthalten."
            }
        }
    }
    return null
}

// ── State-Serialisierung ───────────────────────────────────────────────────────

fun serializeWwState(targetWord: String, guesses: List<String>, currentInput: String, gameStatus: String): String =
    JSONObject().apply {
        put("targetWord", targetWord)
        put("guesses", JSONArray(guesses))
        put("currentInput", currentInput)
        put("gameStatus", gameStatus)
    }.toString()

fun deserializeWwState(raw: String): WwInitState = try {
    val obj = JSONObject(raw)
    val arr = obj.getJSONArray("guesses")
    WwInitState(
        targetWord    = obj.getString("targetWord"),
        guesses       = (0 until arr.length()).map { arr.getString(it) },
        currentInput  = obj.optString("currentInput", ""),
        gameStatus    = obj.optString("gameStatus", "playing"),
        elapsedSeconds = 0,
    )
} catch (_: Exception) {
    WwInitState("", emptyList(), "", "playing", 0)
}

// ── Statistiken ────────────────────────────────────────────────────────────────

private const val WW_STATS_PREFS = "wortwelle_stats"

private fun makeEmptyWwStats(maxGuesses: Int) = WwStats(
    distribution = MutableList(maxGuesses) { 0 },
    dailyDistribution = MutableList(maxGuesses) { 0 },
)

fun getWwStats(context: Context, difficulty: String): WwStats {
    val maxGuesses = WW_CONFIG[difficulty]?.maxGuesses ?: 6
    val prefs = context.getSharedPreferences(WW_STATS_PREFS, Context.MODE_PRIVATE)
    val raw = prefs.getString(difficulty, null) ?: return makeEmptyWwStats(maxGuesses)
    return try {
        val obj = JSONObject(raw)
        val dist = obj.getJSONArray("distribution")
        val ddist = obj.getJSONArray("dailyDistribution")
        WwStats(
            played              = obj.optInt("played"),
            won                 = obj.optInt("won"),
            currentStreak       = obj.optInt("currentStreak"),
            maxStreak           = obj.optInt("maxStreak"),
            distribution        = MutableList(maxGuesses) { i -> if (i < dist.length()) dist.getInt(i) else 0 },
            lastPlayedDate      = obj.optString("lastPlayedDate", ""),
            lastDailyDate       = obj.optString("lastDailyDate", ""),
            dailyPlayed         = obj.optInt("dailyPlayed"),
            dailyWon            = obj.optInt("dailyWon"),
            dailyCurrentStreak  = obj.optInt("dailyCurrentStreak"),
            dailyMaxStreak      = obj.optInt("dailyMaxStreak"),
            dailyDistribution   = MutableList(maxGuesses) { i -> if (i < ddist.length()) ddist.getInt(i) else 0 },
        )
    } catch (_: Exception) {
        makeEmptyWwStats(maxGuesses)
    }
}

private fun saveWwStats(context: Context, difficulty: String, stats: WwStats) {
    val prefs = context.getSharedPreferences(WW_STATS_PREFS, Context.MODE_PRIVATE)
    val obj = JSONObject().apply {
        put("played", stats.played)
        put("won", stats.won)
        put("currentStreak", stats.currentStreak)
        put("maxStreak", stats.maxStreak)
        put("distribution", JSONArray(stats.distribution))
        put("lastPlayedDate", stats.lastPlayedDate)
        put("lastDailyDate", stats.lastDailyDate)
        put("dailyPlayed", stats.dailyPlayed)
        put("dailyWon", stats.dailyWon)
        put("dailyCurrentStreak", stats.dailyCurrentStreak)
        put("dailyMaxStreak", stats.dailyMaxStreak)
        put("dailyDistribution", JSONArray(stats.dailyDistribution))
    }
    prefs.edit().putString(difficulty, obj.toString()).apply()
}

fun recordWwResult(context: Context, difficulty: String, won: Boolean, guessCount: Int, isDaily: Boolean, dateStr: String?) {
    var s = getWwStats(context, difficulty)
    val today = dateStr ?: LocalDate.now().toString()
    val yesterday = LocalDate.now().minusDays(1).toString()

    var newStreak = s.currentStreak
    var newMax = s.maxStreak
    val newDist = s.distribution.toMutableList()
    if (won) {
        val idx = min(guessCount - 1, newDist.size - 1)
        if (idx >= 0) newDist[idx]++
        if (s.lastPlayedDate != today) {
            newStreak = if (s.lastPlayedDate == yesterday) s.currentStreak + 1 else 1
            newMax = maxOf(newMax, newStreak)
        }
    } else {
        newStreak = 0
    }
    s = s.copy(
        played = s.played + 1, won = if (won) s.won + 1 else s.won,
        currentStreak = newStreak, maxStreak = newMax,
        distribution = newDist, lastPlayedDate = today,
    )

    if (isDaily && dateStr != null) {
        var dStreak = s.dailyCurrentStreak
        var dMax = s.dailyMaxStreak
        val dDist = s.dailyDistribution.toMutableList()
        if (won) {
            val idx = min(guessCount - 1, dDist.size - 1)
            if (idx >= 0) dDist[idx]++
            if (s.lastDailyDate != dateStr) {
                dStreak = if (s.lastDailyDate == yesterday) s.dailyCurrentStreak + 1 else 1
                dMax = maxOf(dMax, dStreak)
            }
        } else {
            dStreak = 0
        }
        s = s.copy(
            dailyPlayed = s.dailyPlayed + 1, dailyWon = if (won) s.dailyWon + 1 else s.dailyWon,
            dailyCurrentStreak = dStreak, dailyMaxStreak = dMax,
            dailyDistribution = dDist, lastDailyDate = dateStr,
        )
    }
    saveWwStats(context, difficulty, s)
}

fun hasDailyWwBeenPlayed(context: Context, difficulty: String, dateStr: String): Boolean =
    getWwStats(context, difficulty).lastDailyDate == dateStr
