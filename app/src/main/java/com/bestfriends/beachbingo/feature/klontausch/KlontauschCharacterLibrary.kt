package com.bestfriends.beachbingo.feature.klontausch

data class KlonCharacter(
    val id: String,
    val name: String,
    val category: String,
)

val ALL_KLON_CHARACTERS: List<KlonCharacter> = listOf(
    // Berufe
    KlonCharacter("astronaut",           "Astronaut",            "Beruf"),
    KlonCharacter("bauarbeiter",         "Bauarbeiter",          "Beruf"),
    KlonCharacter("feuerwehr_frau",      "Feuerwehr-Frau",       "Beruf"),
    KlonCharacter("fischer",             "Fischer",              "Beruf"),
    KlonCharacter("koch",                "Koch",                 "Beruf"),
    KlonCharacter("modedesignerin",      "Modedesignerin",       "Beruf"),
    KlonCharacter("nachrichtensprecher", "Nachrichtensprecher",  "Beruf"),
    KlonCharacter("pilotin",             "Pilotin",              "Beruf"),
    KlonCharacter("polizist",            "Polizist",             "Beruf"),
    // Tiere
    KlonCharacter("eule",                "Eule",                 "Tier"),
    KlonCharacter("faultier",            "Faultier",             "Tier"),
    KlonCharacter("flamingo",            "Flamingo",             "Tier"),
    KlonCharacter("frosch",              "Frosch",               "Tier"),
    // Unterhaltung
    KlonCharacter("comedian",            "Comedian",             "Show"),
    KlonCharacter("fussballstar",        "Fußballstar",          "Show"),
    KlonCharacter("influencerin",        "Influencerin",         "Show"),
    KlonCharacter("muskelheld",          "Muskelheld",           "Show"),
    KlonCharacter("pop_diva",            "Pop-Diva",             "Show"),
    KlonCharacter("rockstar",            "Rockstar",             "Show"),
    // Alien & Fantasy
    KlonCharacter("green_eye",           "Green Eye",            "Alien"),
    KlonCharacter("ice_baby",            "Ice Baby",             "Alien"),
    KlonCharacter("one_eye",             "One Eye",              "Alien"),
    KlonCharacter("splash",              "Splash",               "Alien"),
    KlonCharacter("sunny_bear",          "Sunny Bear",           "Alien"),
    KlonCharacter("tech_visionaer",      "Tech-Visionaer",       "Alien"),
    KlonCharacter("worms",               "Worms",                "Alien"),
    // Meerestiere
    KlonCharacter("clownfisch",          "Clownfisch",           "Meer"),
    KlonCharacter("hummer",              "Hummer",               "Meer"),
    KlonCharacter("seepferdchen",        "Seepferdchen",         "Meer"),
    KlonCharacter("tintenfisch",         "Tintenfisch",          "Meer"),
    // Pflanzen
    KlonCharacter("kaktuspflanze",       "Kaktuspflanze",        "Pflanze"),
    KlonCharacter("palme",               "Palme",                "Pflanze"),
    KlonCharacter("sonnenblume",         "Sonnenblume",          "Pflanze"),
    KlonCharacter("venusfliegenfalle",   "Venusfliegenfalle",    "Pflanze"),
    // Comic
    KlonCharacter("comic_pirat",         "Comic-Pirat",          "Comic"),
    KlonCharacter("comic_roboter",       "Comic-Roboter",        "Comic"),
    KlonCharacter("comic_superheld",     "Comic-Superheld",      "Comic"),
    KlonCharacter("comic_zauberer",      "Comic-Zauberer",       "Comic"),
)

fun klonCharacterById(id: String): KlonCharacter =
    ALL_KLON_CHARACTERS.find { it.id == id } ?: ALL_KLON_CHARACTERS.first()
