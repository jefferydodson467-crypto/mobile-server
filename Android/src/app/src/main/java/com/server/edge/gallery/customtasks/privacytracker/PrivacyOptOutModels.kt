package com.server.edge.gallery.customtasks.privacytracker

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PrivacyRequestType {
  @SerialName("opt_out") OPT_OUT,
  @SerialName("deletion") DELETION,
  @SerialName("access") ACCESS,
}

@Serializable
enum class PrivacyRequestStatus {
  @SerialName("not_started") NOT_STARTED,
  @SerialName("draft_ready") DRAFT_READY,
  @SerialName("submitted") SUBMITTED,
  @SerialName("follow_up_due") FOLLOW_UP_DUE,
  @SerialName("completed") COMPLETED,
}

@Serializable
data class PrivacyHistoryEntry(
  val timestamp: String = nowTimestamp(),
  val title: String,
  val notes: String = "",
)

@Serializable
data class PrivacyBrokerEntry(
  val id: String = UUID.randomUUID().toString(),
  val brokerName: String,
  val region: String = "Global",
  val websiteUrl: String = "",
  val requestEmail: String = "",
  val requestType: PrivacyRequestType = PrivacyRequestType.OPT_OUT,
  val status: PrivacyRequestStatus = PrivacyRequestStatus.NOT_STARTED,
  val requestSubject: String = "",
  val requestBody: String = "",
  val notes: String = "",
  val evidence: String = "",
  val requestedOn: String = "",
  val followUpDueOn: String = "",
  val completedOn: String = "",
  val history: List<PrivacyHistoryEntry> = emptyList(),
  val createdAt: String = nowTimestamp(),
  val updatedAt: String = nowTimestamp(),
)

@Serializable
data class PrivacyTrackerExport(
  val version: Int = 1,
  val exportedAt: String = nowTimestamp(),
  val entries: List<PrivacyBrokerEntry>,
)

data class PrivacyOptOutUiState(
  val entries: List<PrivacyBrokerEntry> = emptyList(),
  val selectedEntryId: String? = null,
  val statusFilter: PrivacyStatusFilter = PrivacyStatusFilter.ALL,
  val importExportText: String = "",
  val infoMessage: String = "",
  val errorMessage: String = "",
  val guidanceAccepted: Boolean = false,
)

enum class PrivacyStatusFilter {
  ALL,
  ACTION_NEEDED,
  SUBMITTED,
  COMPLETED,
}

const val DEFAULT_SUBJECT_PREFIX = "Privacy request:"

fun nowTimestamp(): String = OffsetDateTime.now(ZoneOffset.UTC).toString()

fun todayIsoDate(): String = LocalDate.now().toString()

fun defaultFollowUpDate(daysFromNow: Long = 30): String = LocalDate.now().plusDays(daysFromNow).toString()

fun isValidIsoDate(value: String): Boolean {
  if (value.isBlank()) return true
  return runCatching { LocalDate.parse(value) }.isSuccess
}

fun isOverdue(entry: PrivacyBrokerEntry): Boolean {
  if (entry.followUpDueOn.isBlank()) return false
  val dueDate = runCatching { LocalDate.parse(entry.followUpDueOn) }.getOrNull() ?: return false
  return dueDate.isBefore(LocalDate.now()) &&
    entry.status != PrivacyRequestStatus.COMPLETED
}

fun starterPrivacyBrokerEntries(): List<PrivacyBrokerEntry> {
  return listOf(
    starterBroker(
      brokerName = "Acxiom",
      region = "US",
      websiteUrl = "https://www.acxiom.com/privacy/",
      requestEmail = "privacy@acxiom.com",
      requestType = PrivacyRequestType.OPT_OUT,
    ),
    starterBroker(
      brokerName = "Experian",
      region = "US",
      websiteUrl = "https://www.experian.com/privacy/consumer-privacy.html",
      requestEmail = "consumer.support@experian.com",
      requestType = PrivacyRequestType.DELETION,
    ),
    starterBroker(
      brokerName = "LexisNexis Risk Solutions",
      region = "US",
      websiteUrl = "https://consumer.risk.lexisnexis.com/",
      requestEmail = "privacyinfo@lexisnexisrisk.com",
      requestType = PrivacyRequestType.ACCESS,
    ),
    starterBroker(
      brokerName = "Oracle Advertising",
      region = "Global",
      websiteUrl = "https://www.oracle.com/legal/privacy/privacy-choices.html",
      requestEmail = "privacy_ww@oracle.com",
      requestType = PrivacyRequestType.OPT_OUT,
    ),
    starterBroker(
      brokerName = "Equifax",
      region = "US",
      websiteUrl = "https://www.equifax.com/personal/help/article-list/-/h/a/privacy-statement/",
      requestEmail = "customerservice@equifax.com",
      requestType = PrivacyRequestType.ACCESS,
    ),
  )
}

private fun starterBroker(
  brokerName: String,
  region: String,
  websiteUrl: String,
  requestEmail: String,
  requestType: PrivacyRequestType,
): PrivacyBrokerEntry {
  val subject = defaultSubject(brokerName = brokerName, requestType = requestType)
  return PrivacyBrokerEntry(
    brokerName = brokerName,
    region = region,
    websiteUrl = websiteUrl,
    requestEmail = requestEmail,
    requestType = requestType,
    status = PrivacyRequestStatus.DRAFT_READY,
    requestSubject = subject,
    requestBody =
      defaultRequestBody(
        brokerName = brokerName,
        requestType = requestType,
      ),
    followUpDueOn = defaultFollowUpDate(),
    history = listOf(PrivacyHistoryEntry(title = "Starter entry added")),
  )
}

fun defaultSubject(brokerName: String, requestType: PrivacyRequestType): String {
  val requestLabel =
    when (requestType) {
      PrivacyRequestType.OPT_OUT -> "opt-out"
      PrivacyRequestType.DELETION -> "deletion"
      PrivacyRequestType.ACCESS -> "access"
    }
  return "$DEFAULT_SUBJECT_PREFIX $requestLabel for $brokerName"
}

fun defaultRequestBody(brokerName: String, requestType: PrivacyRequestType): String {
  val actionLine =
    when (requestType) {
      PrivacyRequestType.OPT_OUT ->
        "I am requesting that you opt me out of data sale, sharing, profiling, and targeted advertising where applicable."
      PrivacyRequestType.DELETION ->
        "I am requesting deletion of personal data you hold about me, subject to applicable law."
      PrivacyRequestType.ACCESS ->
        "I am requesting access to the categories and specific pieces of personal data you hold about me, where applicable."
    }
  return """
    Hello $brokerName privacy team,

    $actionLine

    Please let me know what verification details you require to process this request. For privacy, I prefer to provide the minimum information necessary.

    Requester name: [fill in only when sending]
    Jurisdiction: [state/country]
    Preferred reply address: [email]

    Please confirm receipt of this request and share your expected response timeline.

    Thank you.
  """.trimIndent()
}
