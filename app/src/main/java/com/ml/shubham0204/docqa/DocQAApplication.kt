package com.ml.shubham0204.docqa

import android.app.Application
import com.ml.shubham0204.docqa.data.ObjectBoxStore
import com.ml.shubham0204.docqa.di.appModule
import com.ml.shubham0204.docqa.domain.llm.AppLLMProvider
import com.ml.shubham0204.docqa.domain.llm.LLMFactory
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DocQAApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@DocQAApplication)
            modules(appModule)
        }
        ObjectBoxStore.init(this)
        
        // Launch in a background thread with a delay to avoid race conditions with Koin
        CoroutineScope(Dispatchers.IO).launch {
            delay(500) // Small delay to ensure Koin is ready
            AppLLMProvider.initialize(get())
        }
    }
}
