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
    private val similarityThreshold = 0.5f

    init {
        CoroutineScope(Dispatchers.IO).launch {
            actions.forEach { action ->
                // For simplicity, we'll use the first description's embedding.
                // A more robust approach could average the embeddings of all descriptions.
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

        return if (maxSimilarity > similarityThreshold) {
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