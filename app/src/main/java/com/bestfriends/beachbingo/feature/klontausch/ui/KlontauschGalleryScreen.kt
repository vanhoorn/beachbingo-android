package com.bestfriends.beachbingo.feature.klontausch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            items(ALL_KLON_CHARACTERS) { char ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Full character: head + body + legs stacked
                    Column(modifier = Modifier.fillMaxWidth()) {
                        KlontauschCharacterView(
                            characterId = char.id,
                            part = KlonPart.KOPF,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1024f / 358f),
                        )
                        KlontauschCharacterView(
                            characterId = char.id,
                            part = KlonPart.KOERPER,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1024f / 358f),
                        )
                        KlontauschCharacterView(
                            characterId = char.id,
                            part = KlonPart.BEINE,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1024f / 308f),
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
}
