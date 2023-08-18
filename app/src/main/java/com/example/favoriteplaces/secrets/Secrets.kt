package com.example.favoriteplaces.secrets


import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.util.Properties

object Secrets {
    private val properties = Properties()

    fun init(localPropertiesFilePath: String) {
        val localPropertiesFile = File(localPropertiesFilePath)
        if (localPropertiesFile.exists()){
            properties.load(FileInputStream(localPropertiesFile))
        }else{
            Log.e("Main", "local.properties file not found")
        }
    }

    val mapsApiKey: String
        get() = properties.getProperty("MAPS_API_KEY", "")

}