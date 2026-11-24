package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ai.TranslationUiState

private val supportedTargetLanguages = listOf(
    "English", "Hindi", "Spanish", "French", "Arabic", "Portuguese", "German", "Japanese"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationBottomSheet(
    sourceText: String,
    targetLanguage: String,
    translationState: TranslationUiState,
    onSourceTextChange: (String) -> Unit,
    onTargetLanguageSelected: (String) -> Unit,
    onTranslateClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var copiedNotice by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Translate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.translate_sheet_title),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                supportedTargetLanguages.forEach { lang ->
                    FilterChip(
                        selected = lang == targetLanguage,
                        onClick = {
                            onTargetLanguageSelected(lang)
                            if (sourceText.isNotBlank()) {
                                onTranslateClick(sourceText)
                            }
                        },
                        label = { Text(lang) }
                    )
                }
            }

            OutlinedTextField(
                value = sourceText,
                onValueChange = onSourceTextChange,
                label = { Text("Caption, comment, or DM text") },
                placeholder = { Text("Paste or edit text to translate...") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("translate_source_input")
            )

            Button(
                onClick = {
                    copiedNotice = false
                    onTranslateClick(sourceText)
                },
                enabled = sourceText.isNotBlank() && translationState !is TranslationUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("translate_submit_button")
            ) {
                Text("Translate to $targetLanguage")
            }

            when (translationState) {
                TranslationUiState.Idle -> Unit
                TranslationUiState.Loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = "Translating...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is TranslationUiState.Streaming -> {
                    TranslationOutputCard(
                        text = translationState.translatedText,
                        isStreaming = true,
                        copiedNotice = false,
                        onCopyClick = {}
                    )
                }
                is TranslationUiState.Success -> {
                    TranslationOutputCard(
                        text = translationState.translatedText,
                        isStreaming = false,
                        copiedNotice = copiedNotice,
                        onCopyClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText("InstaPlus Translation", translationState.translatedText)
                            )
                            copiedNotice = true
                        }
                    )
                }
                is TranslationUiState.Error -> {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = translationState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TranslationOutputCard(
    text: String,
    isStreaming: Boolean,
    copiedNotice: Boolean,
    onCopyClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isStreaming) "Translating live..." else "Translated Result",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                if (!isStreaming) {
                    OutlinedButton(
                        onClick = onCopyClick,
                        modifier = Modifier.testTag("copy_translation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy translation",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (copiedNotice) "Copied" else "Copy")
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
