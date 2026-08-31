package com.server.edge.gallery.customtasks.privacytracker

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivacyOptOutRepository @Inject constructor(@ApplicationContext context: Context) {
  private val prefs =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val gson = Gson()
  private val entryListType = object : TypeToken<List<PrivacyBrokerEntry>>() {}.type

  fun loadEntries(): List<PrivacyBrokerEntry> {
    val json = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
    return runCatching { gson.fromJson<List<PrivacyBrokerEntry>>(json, entryListType) ?: emptyList() }
      .getOrDefault(emptyList())
  }

  fun saveEntries(entries: List<PrivacyBrokerEntry>) {
    prefs.edit { putString(KEY_ENTRIES, gson.toJson(entries)) }
  }

  fun importEntries(json: String): List<PrivacyBrokerEntry> {
    val wrapped = runCatching { gson.fromJson(json, PrivacyTrackerExport::class.java) }.getOrNull()
    val entries =
      wrapped?.entries
        ?: runCatching { gson.fromJson<List<PrivacyBrokerEntry>>(json, entryListType) }
          .getOrElse { throw JsonParseException("Invalid import format.") }
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
    return gson.toJson(PrivacyTrackerExport(entries = entries))
  }

  companion object {
    private const val PREFS_NAME = "privacy_opt_out_tracker"
    private const val KEY_ENTRIES = "entries"
  }
}
