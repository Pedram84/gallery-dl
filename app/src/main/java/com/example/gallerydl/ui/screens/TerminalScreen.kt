package com.example.gallerydl.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gallerydl.data.models.LogEntry
import com.example.gallerydl.engine.GalleryDlEngine
import com.example.gallerydl.engine.ProcessOutputCallback
import com.example.gallerydl.ui.components.TerminalView
import com.example.gallerydl.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TerminalScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var commandInput by remember { mutableStateOf("gallery-dl --version") }
    var logs by remember { mutableStateOf<List<LogEntry>>(emptyList()) }
    var isExecuting by remember { mutableStateOf(false) }

    val presetCommands = listOf(
        "gallery-dl --version",
        "gallery-dl --help",
        "gallery-dl --list-modules",
        "gallery-dl -g https://upload.wikimedia.org/wikipedia/commons/4/47/PNG_transparency_demonstration_1.png",
        "gallery-dl -j https://upload.wikimedia.org/wikipedia/commons/4/47/PNG_transparency_demonstration_1.png"
    )

    fun runCommand(cmd: String) {
        if (cmd.isBlank()) return
        val rawParts = cmd.trim().split("\\s+".toRegex())
        val args = if (rawParts.firstOrNull() == "gallery-dl") rawParts.drop(1) else rawParts

        isExecuting = true
        val startLog = LogEntry(text = "$ $cmd", isError = false)
        logs = logs + startLog

        coroutineScope.launch {
            val callback = object : ProcessOutputCallback {
                override fun onStdout(line: String) {
                    logs = logs + LogEntry(text = line, isError = false)
                }

                override fun onStderr(line: String) {
                    logs = logs + LogEntry(text = line, isError = true)
                }
            }

            val exitCode = withContext(Dispatchers.IO) {
                GalleryDlEngine.execute(args, callback)
            }

            logs = logs + LogEntry(
                text = "Command finished with exit code: $exitCode",
                isError = exitCode != 0
            )
            isExecuting = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Interactive CLI Terminal",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        Text(
            text = "Execute any gallery-dl command directly via the embedded Python engine.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
        )

        // Preset command chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presetCommands) { preset ->
                val label = preset.removePrefix("gallery-dl ").take(22)
                SuggestionChip(
                    onClick = {
                        commandInput = preset
                        runCommand(preset)
                    },
                    label = { Text(label, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = DarkSurfaceVariant,
                        labelColor = AmberPrimary
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = BorderSubtle
                    )
                )
            }
        }

        // Command Input Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                placeholder = { Text("gallery-dl --version", color = TextMuted, fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("terminal_command_input"),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CodeBackground,
                    unfocusedContainerColor = CodeBackground,
                    focusedBorderColor = AmberPrimary,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                singleLine = true
            )

            Button(
                onClick = { runCommand(commandInput) },
                enabled = !isExecuting && commandInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberPrimary,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(52.dp)
                    .testTag("run_command_button")
            ) {
                if (isExecuting) {
                    CircularProgressIndicator(
                        color = DarkBackground,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run")
                }
            }
        }

        // Terminal Output Screen
        TerminalView(
            logs = logs,
            onClear = { logs = emptyList() },
            title = "gallery-dl shell",
            modifier = Modifier
                .weight(1f)
                .testTag("cli_output_view")
        )
    }
}
