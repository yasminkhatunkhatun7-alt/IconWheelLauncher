package com.kyro.ui

import android.media.RingtoneManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyro.KyroApplication
import com.kyro.ui.theme.DarkGraphite
import com.kyro.ui.theme.KyroTheme

class FakeCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appModule = (application as KyroApplication).appModule

        // Feature 19: Fake Call Ringing & TTS
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val ringtone = RingtoneManager.getRingtone(applicationContext, ringtoneUri)

        setContent {
            KyroTheme {
                DisposableEffect(Unit) {
                    ringtone.play()
                    appModule.ttsEngine.speak("Call from Papa.")
                    onDispose {
                        ringtone.stop()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkGraphite),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Incoming Call", color = Color.Gray, fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Papa", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(120.dp))

                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                                .clickable { finish() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("End", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
