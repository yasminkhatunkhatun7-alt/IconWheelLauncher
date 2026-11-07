package com.kyro.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
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

enum class NavTab { HOME, CHAT, ACTIVITY, HOURS, SETUP }

@Composable
fun HomeScreen(appModule: AppModule? = null) {
    var selectedTab by remember { mutableStateOf(NavTab.HOME) }
    var isVoiceModeActive by remember { mutableStateOf(false) }

    if (isVoiceModeActive) {
        VoiceScreen(
            appModule = appModule,
            onClose = { isVoiceModeActive = false }
        )
    } else {
        Scaffold(
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    KyroBottomBar(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }
            },
            containerColor = DeepVoid
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(
                    targetState = selectedTab,
                    animationSpec = tween(300),
                    label = "TabTransition"
                ) { tab ->
                    when (tab) {
                        NavTab.HOME -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                KyroOrb(
                                    isListening = false,
                                    modifier = Modifier.clickable {
                                        isVoiceModeActive = true
                                    }
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                Text(
                                    text = "TAP TO WAKE",
                                    color = MutedPewter,
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                        NavTab.CHAT -> Text("CHAT HISTORY", color = WarmIvory)
                        NavTab.ACTIVITY -> Text("ACTIVITY TRACKER", color = WarmIvory)
                        NavTab.HOURS -> Text("HOURS & SCREEN TIME", color = WarmIvory)
                        NavTab.SETUP -> {
                            val currentUrl = appModule?.termuxServerClient?.serverUrl ?: "http://127.0.0.1:8080/completion"
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
    }
}

@Composable
fun KyroBottomBar(selectedTab: NavTab, onTabSelected: (NavTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .clip(RoundedCornerShape(32.dp))
            .background(DarkGraphite.copy(alpha = 0.9f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavTab.values().forEach { tab ->
            val isSelected = selectedTab == tab

            // Animated background color for smoother transition
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) Amber.copy(alpha = 0.2f) else Color.Transparent,
                animationSpec = tween(300),
                label = "TabBgColor"
            )

            val contentColor by animateColorAsState(
                targetValue = if (isSelected) Amber else MutedPewter,
                animationSpec = tween(300),
                label = "TabContentColor"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(bgColor)
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = tab.name.take(3),
                    color = contentColor,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)
                    )
                )
            }
        }
    }
}
