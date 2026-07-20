package com.bestfriends.beachbingo.feature.bingo.ui

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bestfriends.beachbingo.feature.join.viewmodel.JoinDestination
import com.bestfriends.beachbingo.feature.join.viewmodel.JoinViewModel
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGameScreen(
    onNavigateToBingo: (String) -> Unit,
    onNavigateToPong: (String, Int, Int, String, Int, Boolean, String) -> Unit,
    onNavigateToVier: (String, String) -> Unit,
    onNavigateToBrandung: (String) -> Unit,
    onNavigateToMeermau: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: JoinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var gameCode by remember { mutableStateOf("") }
    val context = LocalContext.current

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intentResult: IntentResult = IntentIntegrator.parseActivityResult(
                result.resultCode, result.data
            )
            intentResult.contents?.let { scanned ->
                // QR may encode a URL or bare gameId
                val match = scanned.trim().let { raw ->
                    Regex("[A-Za-z0-9]{6,}").findAll(raw).lastOrNull()?.value ?: raw
                }
                gameCode = match
                viewModel.joinGame(match)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val integrator = IntentIntegrator(context as Activity).apply {
                setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                setPrompt("QR-Code des Spiels scannen")
                setBeepEnabled(false)
                setOrientationLocked(false)
            }
            scannerLauncher.launch(integrator.createScanIntent())
        }
    }

    LaunchedEffect(uiState.destination) {
        uiState.destination?.let { dest ->
            viewModel.clearNavigate()
            when (dest) {
                is JoinDestination.Bingo    -> onNavigateToBingo(dest.gameId)
                is JoinDestination.Pong     -> onNavigateToPong(
                    dest.gameId, dest.totalPaddles, dest.humanCount,
                    dest.difficulty, dest.scoreLimit, dest.isHost, dest.mySide
                )
                is JoinDestination.Vier     -> onNavigateToVier(dest.gameId, dest.myDrinkId)
                is JoinDestination.Brandung -> onNavigateToBrandung(dest.gameId)
                is JoinDestination.MeerMau  -> onNavigateToMeermau(dest.gameId)
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearNavigate()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spiel beitreten") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .imePadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎯", style = MaterialTheme.typography.displayMedium)

            Spacer(Modifier.height(8.dp))
            Text(
                "BeachBingo · BeachPong · Vier4Bier · Brandung · MeerMau",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.size(10.dp))
                Text("QR-Code scannen", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(Modifier.height(24.dp))

            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text(
                "oder Code manuell eingeben",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = gameCode,
                onValueChange = { gameCode = it },
                label = { Text("Spiel-Code") },
                placeholder = { Text("z.B. abc123de") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { viewModel.joinGame(gameCode) },
                enabled = !uiState.isLoading && gameCode.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text("Beitreten", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
