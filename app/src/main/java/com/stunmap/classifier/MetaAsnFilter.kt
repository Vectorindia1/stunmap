package com.stunmap.classifier

import android.content.Context
import com.stunmap.util.CidrMatcher
import com.stunmap.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetaAsnFilter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var matchers: List<CidrMatcher> = emptyList()

    init {
        load()
    }

    private fun load() {
        try {
            val internalFile = File(context.filesDir, Constants.META_ASN_ASSET_FILE)
            val lines = if (internalFile.exists()) {
                internalFile.readLines()
            } else {
                context.assets.open(Constants.META_ASN_ASSET_FILE).bufferedReader().readLines()
            }
            matchers = lines
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .mapNotNull { line ->
                    val cidr = line.split("#")[0].trim()
                    CidrMatcher.parse(cidr)
                }
            Timber.d("MetaAsnFilter loaded ${matchers.size} CIDR ranges")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load Meta ASN filter")
        }
    }

    fun contains(ip: String): Boolean = matchers.any { it.contains(ip) }

    fun reload() = load()
}
