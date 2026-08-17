package com.bestfriends.beachbingo.ui.theme

import androidx.compose.ui.graphics.Color

// Beach palette – matching web app (always dark)
val BgDark            = Color(0xFF0A1628)   // --bg
val SurfaceDark       = Color(0xFF132035)   // --surface
val Surface2Dark      = Color(0xFF1E3050)   // --surface2
val Surface3Dark      = Color(0xFF243760)   // --surface3

val OceanBlue         = Color(0xFF0EA5E9)   // --primary
val OceanBlueDark     = Color(0xFF0284C7)   // --primary-dark

val SandGold          = Color(0xFFF59E0B)   // --accent
val SandGoldDark      = Color(0xFFD97706)   // --accent-dark
val SandGoldLight     = Color(0xFFFBBF24)   // lighter amber for highlights

val Coral             = Color(0xFFF97316)   // --coral

val TextPrimary       = Color(0xFFE2E8F0)   // --text
val TextMuted         = Color(0xFF64748B)   // --text-muted
val TextSub           = Color(0xFF94A3B8)   // --text-sub
val BorderColor       = Color(0xFF1E3050)   // --border

val Success           = Color(0xFF22C55E)
val SuccessDark       = Color(0xFF15803D)   // darker green for head/accent
val Danger            = Color(0xFFEF4444)
val DangerBright      = Color(0xFFFF4444)   // brighter red for overlay text

// Overlay / special backgrounds
val OverlayDark       = Color(0xCC1A0A0A)   // semi-transparent dark overlay

// Fireworks / confetti palette (Bingo celebration)
val ConfettiRed       = Color(0xFFFF6B6B)
val ConfettiTeal      = Color(0xFF4ECDC4)
val ConfettiYellow    = Color(0xFFFFE66D)
val ConfettiGreen     = Color(0xFF96CEB4)
val ConfettiPink      = Color(0xFFFF9FF3)
val ConfettiBlue      = Color(0xFF54A0FF)
val ConfettiOrange    = Color(0xFFFF9F43)
val ConfettiDarkOrange = Color(0xFFEE5A24)

// Worm game canvas
val WormDeathFlash    = Color(0xEFEF4444.toInt()) // deadly-wall border (A=EF R=EF G=44 B=44)

// Bingo card cell colors
val FreeCellYellow    = Color(0xFFFFD600)   // FREE center cell background
val FreeCellBrown     = Color(0xFF5D4037)   // FREE center cell text

// Perlentaucher piece colors
val PearlWhite        = Color(0xFFF5EFE0)   // Perle — Kreis + Glanzring

// Mahjong / GezeitenSteine tile colors
val MahjongGold      = Color(0xFFD4A820)   // Mahjong accent / active dot
val TileRemoveFlash  = Color(0xFFFFF176)   // gold flash on tile removal
val TileSelected     = Color(0xFFBEE3F8)   // selected tile highlight (light blue)
val TileHinted       = Color(0xFFFFD6B0)   // hint tile highlight (peach)
val TileFreeHint     = Color(0xFFDCF5E5)   // free-move hint (light green)
val TileFree         = Color(0xFFFAF0DC)   // freely playable tile (warm cream)
val TileBlocked      = Color(0xFFEDD9B8)   // blocked tile (dimmed sand)
val TileSandLight    = Color(0xFFBB9C78)   // tile 3D side – lighter sand shadow
val TileSandDark     = Color(0xFFA88B65)   // tile 3D side – deeper sand shadow

// Home screen category accents
val SkyBlue          = Color(0xFF38BDF8)   // Rätsel category highlight

// Pirates game palette
val PiratesPurple     = Color(0xFFA855F7)   // pirates main theme accent
val PurpleDeep        = Color(0xFF7C3AED)   // deep purple, bullet gradient end
val PurpleLight       = Color(0xFFC084FC)   // light purple, player bullet highlight
val PiratesOrange     = Color(0xFFFB923C)   // enemy bullet color
val OrangeDark        = Color(0xFFEA580C)   // enemy bullet gradient end
val BgPirateDark      = Color(0xFF1A0A2E)   // deep purple hero gradient start
val BgPirateDeepest   = Color(0xFF07072A)   // near-black deep bg for canvas gradient

// Game accent colors
val Crimson          = Color(0xFFE11D48)   // Strandraeuber danger/target
val Teal             = Color(0xFF0D9488)   // Brandung player/score accent
val BeerOrange       = Color(0xFFC2410C)   // Vier / Aperol accent
val RoseRed          = Color(0xFFFB7185)   // Kuestenkrieg battle accent
val LimeGreen        = Color(0xFF4ADE80)   // Inselbruecke island/bridge accent
val CyanBright       = Color(0xFF06B6D4)   // WortWelle letter accent
val OrangeVivid      = Color(0xFFE67E22)   // MeerMau card highlight
val GreenVivid       = Color(0xFF27AE60)   // MeerMau valid play highlight
val StrandturmRed    = Color(0xFFDC2626)   // Strandturm lives/danger
val DangerVivid      = Color(0xFFCC0000)   // card suit vivid red

// WortWelle letter status
val WwPresent        = Color(0xFFEAB308)   // letter present, wrong position
val WwAbsent         = Color(0xFF374151)   // letter absent key
val WwAbsentBg       = Color(0xFF1F2937)   // absent letter background

// Puzzle cell backgrounds
val BgNavyCell       = Color(0xFF0A1929)   // very dark navy puzzle cell
val BgNightBlue      = Color(0xFF111827)   // near-black board background
val BgDeepNavy       = Color(0xFF0D1B2E)   // deep navy canvas background
val NearBlack        = Color(0xFF0D0D0D)   // Pong table surface
val DarkGray         = Color(0xFF333333)   // dark border / divider
val MidGray          = Color(0xFF444444)   // mid-gray hint / label

// Card game colors (Brandung + MeerMau)
val CardFace         = Color(0xFFFFFBF0)   // card face warm white
val CardBack         = Color(0xFF0D1F3C)   // card back navy
val CardColorDark    = Color(0xFF1A1A2E)   // card color for black suits
val CardBorderLight  = Color(0xFFDDE0E4)   // default card border
val CardTable        = Color(0xFF1A5C2E)   // green card table surface
val CardFrame        = Color(0xFF8B7355)   // wooden card frame border
val CardOptionBg     = Color(0xFFF5F5F5)   // suit choice sheet background
val InkBlack         = Color(0xFF111111)   // card suit dark button text

// Kuestenkrieg battle cell states
val HitCell          = Color(0x88EF4444)   // hit ship cell (53% red)
val SunkCell         = Color(0xCCEF4444)   // sunk ship cell (80% red)
val MyShipCell       = Color(0x88FB7185)   // own ship cell (53% rose)
val DangerGlow       = Color(0x22EF4444)   // danger highlight glow (13%)
val DangerRing       = Color(0x55EF4444)   // danger ring border (33%)

// Strandturm wood & canvas colors
val WoodBrown        = Color(0xFF7C3F1A)   // block face
val WoodHighlight    = Color(0xFFA05A2C)   // block top highlight
val WoodShadow       = Color(0xFF4A2409)   // block bottom shadow
val WoodGrain        = Color(0xFF6B3416)   // block grain accent
val WoodRail         = Color(0xFF8B6534)   // rail guide
val BurntAmber       = Color(0xFF92400E)   // dark wood / drink whisky
val WoodDeep         = Color(0xFF5C2D0A)   // very dark block underside
val WoodVein         = Color(0xFF3D1A06)   // darkest wood spot
val CharacterSkin    = Color(0xFFFDE68A)   // character skin / hair
val OverlayWhite35   = Color(0x59FFFFFF)   // 35% white canvas overlay
val GlowAmber25      = Color(0x40FBB124)   // 25% amber glow
val RippleBlue15     = Color(0x260EA5E9)   // 15% ocean blue ripple
val ShimmerWhite13   = Color(0x21FFFFFF)   // 13% white shimmer

// Strandturm belt / elevator / crane / niete canvas colors
val StBeltSurface    = Color(0xE0334155)   // conveyor belt surface (88% slate)
val BeltEdgeAmber    = Color(0xD9FBBF24)   // belt direction edge – moving right (85% amber)
val BeltEdgeBlue     = Color(0xD960A5FA)   // belt direction edge – moving left (85% blue)
val ElevTrackBlue    = Color(0x333B82F6)   // elevator track rail (20% blue)
val CanvasShadow     = Color(0x4D000000)   // generic canvas drop shadow (30% black)
val ElevBodyBlue     = Color(0xFF1D4ED8)   // elevator main body
val ElevHighlight    = Color(0xFF3B82F6)   // elevator top stripe
val ElevShadowBlue   = Color(0xFF1E3A8A)   // elevator bottom stripe
val ElevBoltLight    = Color(0xFF93C5FD)   // elevator side bolts
val WeightBody       = Color(0xFF374151)   // weight main body fill
val WeightEdge       = Color(0xFF4B5563)   // weight top/side edge highlight
val WeightShadow     = Color(0xFF1F2937)   // weight bottom shadow
val WanneBody        = Color(0xFF78716C)   // cement trough body
val WanneCement      = Color(0xFFA8A29E)   // cement surface
val WanneHandle      = Color(0xFF57534E)   // crane handle
val WanneSlot        = Color(0xFF44403C)   // crane slot
val NieteGlow        = Color(0x38FBB124)   // bolt glow (22% amber)
val NieteCenter      = Color(0xFF78350F)   // bolt center dark amber

// Brandung wave / beach / palm canvas
val WaveDark         = Color(0xFF1A72C8)
val WaveLight        = Color(0xFF5AB8E8)
val WaveMid          = Color(0xFF1A8AB8)
val WaveDeep         = Color(0xFF0A4A7A)
val SunGlow          = Color(0xFFFFE033)
val SunCore          = Color(0xFFFFD700)
val SunBright        = Color(0xFFFFED4A)
val SandBeach        = Color(0xFFC8942A)   // beach sand base
val SandBeachLight   = Color(0xFFE4B44A)   // beach sand highlight
val TrunkBrown       = Color(0xFF7A5C2E)   // palm trunk
val PalmDark         = Color(0xFF2A7828)   // palm frond dark
val PalmMid          = Color(0xFF36963A)   // palm frond mid
val PalmDeep         = Color(0xFF1A5020)   // palm frond deep shadow

// Inselbruecke hint colors
val AmberBrown       = Color(0xFFB45309)   // warning hint background
val YellowLight      = Color(0xFFFCD34D)   // hint arrow foreground

// Strandraeuber specific
val BgPlayerZone     = Color(0xFF0F3460)   // player word zone background
val SlateBlueDark    = Color(0xFF334155)   // slate blue border

// Vier gewinnt board
val BoardBlueDark    = Color(0xFF0C1F3D)   // board background
val BoardBlueMid     = Color(0xFF1E3A5F)   // board border
val BoardBlueDeep    = Color(0xFF091525)   // empty cell background

// Vier drink colors
val DrinkDunkles     = Color(0xFF6B2600)
val DrinkProsecco    = Color(0xFFA37C00)
val DrinkRotwein     = Color(0xFFB91C1C)
val DrinkWeisswein   = Color(0xFFA16207)
val DrinkRose        = Color(0xFFBE185D)
val DrinkRum         = Color(0xFF7C2D12)
val DrinkTequila     = Color(0xFF3F6212)
val DrinkGin         = Color(0xFF0369A1)
