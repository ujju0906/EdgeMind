package com.ml.EdgeMind.docqa.data

import android.content.Context
import io.objectbox.BoxStore

object ObjectBoxStore {
    lateinit var store: BoxStore
        private set

    fun init(context: Context) {
        store = MyObjectBox.builder().androidContext(context).build()
        android.util.Log.d("ObjectBoxStore", "ObjectBox store initialized successfully")
    }
}
