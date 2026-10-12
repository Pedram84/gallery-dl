package com.example.gallerydl.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.gallerydl.data.repository.ConfigRepository
import com.example.gallerydl.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    configRepository: ConfigRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(0) } // 0 = config.json, 1 = cookies.txt
    var configText by remember { mutableStateOf("") }
    var cookiesText by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        configText = configRepository.loadConfig()
        cookiesText = configRepository.loadCookies()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Configuration & Authentication",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        // Tab Row: config.json vs cookies.txt
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = DarkSurface,
            contentColor = AmberPrimary
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("config.json", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("cookies.txt", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            )
        }

        if (activeTab == 0) {
            // config.json Section
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Config Templates:",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = { configText = configRepository.getDefaultTemplate() },
                            label = { Text("Default", fontSize = 11.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = DarkSurfaceVariant,
                                labelColor = AmberPrimary
                            )
                        )
                        SuggestionChip(
                            onClick = { configText = configRepository.getMangaTemplate() },
                            label = { Text("Manga / Chapters", fontSize = 11.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = DarkSurfaceVariant,
                                labelColor = CyanAccent
                            )
                        )
                        SuggestionChip(
                            onClick = { configText = configRepository.getAnonymityTemplate() },
                            label = { Text("Proxy & UA", fontSize = 11.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                    }

                    OutlinedTextField(
                        value = configText,
                        onValueChange = {
                            configText = it
                            validationError = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .testTag("config_json_editor"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CodeBackground,
                            unfocusedContainerColor = CodeBackground,
                            focusedBorderColor = AmberPrimary,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    )

                    if (validationError != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ErrorRed.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Validation Error: $validationError",
                                color = ErrorRed,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isSaving = true
                                    val (success, err) = configRepository.saveConfig(configText)
                                    isSaving = false
                                    if (success) {
                                        validationError = null
                                        Toast.makeText(context, "config.json saved successfully", Toast.LENGTH_SHORT).show()
                                    } else {
                                        validationError = err
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberPrimary,
                                contentColor = DarkBackground
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("save_config_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Configuration", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    configText = configRepository.getDefaultTemplate()
                                    configRepository.saveConfig(configText)
                                    validationError = null
                                    Toast.makeText(context, "Reset to default template", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Reset")
                        }
                    }
                }
            }
        } else {
            // cookies.txt Section
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Netscape HTTP Cookie File",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    Text(
                        text = "Paste your exported cookies.txt content here for authenticated extractors (e.g., Pixiv, Twitter, DeviantArt, Patreon). When saved, gallery-dl can reference this cookie jar.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )

                    OutlinedTextField(
                        value = cookiesText,
                        onValueChange = { cookiesText = it },
                        placeholder = {
                            Text(
                                text = "# Netscape HTTP Cookie File\n# http://curl.haxx.se/rfc/cookie_spec.html\n.example.com\tTRUE\t/\tFALSE\t1700000000\tsession_id\tabc123",
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .testTag("cookies_txt_editor"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CodeBackground,
                            unfocusedContainerColor = CodeBackground,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    )

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val saved = configRepository.saveCookies(cookiesText)
                                if (saved) {
                                    Toast.makeText(context, "cookies.txt saved successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to save cookies", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanAccent,
                            contentColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("save_cookies_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save cookies.txt", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
