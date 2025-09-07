package com.ml.EdgeBrain.docqa.domain.actions

import com.ml.EdgeBrain.docqa.domain.embeddings.SentenceEmbeddingProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.Context
import android.util.Log

class ActionMatcher(
    private val sentenceEncoder: SentenceEmbeddingProvider,
    private val context: Context
) {

    private var actions = getPredefinedActions(context)
    private val similarityThreshold = 0.6f
    private val appActionSimilarityThreshold = 0.5f // Lower threshold for app actions

    init {
        CoroutineScope(Dispatchers.IO).launch {
            if (sentenceEncoder.isReady()) {
                Log.d("ActionMatcher", "Initializing action embeddings...")
                actions.forEach { action ->
                    try {
                        // Use the first description's embedding for now
                        // TODO: Consider averaging embeddings of all descriptions for better matching
                        action.embedding = sentenceEncoder.encodeText(action.descriptions.first())
                    } catch (e: Exception) {
                        Log.e("ActionMatcher", "Failed to create embedding for action ${action.id}: ${e.message}", e)
                        action.embedding = null
                    }
                }
                Log.d("ActionMatcher", "Action embeddings initialization complete")
            } else {
                Log.w("ActionMatcher", "Sentence encoder not ready, skipping action embeddings initialization")
            }
        }
    }

    fun executeAction(action: AppAction, query: String): String {
        return action.action(context, query) ?: action.response
    }
    
    fun retryEmbeddingInitialization() {
        if (sentenceEncoder.isReady() && actions.any { it.embedding == null }) {
            Log.d("ActionMatcher", "Retrying action embeddings initialization...")
            CoroutineScope(Dispatchers.IO).launch {
                actions.forEach { action ->
                    if (action.embedding == null) {
                        try {
                            action.embedding = sentenceEncoder.encodeText(action.descriptions.first())
                        } catch (e: Exception) {
                            Log.e("ActionMatcher", "Failed to create embedding for action ${action.id}: ${e.message}", e)
                        }
                    }
                }
                Log.d("ActionMatcher", "Action embeddings retry complete")
            }
        }
    }

    fun findBestAction(query: String): AppAction? {
        val queryLower = query.lowercase().trim()
        Log.d("ActionMatcher", "Looking for action with query: '$queryLower'")
        Log.d("ActionMatcher", "Total actions available: ${actions.size}")
        Log.d("ActionMatcher", "Sentence encoder ready: ${sentenceEncoder.isReady()}")
        
        // First, check for exact keyword matches (highest priority) - this works even without embeddings
        val exactMatches = actions.filter { action ->
            action.descriptions.any { description ->
                queryLower.contains(description.lowercase())
            }
        }
        
        Log.d("ActionMatcher", "Exact matches found: ${exactMatches.size}")
        if (exactMatches.isNotEmpty()) {
            // Return the first exact match (prioritize specific actions over general ones)
            Log.d("ActionMatcher", "Returning exact match: ${exactMatches.first().id}")
            return exactMatches.first()
        }

        // Only use semantic similarity if sentence encoder is ready and embeddings are available
        if (!sentenceEncoder.isReady() || actions.any { it.embedding == null }) {
            Log.d("ActionMatcher", "Sentence encoder not ready or embeddings not available, skipping semantic similarity")
            return null // Semantic similarity not available, but exact matches would have been found above
        }

        try {
            Log.d("ActionMatcher", "Trying semantic similarity matching")
            val queryEmbedding = sentenceEncoder.encodeText(query)
            var bestAction: AppAction? = null
            var maxSimilarity = 0f

            actions.forEach { action ->
                val similarity = cosineSimilarity(queryEmbedding, action.embedding!!)
                if (similarity > maxSimilarity) {
                    maxSimilarity = similarity
                    bestAction = action
                }
            }

            // Use different thresholds based on action type
            val threshold = if (bestAction?.id == "open_application") {
                appActionSimilarityThreshold
            } else {
                similarityThreshold
            }

            Log.d("ActionMatcher", "Best semantic match: ${bestAction?.id} with similarity: $maxSimilarity (threshold: $threshold)")
            return if (maxSimilarity > threshold) {
                bestAction
            } else {
                null
            }
        } catch (e: Exception) {
            // If sentence encoder fails, fall back to exact matching only
            Log.e("ActionMatcher", "Sentence encoder failed: ${e.message}", e)
            return null
        }
        
        Log.d("ActionMatcher", "No action found for query: '$query'")
    }

    private fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
        var dotProduct = 0.0f
        var norm1 = 0.0f
        var norm2 = 0.0f
        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }
        return dotProduct / (kotlin.math.sqrt(norm1) * kotlin.math.sqrt(norm2))
    }
} 