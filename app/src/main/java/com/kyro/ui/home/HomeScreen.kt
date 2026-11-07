package com.kyro.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyro.AppModule
import com.kyro.ui.theme.Amber
import com.kyro.ui.theme.DarkGraphite
import com.kyro.ui.theme.DeepVoid
import com.kyro.ui.theme.MutedPewter
import com.kyro.ui.theme.WarmIvory
import kotlinx.coroutines.launch

enum class NavTab { HOME, CHAT, ACTIVITY, HOURS, SETUP }

@Composable
fun HomeScreen(appModule: AppModule? = null) {
    var selectedTab by remember { mutableStateOf(NavTab.HOME) }
    var isListening by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // For setup screen
    val currentUrl = appModule?.termuxServerClient?.serverUrl ?: "http://127.0.0.1:8080/completion"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepVoid)
    ) {
        // Top Navigation Bar
        KyroNavBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        // Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when (selectedTab) {
                NavTab.HOME -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        KyroOrb(
                            isListening = isListening,
                            modifier = Modifier.clickable {
                                isListening = !isListening
                                if (isListening) {
                                    appModule?.speechInput?.startListening()
                                } else {
                                    appModule?.speechInput?.stopListening()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = if (isListening) "LISTENING..." else "TAP TO WAKE",
                            color = MutedPewter,
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                NavTab.CHAT -> Text("CHAT", color = WarmIvory)
                NavTab.ACTIVITY -> Text("ACTIVITY", color = WarmIvory)
                NavTab.HOURS -> Text("HOURS", color = WarmIvory)
                NavTab.SETUP -> {
                    SetupScreen(
                        currentUrl = currentUrl,
                        onUrlChange = { appModule?.updateServerUrl(it) },
                        onSave = {
                            appModule?.ttsEngine?.speak("Configuration saved.")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KyroNavBar(selectedTab: NavTab, onTabSelected: (NavTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(DarkGraphite.copy(alpha = 0.8f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        NavTab.values().forEach { tab ->
            val isSelected = selectedTab == tab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) Amber.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = tab.name,
                    color = if (isSelected) Amber else MutedPewter,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }
        }
    }
}
