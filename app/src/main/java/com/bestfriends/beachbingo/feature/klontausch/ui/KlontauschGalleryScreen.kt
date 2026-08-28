package com.bestfriends.beachbingo.feature.klontausch.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bestfriends.beachbingo.feature.klontausch.ALL_KLON_CHARACTERS
import com.bestfriends.beachbingo.feature.klontausch.KlonPart
import com.bestfriends.beachbingo.ui.theme.*

private val KlontauschAccent = Color(0xFF8B5CF6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KlontauschGalleryScreen(
    onNavigateBack: () -> Unit,
) {
    var fullscreenIndex by remember { mutableStateOf<Int?>(null) }

    BackHandler(enabled = fullscreenIndex != null) {
        fullscreenIndex = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Figurengalerie", color = TextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = TextSub)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
                )
            },
            containerColor = BgDark,
        ) { padding ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(ALL_KLON_CHARACTERS) { index, char ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { fullscreenIndex = index },
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            KlontauschCharacterView(
                                characterId = char.id,
                                part = KlonPart.KOPF,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1024f / 358f),
                            )
                            KlontauschCharacterView(
                                characterId = char.id,
                                part = KlonPart.KOERPER,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1024f / 358f),
                            )
                            KlontauschCharacterView(
                                characterId = char.id,
                                part = KlonPart.BEINE,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1024f / 308f),
                            )
                        }
                        Text(
                            text = char.name,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = char.category,
                            color = KlontauschAccent.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }

        // ── Vollbild-Pager ────────────────────────────────────────────────────
        val startIndex = fullscreenIndex
        if (startIndex != null) {
            val pagerState = rememberPagerState(initialPage = startIndex) { ALL_KLON_CHARACTERS.size }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgDark.copy(alpha = 0.97f)),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val char = ALL_KLON_CHARACTERS[page]
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.72f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceDark),
                        ) {
                            KlontauschCharacterView(
                                characterId = char.id,
                                part = KlonPart.KOPF,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1024f / 358f),
                            )
                            KlontauschCharacterView(
                                characterId = char.id,
                                part = KlonPart.KOERPER,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1024f / 358f),
                            )
                            KlontauschCharacterView(
                                characterId = char.id,
                                part = KlonPart.BEINE,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1024f / 308f),
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = char.name,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = char.category,
                            color = KlontauschAccent,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "${pagerState.currentPage + 1} / ${ALL_KLON_CHARACTERS.size}",
                            color = TextMuted,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }

                // Close button
                IconButton(
                    onClick = { fullscreenIndex = null },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(8.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Schließen", tint = TextPrimary)
                }
            }
        }
    }
}
