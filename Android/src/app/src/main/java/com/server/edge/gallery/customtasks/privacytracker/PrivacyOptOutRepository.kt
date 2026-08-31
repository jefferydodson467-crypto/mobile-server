package com.server.edge.gallery.customtasks.privacytracker

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class PrivacyOptOutRepository @Inject constructor(@ApplicationContext context: Context) {
  private val prefs =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val storageJson = Json { ignoreUnknownKeys = true }
  private val exportJson = Json { ignoreUnknownKeys = true; prettyPrint = true }

  fun hasSavedEntries(): Boolean {
    return prefs.contains(KEY_ENTRIES)
  }

  fun loadEntries(): Result<List<PrivacyBrokerEntry>> {
    val raw = prefs.getString(KEY_ENTRIES, null) ?: return Result.success(emptyList())
    return runCatching { storageJson.decodeFromString<List<PrivacyBrokerEntry>>(raw) }
  }

  fun saveEntries(entries: List<PrivacyBrokerEntry>) {
    prefs.edit { putString(KEY_ENTRIES, storageJson.encodeToString(entries)) }
  }

  fun importEntries(rawJson: String): List<PrivacyBrokerEntry> {
    val wrapped = runCatching { exportJson.decodeFromString<PrivacyTrackerExport>(rawJson) }.getOrNull()
    val entries =
      wrapped?.entries
        ?: runCatching { exportJson.decodeFromString<List<PrivacyBrokerEntry>>(rawJson) }
          .getOrElse { throw SerializationException("Invalid import format.") }
    return entries
      .map { entry ->
        val subject =
          if (entry.requestSubject.isBlank()) {
            defaultSubject(entry.brokerName, entry.requestType)
          } else {
            entry.requestSubject
          }
        val body =
          if (entry.requestBody.isBlank()) {
            defaultRequestBody(entry.brokerName, entry.requestType)
          } else {
            entry.requestBody
          }
        entry.copy(requestSubject = subject, requestBody = body, updatedAt = nowTimestamp())
      }
      .distinctBy { it.id }
  }

  fun exportEntries(entries: List<PrivacyBrokerEntry>): String {
    return exportJson.encodeToString(PrivacyTrackerExport(entries = entries))
  }

  companion object {
    private const val PREFS_NAME = "privacy_opt_out_tracker"
    private const val KEY_ENTRIES = "entries"
  }
}
