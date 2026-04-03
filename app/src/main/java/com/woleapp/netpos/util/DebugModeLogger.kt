package com.woleapp.netpos.util

import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object DebugModeLoggerrr {
    // Debug session config (provided by the debug system).
    private const val LOG_PATH =
        "/Users/emmanuelakozi/Downloads/storm_pos/.cursor/debug-886a3d.log"

    private const val SESSION_ID = "886a3d"
    private const val LOG_TAG = "AGENT_DEBUG_MODE"
    private const val SERVER_PATH =
        "/ingest/b97a4ffe-6107-4903-8f65-b7a78af585fb"

    // From Android, `127.0.0.1` refers to the device/emulator itself.
    // - Emulator usually reaches host via 10.0.2.2
    // - Physical device needs a LAN IP (not handled automatically here)
    private val SERVER_BASE_CANDIDATES = listOf(
        "http://127.0.0.1:7460",
        "http://10.0.2.2:7460",
    )

    fun log(
        hypothesisId: String,
        location: String,
        message: String,
        data: Map<String, Any?> = emptyMap(),
        runId: String = "pre-fix",
    ) {
        val timestamp = System.currentTimeMillis()
        val payload = JSONObject().apply {
            put("sessionId", SESSION_ID)
            put("runId", runId)
            put("hypothesisId", hypothesisId)
            put("location", location)
            put("message", message)
            put("timestamp", timestamp)

            val dataObj = JSONObject()
            for ((k, v) in data) {
                if (v == null) continue
                dataObj.put(k, v)
            }
            put("data", dataObj)
        }

        // Also emit to Logcat so you can immediately verify instrumentation.
        // Avoid printing any sensitive values; data is expected to be masked/booleans/lengths.
        Log.d(
            LOG_TAG,
            "agentlog hypothesisId=$hypothesisId location=$location message=$message runId=$runId dataKeys=${data.keys.joinToString(",")}"
        )

        // Primary: send to the ingest endpoint so the host writes NDJSON to the configured log file.
        for (base in SERVER_BASE_CANDIDATES) {
            val serverEndpoint = base + SERVER_PATH
            try {
                val url = URL(serverEndpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("X-Debug-Session-Id", SESSION_ID)
                conn.doOutput = true
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }
                // Force read to complete request
                conn.responseCode
                Log.d(LOG_TAG, "agentlog sent to ingest endpoint: $serverEndpoint")
                return
            } catch (t: Throwable) {
                Log.e(
                    LOG_TAG,
                    "agentlog HTTP send failed for $serverEndpoint (${t.javaClass.simpleName}: ${t.message})"
                )
            }
        }

        // Fallback: file append (may not work on-device, but harmless if it fails).
        try {
            val file = File(LOG_PATH)
            file.parentFile?.mkdirs()
            file.appendText(payload.toString() + "\n", Charsets.UTF_8)
            Log.d(LOG_TAG, "agentlog write ok to $LOG_PATH")
        } catch (t: Throwable) {
            // Never break the payment flow if debug logging fails.
            Log.e(LOG_TAG, "agentlog file write failed (${t.javaClass.simpleName}: ${t.message})")
        }
    }
}

