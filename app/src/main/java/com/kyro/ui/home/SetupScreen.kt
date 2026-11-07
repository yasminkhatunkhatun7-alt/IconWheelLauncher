package com.kyro.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kyro.ui.theme.Amber
import com.kyro.ui.theme.DarkGraphite
import com.kyro.ui.theme.DeepVoid
import com.kyro.ui.theme.WarmIvory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    currentUrl: String,
    currentWakeWord: String,
    onUrlChange: (String) -> Unit,
    onWakeWordChange: (String) -> Unit,
    onSave: () -> Unit
) {
    var urlText by remember { mutableStateOf(currentUrl) }
    var wakeWordText by remember { mutableStateOf(currentWakeWord) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepVoid)
            .padding(24.dp)
    ) {
        Text(
            text = "SYSTEM SETUP",
            color = Amber,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkGraphite.copy(alpha = 0.5f))
                .border(1.dp, Amber.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column {
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("Local AI Server URL", color = WarmIvory) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Amber,
                        unfocusedBorderColor = DarkGraphite,
                        focusedTextColor = WarmIvory,
                        unfocusedTextColor = WarmIvory,
                        cursorColor = Amber
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = wakeWordText,
                    onValueChange = { wakeWordText = it },
                    label = { Text("Custom Wake Word", color = WarmIvory) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Amber,
                        unfocusedBorderColor = DarkGraphite,
                        focusedTextColor = WarmIvory,
                        unfocusedTextColor = WarmIvory,
                        cursorColor = Amber
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        onUrlChange(urlText)
                        onWakeWordChange(wakeWordText)
                        onSave()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = DeepVoid),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("SAVE CONFIGURATION", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
