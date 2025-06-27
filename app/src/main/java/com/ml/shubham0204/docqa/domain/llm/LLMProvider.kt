package com.ml.shubham0204.docqa.domain.llm

import kotlinx.coroutines.flow.Flow

interface LLMProvider {
    fun generateResponse(prompt: String): Flow<String>
    fun close()
} 