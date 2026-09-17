package com.stunmap.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.stunmap.session.CaptureSession
import com.stunmap.session.StunHit
import com.stunmap.geo.GeoResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun exportAsJson(
        context: Context,
        session: CaptureSession,
        hits: List<StunHit>,
        geoResults: Map<String, GeoResult>
    ): Intent? {
        return try {
            val data = mapOf(
                "session" to session,
                "stun_hits" to hits,
                "geo_results" to geoResults.values.toList()
            )
            val jsonString = json.encodeToString(data)
            val fileName = "stunmap_session_${session.id.take(8)}.json"
            writeAndShare(context, fileName, jsonString, "application/json")
        } catch (e: Exception) {
            Timber.e(e, "JSON export failed")
            null
        }
    }

    fun exportAsText(
        context: Context,
        session: CaptureSession,
        hits: List<StunHit>,
        geoResults: Map<String, GeoResult>
    ): Intent? {
        return try {
            val sb = StringBuilder()
            sb.appendLine("STUNMAP Session Report")
            sb.appendLine("=" .repeat(40))
            sb.appendLine("Session ID: ${session.id}")
            sb.appendLine("Started: ${dateFormat.format(Date(session.startedAt))}")
            session.endedAt?.let { sb.appendLine("Ended: ${dateFormat.format(Date(it))}") }
            sb.appendLine("Device IP: ${session.devicePublicIp}")
            sb.appendLine("STUN Hits: ${session.stunHitCount}")
            sb.appendLine("Candidate IPs: ${session.candidateCount}")
            sb.appendLine()

            val candidates = hits.filter { it.classification.name == "CANDIDATE" }
                .mapNotNull { it.mappedIp }
                .distinct()

            if (candidates.isEmpty()) {
                sb.appendLine("No candidate IPs detected (possible TURN relay).")
            } else {
                sb.appendLine("CANDIDATE IPs:")
                sb.appendLine("-".repeat(40))
                for (ip in candidates) {
                    sb.appendLine("IP: $ip")
                    geoResults[ip]?.let { geo ->
                        geo.city?.let { sb.appendLine("  City: $it") }
                        geo.region?.let { sb.appendLine("  Region: $it") }
                        geo.country?.let { sb.appendLine("  Country: $it") }
                        geo.org?.let { sb.appendLine("  ISP: $it") }
                        geo.asn?.let { sb.appendLine("  ASN: $it") }
                        if (geo.latitude != null && geo.longitude != null) {
                            sb.appendLine("  Coords: ${geo.latitude}, ${geo.longitude}")
                        }
                    }
                    sb.appendLine()
                }
            }

            val fileName = "stunmap_session_${session.id.take(8)}.txt"
            writeAndShare(context, fileName, sb.toString(), "text/plain")
        } catch (e: Exception) {
            Timber.e(e, "Text export failed")
            null
        }
    }

    private fun writeAndShare(
        context: Context,
        fileName: String,
        content: String,
        mimeType: String
    ): Intent {
        val file = File(context.cacheDir, fileName)
        file.writeText(content)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
