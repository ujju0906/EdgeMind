package com.ml.EdgeMind.docqa.domain.llm

import kotlinx.coroutines.flow.Flow

interface LLMProvider {
    suspend fun init()
    fun generateResponse(prompt: String): Flow<String>
    fun stopGeneration()
    fun close()
} 