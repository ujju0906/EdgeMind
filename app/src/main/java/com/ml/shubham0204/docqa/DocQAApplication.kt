package com.ml.shubham0204.docqa

import android.app.Application
import com.ml.shubham0204.docqa.data.ObjectBoxStore
import com.ml.shubham0204.docqa.di.appModule
import com.ml.shubham0204.docqa.domain.llm.AppLLMProvider
import com.ml.shubham0204.docqa.domain.llm.LLMFactory
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DocQAApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@DocQAApplication)
            modules(appModule)
        }
        ObjectBoxStore.init(this)
        AppLLMProvider.initialize(get())
    }
}
