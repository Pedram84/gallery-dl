package com.example.gallerydl.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gallerydl.R
import com.example.gallerydl.data.models.CliOptionToggle
import com.example.gallerydl.data.models.DefaultCliOptions
import com.example.gallerydl.data.models.LogEntry
import com.example.gallerydl.data.repository.JobRepository
import com.example.gallerydl.service.DownloadService
import com.example.gallerydl.ui.components.TerminalView
import com.example.gallerydl.ui.theme.*
import com.example.gallerydl.util.CliParseException
import com.example.gallerydl.util.CliTokenizer
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloaderScreen(
    jobRepository: JobRepository,
    initialUrl: String = "",
    onNavigateToJobs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var urlInput by remember { mutableStateOf(initialUrl) }
    val currentJobId by jobRepository.currentRunningJobId.collectAsState()
    val isRunning = currentJobId != null

    val optionToggles = remember { mutableStateListOf(*DefaultCliOptions.getCommonToggles().toTypedArray()) }
    var showAdvancedOptions by remember { mutableStateOf(false) }
    var customArgsInput by remember { mutableStateOf("") }
    var liveLogs by remember { mutableStateOf<List<LogEntry>>(emptyList()) }

    val defaultDest = remember { jobRepository.getDefaultDownloadDirectory() }

    val sampleUrls = listOf(
        "https://upload.wikimedia.org/wikipedia/commons/4/47/PNG_transparency_demonstration_1.png",
        "https://commons.wikimedia.org/wiki/Category:Featured_pictures_on_Wikimedia_Commons",
        "https://danbooru.donmai.us/posts?tags=scenery",
        "https://reddit.com/r/wallpapers/hot"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner Graphic
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_downloader),
                    contentDescription = "Downloader Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(
                                    DarkBackground.copy(alpha = 0.85f),
                                    DarkBackground.copy(alpha = 0.3f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "gallery-dl Downloader",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Download images, manga, albums & metadata from 500+ sites",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AmberLight
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // URL Input Section
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Target URL",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Paste from clipboard button
                        FilledTonalButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val text = clip.getItemAt(0).text?.toString() ?: ""
                                    if (text.isNotBlank()) {
                                        urlInput = text.trim()
                                        Toast.makeText(context, "Pasted URL", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp).testTag("paste_url_button")
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste", fontSize = 12.sp)
                        }

                        if (urlInput.isNotBlank()) {
                            IconButton(
                                onClick = { urlInput = "" },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = {
                        Text(
                            text = "https://example.com/gallery/123",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("target_url_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedBorderColor = AmberPrimary,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = false,
                    maxLines = 3
                )

                // Sample URLs Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Quick Sample URLs:",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sampleUrls) { sample ->
                            val label = sample.substringAfter("://").substringBefore("/").take(18)
                            SuggestionChip(
                                onClick = { urlInput = sample },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = CyanAccent
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = BorderSubtle
                                )
                            )
                        }
                    }
                }
            }
        }

        // Quick Options Toggle Chips
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Extraction Options",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(optionToggles) { opt ->
                        FilterChip(
                            selected = opt.isEnabled,
                            onClick = { opt.isEnabled = !opt.isEnabled },
                            label = { Text(opt.label, fontSize = 12.sp) },
                            leadingIcon = if (opt.isEnabled) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary,
                                selectedContainerColor = AmberPrimary,
                                selectedLabelColor = DarkBackground,
                                selectedLeadingIconColor = DarkBackground
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = opt.isEnabled,
                                borderColor = BorderSubtle,
                                selectedBorderColor = AmberPrimary
                            ),
                            modifier = Modifier.testTag("option_toggle_${opt.id}")
                        )
                    }
                }

                // Advanced Options Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvancedOptions = !showAdvancedOptions }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Advanced CLI Flags",
                        style = MaterialTheme.typography.bodyMedium.copy(color = CyanAccent)
                    )
                    Icon(
                        imageVector = if (showAdvancedOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Advanced Options",
                        tint = CyanAccent
                    )
                }

                AnimatedVisibility(visible = showAdvancedOptions) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customArgsInput,
                            onValueChange = { customArgsInput = it },
                            placeholder = {
                                Text(
                                    text = "e.g. --chapter-filter 1-10 --write-pages",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("custom_cli_args_input"),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant,
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                        )
                        Text(
                            text = "Save directory: ${defaultDest.path}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val urls = urlInput.lines()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    if (urls.isEmpty()) {
                        Toast.makeText(context, "Please enter at least one valid URL", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val argsList = mutableListOf<String>()
                    optionToggles.filter { it.isEnabled }.forEach { toggle ->
                        val parts = CliTokenizer.tokenize(toggle.flag, stripExecutablePrefix = false)
                        argsList.addAll(parts)
                    }

                    if (customArgsInput.isNotBlank()) {
                        try {
                            argsList.addAll(CliTokenizer.tokenize(customArgsInput, stripExecutablePrefix = true))
                        } catch (e: CliParseException) {
                            Toast.makeText(context, "Argument error: ${e.message}", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                    }

                    liveLogs = emptyList()
                    DownloadService.start(context, urls.first())
                    jobRepository.startJob(
                        urls = urls,
                        extraArgs = argsList,
                        destination = defaultDest,
                        onLogUpdate = { newLog ->
                            liveLogs = liveLogs + newLog
                        }
                    )
                },
                enabled = !isRunning && urlInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberPrimary,
                    contentColor = DarkBackground,
                    disabledContainerColor = DarkSurfaceHigh,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("download_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRunning) "Downloading..." else "Download",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            OutlinedButton(
                onClick = {
                    val urls = urlInput.lines()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    if (urls.isEmpty()) {
                        Toast.makeText(context, "Please enter at least one valid URL", Toast.LENGTH_SHORT).show()
                        return@OutlinedButton
                    }

                    val argsList = mutableListOf<String>("-g")
                    if (customArgsInput.isNotBlank()) {
                        try {
                            argsList.addAll(CliTokenizer.tokenize(customArgsInput, stripExecutablePrefix = true))
                        } catch (e: CliParseException) {
                            Toast.makeText(context, "Argument error: ${e.message}", Toast.LENGTH_LONG).show()
                            return@OutlinedButton
                        }
                    }

                    liveLogs = emptyList()
                    jobRepository.startJob(
                        urls = urls,
                        extraArgs = argsList,
                        destination = defaultDest,
                        onLogUpdate = { newLog ->
                            liveLogs = liveLogs + newLog
                        }
                    )
                },
                enabled = !isRunning && urlInput.isNotBlank(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = CyanAccent
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (!isRunning && urlInput.isNotBlank()) CyanAccent else BorderSubtle),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("extract_urls_button")
            ) {
                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Get URLs",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        // Live Execution Console
        TerminalView(
            logs = liveLogs,
            onClear = { liveLogs = emptyList() },
            title = "Live Engine Console",
            modifier = Modifier.testTag("live_terminal_console")
        )
    }
}
