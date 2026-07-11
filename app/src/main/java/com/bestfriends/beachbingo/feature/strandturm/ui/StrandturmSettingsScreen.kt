package com.bestfriends.beachbingo.feature.strandturm.ui

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val StrandturmRed = Color(0xFFDC2626)

private data class OptionItem(val id: String, val title: String, val desc: String)

private val CONTROL_OPTIONS = listOf(
    OptionItem("BUTTONS", "🔲 Klassisch",    "D-Pad mittig unter dem Spielfeld – ◄ ▲ ► und ▼"),
    OptionItem("SPLIT",   "✌️ Zwei-Händig", "◄ ► links · ▲ ▼ rechts – ideal für zwei Daumen"),
    OptionItem("TOUCH",   "👆 Touch",        "Bildschirm antippen zum Lenken und Springen"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrandturmSettingsScreen(onNavigateBack: () -> Unit) {
    val auth      = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val uid       = auth.currentUser?.uid
    val scope     = rememberCoroutineScope()

    var controlMode by remember { mutableStateOf("BUTTONS") }
    var saving      by remember { mutableStateOf(false) }
    var saved       by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        val snap = firestore.collection("users").document(uid).get().await()
        controlMode = snap.getString("preferredStrandturmControlMode") ?: "BUTTONS"
    }

    fun doSave() {
        if (uid == null) return
        saving = true
        scope.launch {
            try {
                firestore.collection("users").document(uid).update(
                    mapOf("preferredStrandturmControlMode" to controlMode)
                ).await()
            } catch (_: Exception) {}
            saving = false
            saved  = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("STRANDTURM", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("⚙️ Einstellungen", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.ExtraBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = TextPrimary)
                    }
                },
                actions = {
                    TextButton(onClick = { doSave() }, enabled = !saving) {
                        Text(if (saving) "…" else "Speichern", color = StrandturmRed, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
            )
        },
        containerColor = BgDark,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Diese Einstellungen werden beim Start als Standardwerte übernommen.",
                fontSize = 14.sp, color = TextMuted,
            )

            Text("🕹️ Steuerung", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.padding(start = 4.dp))
            CONTROL_OPTIONS.forEach { opt ->
                RadioRow(opt = opt, selected = controlMode == opt.id, accentColor = StrandturmRed) { controlMode = opt.id }
            }

            if (saved) {
                Surface(
                    color = StrandturmRed.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "✓ Einstellungen gespeichert",
                        color = StrandturmRed,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun RadioRow(opt: OptionItem, selected: Boolean, accentColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accentColor.copy(alpha = 0.12f) else SurfaceDark)
            .border(1.5.dp, if (selected) accentColor else BorderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(18.dp).clip(RoundedCornerShape(50))
                .background(if (selected) accentColor else Color.Transparent)
                .border(2.dp, if (selected) accentColor else TextMuted, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(Color.White))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(opt.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(opt.desc, fontSize = 12.sp, color = TextMuted)
        }
    }
}
