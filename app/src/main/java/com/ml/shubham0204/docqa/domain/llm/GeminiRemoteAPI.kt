package com.ml.shubham0204.docqa.domain.llm

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GeminiRemoteAPI(
    private val apiKey: String,
) : LLMProvider {
    private val generativeModel: GenerativeModel

    init {
        // Here's a good reference on topK, topP and temperature
        // parameters, which are used to control the output of a LLM
        // See
        // https://ivibudh.medium.com/a-guide-to-controlling-llm-model-output-exploring-top-k-top-p-and-temperature-parameters-ed6a31313910
        val configBuilder = GenerationConfig.Builder()
        configBuilder.topP = 0.4f
        configBuilder.temperature = 0.3f
        generativeModel =
            GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = apiKey,
                generationConfig = configBuilder.build(),
                systemInstruction =
                    content {
                        text(
                            "You are an intelligent search engine. You will be provided with some retrieved context, as well as the users query. Your job is to understand the request, and answer based on the retrieved context.",
                        )
                    },
            )
    }

    override fun generateResponse(prompt: String): Flow<String> {
        Log.d("APP", "Prompt given: $prompt")
        return generativeModel.generateContentStream(prompt).map { it.text ?: "" }
    }

    override fun close() {
        // No cleanup needed for remote API
    }
}
