package com.ml.shubham0204.docqa.di

import com.ml.shubham0204.docqa.data.ChatHistoryDB
import com.ml.shubham0204.docqa.data.ChatMessage
import com.ml.shubham0204.docqa.data.ChunksDB
import com.ml.shubham0204.docqa.data.DocumentsDB
import com.ml.shubham0204.docqa.data.GeminiAPIKey
import com.ml.shubham0204.docqa.data.ObjectBoxStore
import com.ml.shubham0204.docqa.data.SettingsRepository
import com.ml.shubham0204.docqa.domain.actions.ActionMatcher
import com.ml.shubham0204.docqa.domain.embeddings.SentenceEmbeddingProvider
import com.ml.shubham0204.docqa.domain.llm.LLMFactory
import com.ml.shubham0204.docqa.domain.llm.ModelManager
import com.ml.shubham0204.docqa.domain.sms.CallLogsReader
import com.ml.shubham0204.docqa.domain.sms.SmsReader
import com.ml.shubham0204.docqa.ui.screens.advanced_options.AdvancedOptionsViewModel
import com.ml.shubham0204.docqa.ui.screens.chat.ChatViewModel
import com.ml.shubham0204.docqa.ui.screens.docs.DocsViewModel
import com.ml.shubham0204.docqa.ui.screens.edit_api_key.EditAPIKeyViewModel
import com.ml.shubham0204.docqa.ui.screens.model_download.ModelDownloadViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // Data Layer
    single { DocumentsDB() }
    single { ChunksDB() }
    single {
        val chatHistoryDB = ChatHistoryDB(ObjectBoxStore.store.boxFor(ChatMessage::class.java))
        android.util.Log.d("AppModule", "ChatHistoryDB created successfully")
        chatHistoryDB
    }
    single { GeminiAPIKey(androidContext()) }
    single { SettingsRepository(androidContext()) }

    // Domain Layer
    single { SentenceEmbeddingProvider(androidContext()) }
    single { ModelManager(androidContext()) }
    single { LLMFactory(context = androidContext(), geminiAPIKey = get(), modelManager = get(), settingsRepository = get()) }
    single { SmsReader(androidContext()) }
    single { CallLogsReader(androidContext()) }
    single { ActionMatcher(get(), androidContext()) }

    // UI Layer (ViewModels)
    viewModel { ChatViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { DocsViewModel(get(), get(), get()) }
    viewModel { EditAPIKeyViewModel(get()) }
    viewModel { ModelDownloadViewModel(get(), androidContext(), get()) }
    viewModel { AdvancedOptionsViewModel(get()) }
}
