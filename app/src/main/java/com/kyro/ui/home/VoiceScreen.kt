package com.kyro.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyro.AppModule
import com.kyro.ui.theme.Amber
import com.kyro.ui.theme.DarkGraphite
import com.kyro.ui.theme.DeepVoid
import com.kyro.ui.theme.WarmIvory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class VoiceState { LISTENING, THINKING, SPEAKING, IDLE }

@Composable
fun VoiceScreen(appModule: AppModule?, onClose: () -> Unit) {
    var voiceState by remember { mutableStateOf(VoiceState.IDLE) }
    var transcription by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val spokenTextFlow = appModule?.speechInput?.spokenText?.collectAsState()
    val isSpeakingFlow = appModule?.ttsEngine?.isSpeaking?.collectAsState()

    LaunchedEffect(Unit) {
        voiceState = VoiceState.LISTENING
        appModule?.speechInput?.startListening()
    }

    LaunchedEffect(spokenTextFlow?.value) {
        val text = spokenTextFlow?.value
        if (!text.isNullOrBlank() && voiceState == VoiceState.LISTENING) {
            transcription = text
            voiceState = VoiceState.THINKING

            scope.launch {
                val responses = appModule?.commandParser?.processCommand(text)
                val reply = responses?.joinToString(" ") { it.responseText } ?: "Data nahi hai"
                transcription = reply
                voiceState = VoiceState.SPEAKING
                appModule?.ttsEngine?.speak(reply, appModule.speechInput.rmsLevel.value)
            }
        }
    }

    LaunchedEffect(isSpeakingFlow?.value) {
        if (isSpeakingFlow?.value == false && voiceState == VoiceState.SPEAKING) {
            delay(500)
            transcription = ""
            voiceState = VoiceState.LISTENING
            appModule?.speechInput?.startListening()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            appModule?.speechInput?.stopListening()
            appModule?.ttsEngine?.stop()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepVoid)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            KyroOrb(
                state = when (voiceState) {
                    VoiceState.LISTENING -> OrbState.LISTENING
                    VoiceState.THINKING -> OrbState.THINKING
                    VoiceState.SPEAKING -> OrbState.SPEAKING
                    else -> OrbState.IDLE
                }
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = when(voiceState) {
                    VoiceState.LISTENING -> "LISTENING..."
                    VoiceState.THINKING -> "PROCESSING..."
                    VoiceState.SPEAKING -> "SPEAKING..."
                    VoiceState.IDLE -> ""
                },
                color = Amber,
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = transcription,
                color = WarmIvory,
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp
                ),
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Box(
            modifier = Modifier
                .padding(bottom = 32.dp)
                .size(64.dp)
                .clip(CircleShape)
                .background(DarkGraphite)
                .clickable {
                    appModule?.speechInput?.stopListening()
                    onClose()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "X",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
            )
        }
    }
}
