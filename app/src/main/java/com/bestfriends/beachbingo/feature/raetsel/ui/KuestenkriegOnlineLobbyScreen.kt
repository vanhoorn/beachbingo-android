package com.bestfriends.beachbingo.feature.raetsel.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.feature.bingo.ui.components.QrCodeImage
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val KkLobbyAccent = Color(0xFFFB7185)

@Composable
fun KuestenkriegOnlineLobbyScreen(
    gameCode: String,
    onNavigateBack: () -> Unit,
    onNavigateToPlacement: (code: String) -> Unit,
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var adminId by remember { mutableStateOf("") }
    var playerNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var starting by remember { mutableStateOf(false) }
    var navigated by remember { mutableStateOf(false) }

    val isAdmin = uid.isNotBlank() && uid == adminId

    LaunchedEffect(gameCode) {
        if (gameCode.isBlank()) return@LaunchedEffect
        db.collection("kuestenkriegGames").document(gameCode)
            .addSnapshotListener { snap, _ ->
                if (snap == null || !snap.exists()) return@addSnapshotListener
                val status = snap.getString("status") ?: "LOBBY"
                adminId = snap.getString("adminId") ?: ""
                @Suppress("UNCHECKED_CAST")
                val pIds = (snap.get("playerIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val playersMap = snap.get("players") as? Map<*, *> ?: emptyMap<Any, Any>()
                playerNames = pIds.map { id ->
                    (playersMap[id] as? Map<*, *>)?.get("displayName") as? String ?: id
                }
                if (status == "PLACEMENT" && !navigated) {
                    navigated = true
                    onNavigateToPlacement(gameCode)
                }
            }
    }

    fun startPlacement() {
        if (!isAdmin || starting || playerNames.size < 2) return
        starting = true
        scope.launch {
            try {
                db.collection("kuestenkriegGames").document(gameCode)
                    .update("status", "PLACEMENT").await()
            } catch (_: Exception) {
                starting = false
            }
        }
    }

    fun cancel() {
        if (isAdmin) db.collection("kuestenkriegGames").document(gameCode).delete()
        onNavigateBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(SurfaceDark, Surface2Dark)))
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp), color = Surface2Dark,
                    modifier = Modifier.size(40.dp).border(1.dp, BorderColor, RoundedCornerShape(12.dp)).clickable { cancel() },
                ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = TextSub, modifier = Modifier.size(20.dp)) } }
                Spacer(Modifier.width(14.dp))
                Text("⚓", fontSize = 32.sp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("KÜSTENKRIEG · ONLINE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.5.sp)
                    Text("Warte auf Spieler…", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                }
            }
        }

        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

            // Game code + QR card
            Surface(
                shape = RoundedCornerShape(16.dp), color = SurfaceDark,
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("SPIELCODE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.5.sp)
                    Text(
                        gameCode,
                        fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = KkLobbyAccent,
                        fontFamily = FontFamily.Monospace, letterSpacing = 6.sp,
                    )
                    OutlinedButton(
                        onClick = {
                            val clip = ClipData.newPlainText("Spielcode", gameCode)
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                        },
                        shape = RoundedCornerShape(8.dp),
                    ) { Text("📋 Kopieren", fontSize = 13.sp, color = TextPrimary) }
                    Surface(shape = RoundedCornerShape(12.dp), color = Color.White, modifier = Modifier.padding(4.dp)) {
                        QrCodeImage(content = "https://beachbande.de/join?code=$gameCode", size = 160.dp)
                    }
                    Text(
                        "QR-Code scannen oder Code eingeben",
                        fontSize = 12.sp, color = TextMuted, textAlign = TextAlign.Center,
                    )
                }
            }

            // Players list
            Surface(
                shape = RoundedCornerShape(12.dp), color = SurfaceDark,
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${playerNames.size} / 2 Spieler", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    playerNames.forEach { name ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("👤", fontSize = 16.sp)
                            Text(name, fontSize = 13.sp, color = TextPrimary)
                        }
                    }
                    if (playerNames.size < 2) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = KkLobbyAccent)
                            Text("Warte auf Gegner…", fontSize = 13.sp, color = TextMuted)
                        }
                    }
                }
            }

            // Start button (admin) or waiting indicator (guest)
            if (isAdmin) {
                Button(
                    onClick = ::startPlacement,
                    enabled = playerNames.size >= 2 && !starting,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KkLobbyAccent,
                        disabledContainerColor = KkLobbyAccent.copy(alpha = 0.3f),
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (starting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BgDark, strokeWidth = 2.dp)
                    } else {
                        Text("Schiffe setzen! →", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = BgDark)
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp), color = SurfaceDark,
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = KkLobbyAccent)
                        Text("Admin startet das Spiel…", fontSize = 13.sp, color = TextMuted)
                    }
                }
            }

            OutlinedButton(
                onClick = ::cancel,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSub),
            ) { Text("Abbrechen", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(32.dp))
    }
}
