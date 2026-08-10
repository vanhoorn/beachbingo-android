package com.bestfriends.beachbingo.ui.theme

import androidx.compose.ui.unit.sp

// Intentional display/emoji sizes that fall outside the standard typography scale.
// These live in ui/theme/ and are exempt from the checkHardcodedTheme lint task.

val StatusTiny       = 9.sp   // tiny subtitle text (e.g. "BeachBingo" in HUD)
val ChipLabelTiny    = 10.sp  // extra-small number in mini circle (tablet)
val ChipLabel        = 12.sp  // small number circle / compact chip label
val CellNumber       = 14.sp  // Bingo card grid cell number
val BingoCallSize    = 20.sp  // BINGO! button text and FREE-cell star
val EmojiMedium      = 30.sp  // avatar emoji in player rows
val EmojiLarge       = 40.sp  // pause/game-over overlay emoji
val EmojiXLarge      = 56.sp  // large hero/branding emoji
val DrawNumberTablet = 48.sp  // current draw number on tablet layout
val DrawNumberPhone  = 64.sp  // current draw number on phone layout
val ScoreLarge       = 32.sp  // large score / highscore number display
val EmojiCelebrate   = 72.sp  // celebration overlay emoji
val BadgeTiny        = 7.sp   // "NEU" badge text (card game)
val LabelMicro       = 8.sp   // micro label (HUD tiny status text)
val TitleHero        = 26.sp  // hero brand title in lobby header
val EmojiHuge        = 52.sp  // oversized game emoji (tie/win in Vier)
val ScoreHighlight   = 38.sp  // highlighted score in online lobbies
