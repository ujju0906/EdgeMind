package com.ml.shubham0204.docqa.domain.actions

import com.ml.shubham0204.docqa.domain.embeddings.SentenceEmbeddingProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.Context

class ActionMatcher(
    private val sentenceEncoder: SentenceEmbeddingProvider,
    private val context: Context
) {

    private var actions = getPredefinedActions(context)
    private val similarityThreshold = 0.7f
    private val appActionSimilarityThreshold = 0.5f // Lower threshold for app actions

    init {
        CoroutineScope(Dispatchers.IO).launch {
            actions.forEach { action ->
                // Use the first description's embedding for now
                // TODO: Consider averaging embeddings of all descriptions for better matching
                action.embedding = sentenceEncoder.encodeText(action.descriptions.first())
            }
        }
    }

    fun executeAction(action: AppAction, query: String): String {
        return action.action(context, query) ?: action.response
    }

    fun findBestAction(query: String): AppAction? {
        if (actions.any { it.embedding == null }) {
            return null // Embeddings are not ready yet
        }

        val queryLower = query.lowercase().trim()
        
        // First, check for exact keyword matches (highest priority)
        val exactMatches = actions.filter { action ->
            action.descriptions.any { description ->
                queryLower.contains(description.lowercase())
            }
        }
        
        if (exactMatches.isNotEmpty()) {
            // Return the first exact match (prioritize specific actions over general ones)
            return exactMatches.first()
        }

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

        return if (maxSimilarity > threshold) {
            bestAction
        } else {
            null
        }
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