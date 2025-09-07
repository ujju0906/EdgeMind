package com.ml.EdgeMind.docqa.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

@Entity
data class Chunk(
    @Id var chunkId: Long = 0,
    @Index var docId: Long = 0,
    var docFileName: String = "",
    var chunkData: String = "",
    @HnswIndex(dimensions = 384) var chunkEmbedding: FloatArray = floatArrayOf(),
)

@Entity
data class Document(
    @Id var docId: Long = 0,
    var docText: String = "",
    var docFileName: String = "",
    var docAddedTime: Long = 0,
)

@Entity
data class ChatMessage(
    @Id var messageId: Long = 0,
    var question: String = "",
    var response: String = "",
    var timestamp: Long = 0,
    var isUserMessage: Boolean = true,
    var contextUsed: String = "", // Store what context was used (SMS, Call Logs, Documents)
    var detailedContext: String = "", // Store the actual context content used for generation
)

data class RetrievedContext(
    val fileName: String,
    val context: String,
)
