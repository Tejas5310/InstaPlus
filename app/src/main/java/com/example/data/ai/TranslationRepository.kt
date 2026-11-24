package com.example.data.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "TranslationRepository"

sealed interface TranslationUiState {
    data object Idle : TranslationUiState
    data object Loading : TranslationUiState
    data class Streaming(val translatedText: String) : TranslationUiState
    data class Success(val translatedText: String) : TranslationUiState
    data class Error(val message: String) : TranslationUiState
}

sealed interface AiCaptionUiState {
    data object Idle : AiCaptionUiState
    data object Loading : AiCaptionUiState
    data class Success(val caption: String, val hashtags: String) : AiCaptionUiState
    data class Error(val message: String) : AiCaptionUiState
}

class TranslationRepository {

    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val captionModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "caption" to Schema.string(description = "Engaging standalone social post caption"),
                "hashtags" to Schema.string(description = "Space-separated relevant hashtags starting with #")
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    fun translateStream(
        sourceText: String,
        targetLanguage: String
    ): Flow<TranslationUiState> = flow {
        val cleaned = sourceText.trim()
        if (cleaned.isBlank()) {
            emit(TranslationUiState.Error("Enter or select a caption, comment, or message to translate."))
            return@flow
        }

        emit(TranslationUiState.Loading)
        val prompt = """
            Translate the following social media caption, comment, or chat message into $targetLanguage.
            Rules:
            - Preserve @mentions, #hashtags, emojis, and natural conversational tone.
            - Auto-detect the original language.
            - Return ONLY the translated text without quotes, labels, or explanations.

            Text:
            $cleaned
        """.trimIndent()

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                model.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    accumulated.append(piece)
                    if (accumulated.isNotBlank()) {
                        emit(TranslationUiState.Streaming(accumulated.toString().trim()))
                    }
                }
            }
            val finalOutput = accumulated.toString().trim()
            if (finalOutput.isNotBlank()) {
                emit(TranslationUiState.Success(finalOutput))
            } else {
                emit(TranslationUiState.Error("Translator returned an empty response. Please try again."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Translation timed out", e)
            emit(TranslationUiState.Error("Translation timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI translation failed", e)
            emit(TranslationUiState.Error(e.localizedMessage ?: "Translation failed."))
        }
    }

    suspend fun generateCaptionAndHashtags(
        topic: String,
        tone: String
    ): Result<Pair<String, String>> {
        val cleanTopic = topic.trim()
        if (cleanTopic.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter a topic or mood first."))
        }
        return try {
            val prompt = """
                Write an authentic social media post caption and 5 matching hashtags for InstaPlus.
                Topic / context: $cleanTopic
                Tone: $tone
                Keep the caption concise (1 to 2 sentences), vivid, and human.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                captionModel.generateContent(prompt)
            }
            val jsonRaw = response.text ?: error("AI model returned an empty caption.")
            val json = JSONObject(jsonRaw)
            val caption = json.optString("caption").trim()
            val hashtags = json.optString("hashtags").trim()
            if (caption.isBlank()) error("Generated caption was empty.")
            Result.success(caption to hashtags)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Caption generation timed out", e)
            Result.failure(IllegalStateException("AI caption generation timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI caption generation failed", e)
            Result.failure(e)
        }
    }
}
