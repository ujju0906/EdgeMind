package com.example.build

import java.io.File
import java.util.Properties

fun loadProperties(file: File): Properties {
    return Properties().apply {
        if (file.exists()) {
            file.inputStream().use { load(it) }
        }
    }
} 