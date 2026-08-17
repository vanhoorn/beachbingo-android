package com.bestfriends.beachbingo.feature.perlentaucher

enum class PieceType {
    PERLE, SEEGLAS, MUSCHEL, SEESTERN, KORALLE, SEETANG;

    companion object {
        fun fromOrdinal(i: Int): PieceType = entries[i]
    }
}

enum class SpecialType {
    NONE,
    GESTREIFT_H,  // 4er H-Reihe → räumt ganze Zeile
    GESTREIFT_V,  // 4er V-Reihe → räumt ganze Spalte
    EINGEPACKT,   // L/T-Form    → räumt 3×3 Bereich
    PERLENKETTE;  // 5er gerade  → Farbbombe (räumt alle gleiche Farbe)

    companion object {
        fun fromOrdinal(i: Int): SpecialType = entries[i]
    }
}

data class PerlentaucherPiece(
    val type: PieceType,
    val special: SpecialType = SpecialType.NONE,
) {
    // Compact int encoding for board serialization: type * 10 + special
    fun toInt(): Int = type.ordinal * 10 + special.ordinal

    companion object {
        fun fromInt(v: Int): PerlentaucherPiece =
            PerlentaucherPiece(
                PieceType.fromOrdinal(v / 10),
                SpecialType.fromOrdinal(v % 10),
            )
    }
}
