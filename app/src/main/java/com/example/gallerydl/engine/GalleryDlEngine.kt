package com.example.gallerydl.engine

import android.content.Context
import android.util.Log
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.gallerydl.data.models.ExtractorInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object GalleryDlEngine {
    private const val TAG = "GalleryDlEngine"
    private var isInitialized = false

    fun init(context: Context) {
        if (!isInitialized) {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context.applicationContext))
            }
            isInitialized = true
            Log.d(TAG, "Python runtime initialized successfully.")
        }
    }

    private fun getRunnerModule(): PyObject {
        val python = Python.getInstance()
        return python.getModule("gallery_runner")
    }

    suspend fun getVersion(): String = withContext(Dispatchers.IO) {
        try {
            val runner = getRunnerModule()
            runner.callAttr("get_version").toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get version", e)
            "1.32.16 (Error: ${e.message})"
        }
    }

    suspend fun getHelpText(): String = withContext(Dispatchers.IO) {
        try {
            val runner = getRunnerModule()
            runner.callAttr("get_help_text").toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get help text", e)
            "Error loading help text: ${e.message}"
        }
    }

    suspend fun fetchExtractors(filterText: String = "", limit: Int = 100): List<ExtractorInfo> = withContext(Dispatchers.IO) {
        try {
            val runner = getRunnerModule()
            val jsonStr = runner.callAttr("list_extractors", filterText, limit).toString()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<ExtractorInfo>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ExtractorInfo(
                        name = obj.optString("name", ""),
                        category = obj.optString("category", ""),
                        subcategory = obj.optString("subcategory", ""),
                        example = obj.optString("example", ""),
                        description = obj.optString("description", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch extractors", e)
            emptyList()
        }
    }

    suspend fun validateConfig(configJson: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val runner = getRunnerModule()
            val resultJson = runner.callAttr("validate_config", configJson).toString()
            val obj = JSONObject(resultJson)
            val valid = obj.optBoolean("valid", false)
            val error = if (obj.isNull("error")) null else obj.optString("error")
            Pair(valid, error)
        } catch (e: Exception) {
            Pair(false, e.message)
        }
    }

    suspend fun execute(
        args: List<String>,
        outputCallback: ProcessOutputCallback
    ): Int = withContext(Dispatchers.IO) {
        try {
            val runner = getRunnerModule()
            val pyArgs = args.toTypedArray()
            val exitCodeObj = runner.callAttr("run_gallery_dl", pyArgs, outputCallback)
            exitCodeObj.toInt()
        } catch (e: Exception) {
            Log.e(TAG, "Execution failure", e)
            outputCallback.onStderr("Execution exception: ${e.localizedMessage}")
            1
        }
    }
}
