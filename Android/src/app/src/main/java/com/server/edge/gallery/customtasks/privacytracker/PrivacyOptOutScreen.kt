package com.server.edge.gallery.customtasks.privacytracker

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PrivacyOptOutScreen(
  bottomPadding: Dp,
  viewModel: PrivacyOptOutViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()
  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current
  val filteredEntries = filterEntries(uiState.entries, uiState.statusFilter)
  val selectedEntry = uiState.entries.find { it.id == uiState.selectedEntryId }
  val legalHelpProfile = uiState.legalHelpProfile
  val overdueEntries = uiState.entries.filter(::isOverdue)
  val submittedCount =
    uiState.entries.count {
      it.status == PrivacyRequestStatus.SUBMITTED || it.status == PrivacyRequestStatus.FOLLOW_UP_DUE
    }
  val completedCount = uiState.entries.count { it.status == PrivacyRequestStatus.COMPLETED }

  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp)
        .padding(top = 16.dp, bottom = bottomPadding + 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Privacy request tracker", style = MaterialTheme.typography.titleLarge)
        Text(
          "This workflow is limited to lawful privacy management and legal-help preparation. It drafts requests, opens your mail or browser, helps prepare attorney outreach, and records your manual follow-up history. It does not auto-submit requests or alter third-party systems.",
          style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Checkbox(
            checked = uiState.guidanceAccepted,
            onCheckedChange = viewModel::setGuidanceAccepted,
          )
          Text(
            "I understand submissions stay manual and I should store only the minimum necessary personal data.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 12.dp),
          )
        }
      }
    }

    ElevatedCard {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Low-cost legal help finder", style = MaterialTheme.typography.titleMedium)
        Text(
          "Use this section to organize public defender, legal aid, bar referral, and low-cost attorney outreach. It cannot guarantee outcomes or replace legal advice.",
          style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
          value = legalHelpProfile.state,
          onValueChange = { value -> viewModel.updateLegalHelpProfile { it.copy(state = value) } },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("State") },
        )
        OutlinedTextField(
          value = legalHelpProfile.county,
          onValueChange = { value -> viewModel.updateLegalHelpProfile { it.copy(county = value) } },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("County") },
        )
        OutlinedTextField(
          value = legalHelpProfile.chargeLabel,
          onValueChange = { value -> viewModel.updateLegalHelpProfile { it.copy(chargeLabel = value) } },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Charge or case label") },
          placeholder = { Text("Example: misdemeanor domestic assault") },
        )
        OutlinedTextField(
          value = legalHelpProfile.budgetNotes,
          onValueChange = { value -> viewModel.updateLegalHelpProfile { it.copy(budgetNotes = value) } },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Budget / affordability notes") },
          placeholder = { Text("Example: low-cost, payment plan, or court-appointed") },
          minLines = 2,
        )
        DateTextField(
          value = legalHelpProfile.nextCourtDate,
          onValidValue = { value -> viewModel.updateLegalHelpProfile { it.copy(nextCourtDate = value) } },
          label = { Text("Next court date (YYYY-MM-DD)") },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Checkbox(
            checked = legalHelpProfile.publicDefenderRequested,
            onCheckedChange = { checked ->
              viewModel.updateLegalHelpProfile { it.copy(publicDefenderRequested = checked) }
            },
          )
          Text(
            "I have already asked the court about a public defender or indigent-defense application.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 12.dp),
          )
        }
        OutlinedTextField(
          value = legalHelpProfile.gatherDocuments,
          onValueChange = { value -> viewModel.updateLegalHelpProfile { it.copy(gatherDocuments = value) } },
          modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
          label = { Text("Documents to gather") },
          minLines = 4,
        )
        OutlinedTextField(
          value = legalHelpProfile.contactScript,
          onValueChange = { value -> viewModel.updateLegalHelpProfile { it.copy(contactScript = value) } },
          modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
          label = { Text("Call / intake script") },
          minLines = 4,
        )
        OutlinedTextField(
          value = legalHelpProfile.notes,
          onValueChange = { value -> viewModel.updateLegalHelpProfile { it.copy(notes = value) } },
          modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
          label = { Text("Attorney search notes") },
          minLines = 4,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
          Button(
            onClick = {
              clipboardManager.setText(AnnotatedString(buildLegalHelpChecklist(legalHelpProfile)))
              viewModel.onLegalHelpChecklistCopied()
            },
            enabled = hasLegalHelpProfileContent(legalHelpProfile),
          ) { Text("Copy checklist") }
          Button(
            onClick = {
              val error = openBrowser(context, publicDefenderSearchUrl(legalHelpProfile))
              if (error == null) viewModel.onPortalOpened() else viewModel.onExternalActionFailed(error)
            },
            enabled = uiState.guidanceAccepted && legalHelpProfile.state.isNotBlank(),
          ) { Text("Find public defender") }
          Button(
            onClick = {
              val error = openBrowser(context, legalAidSearchUrl(legalHelpProfile))
              if (error == null) viewModel.onPortalOpened() else viewModel.onExternalActionFailed(error)
            },
            enabled = uiState.guidanceAccepted && legalHelpProfile.state.isNotBlank(),
          ) { Text("Find legal aid") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
          Button(
            onClick = {
              val error = openBrowser(context, barReferralSearchUrl(legalHelpProfile))
              if (error == null) viewModel.onPortalOpened() else viewModel.onExternalActionFailed(error)
            },
            enabled = uiState.guidanceAccepted && legalHelpProfile.state.isNotBlank(),
          ) { Text("State bar referral") }
          Button(
            onClick = {
              val error = openBrowser(context, lowCostAttorneySearchUrl(legalHelpProfile))
              if (error == null) viewModel.onPortalOpened() else viewModel.onExternalActionFailed(error)
            },
            enabled = uiState.guidanceAccepted && legalHelpProfile.state.isNotBlank(),
          ) { Text("Low-cost attorney search") }
        }
      }
    }

    ElevatedCard {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Status board", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          SummaryChip("Tracked", uiState.entries.size.toString())
          SummaryChip("Submitted", submittedCount.toString())
          SummaryChip("Overdue", overdueEntries.size.toString())
          SummaryChip("Completed", completedCount.toString())
        }
        if (overdueEntries.isNotEmpty()) {
          Text(
            "Follow-up due: ${overdueEntries.joinToString { it.brokerName }}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    }

    ElevatedCard {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tracker controls", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
          FilterChip(
            selected = uiState.statusFilter == PrivacyStatusFilter.ALL,
            onClick = { viewModel.setStatusFilter(PrivacyStatusFilter.ALL) },
            label = { Text("All") },
          )
          FilterChip(
            selected = uiState.statusFilter == PrivacyStatusFilter.ACTION_NEEDED,
            onClick = { viewModel.setStatusFilter(PrivacyStatusFilter.ACTION_NEEDED) },
            label = { Text("Action needed") },
          )
          FilterChip(
            selected = uiState.statusFilter == PrivacyStatusFilter.SUBMITTED,
            onClick = { viewModel.setStatusFilter(PrivacyStatusFilter.SUBMITTED) },
            label = { Text("Submitted") },
          )
          FilterChip(
            selected = uiState.statusFilter == PrivacyStatusFilter.COMPLETED,
            onClick = { viewModel.setStatusFilter(PrivacyStatusFilter.COMPLETED) },
            label = { Text("Completed") },
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
          Button(onClick = viewModel::addEntry) { Text("Add broker") }
          TextButton(onClick = viewModel::restoreStarterEntries) { Text("Restore starter list") }
          TextButton(onClick = viewModel::exportEntries) { Text("Export JSON") }
        }
        if (uiState.infoMessage.isNotBlank()) {
          Text(uiState.infoMessage, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
        if (uiState.errorMessage.isNotBlank()) {
          Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
      }
    }

    ElevatedCard {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Broker list", style = MaterialTheme.typography.titleMedium)
        filteredEntries.forEach { entry ->
          BrokerListCard(
            entry = entry,
            selected = entry.id == uiState.selectedEntryId,
            onClick = { viewModel.selectEntry(entry.id) },
          )
        }
        if (filteredEntries.isEmpty()) {
          Text("No entries match the current filter.", style = MaterialTheme.typography.bodyMedium)
        }
      }
    }

    if (selectedEntry != null) {
      ElevatedCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Selected broker", style = MaterialTheme.typography.titleMedium)
          OutlinedTextField(
            value = selectedEntry.brokerName,
            onValueChange = { value ->
              viewModel.updateSelectedEntry { entry ->
                val refreshedType = entry.requestType
                entry.copy(
                  brokerName = value,
                  requestSubject =
                    if (entry.requestSubject.startsWith(DEFAULT_SUBJECT_PREFIX)) {
                      defaultSubject(value, refreshedType)
                    } else {
                      entry.requestSubject
                    },
                )
              }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Broker name") },
          )
          OutlinedTextField(
            value = selectedEntry.region,
            onValueChange = { value -> viewModel.updateSelectedEntry { it.copy(region = value) } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Region") },
          )
          OutlinedTextField(
            value = selectedEntry.websiteUrl,
            onValueChange = { value -> viewModel.updateSelectedEntry { it.copy(websiteUrl = value) } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Website or privacy portal") },
          )
          OutlinedTextField(
            value = selectedEntry.requestEmail,
            onValueChange = { value -> viewModel.updateSelectedEntry { it.copy(requestEmail = value) } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Privacy email") },
          )

          Text("Request type", style = MaterialTheme.typography.titleSmall)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            PrivacyRequestType.entries.forEach { type ->
              FilterChip(
                selected = selectedEntry.requestType == type,
                onClick = {
                  viewModel.updateSelectedEntry {
                    it.copy(
                      requestType = type,
                      requestSubject = defaultSubject(it.brokerName, type),
                      requestBody = defaultRequestBody(it.brokerName, type),
                    )
                  }
                },
                label = { Text(type.label()) },
              )
            }
          }

          Text("Status", style = MaterialTheme.typography.titleSmall)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            PrivacyRequestStatus.entries.forEach { status ->
              FilterChip(
                selected = selectedEntry.status == status,
                onClick = { viewModel.updateSelectedEntry { it.copy(status = status) } },
                label = { Text(status.label()) },
              )
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            TextButton(onClick = viewModel::markDraftReady) { Text("Save draft") }
            TextButton(onClick = viewModel::markSubmitted) { Text("Record submit") }
            TextButton(onClick = viewModel::markFollowUpDue) { Text("Follow-up due") }
            TextButton(onClick = viewModel::markCompleted) { Text("Complete") }
          }

          DateTextField(
            value = selectedEntry.requestedOn,
            onValidValue = { value -> viewModel.updateSelectedEntry { it.copy(requestedOn = value) } },
            label = { Text("Requested on (YYYY-MM-DD)") },
          )
          DateTextField(
            value = selectedEntry.followUpDueOn,
            onValidValue = { value -> viewModel.updateSelectedEntry { it.copy(followUpDueOn = value) } },
            label = { Text("Follow-up due (YYYY-MM-DD)") },
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            AssistChip(onClick = { viewModel.setDueInDays(14) }, label = { Text("+14d") })
            AssistChip(onClick = { viewModel.setDueInDays(30) }, label = { Text("+30d") })
            AssistChip(onClick = { viewModel.setDueInDays(45) }, label = { Text("+45d") })
          }
          DateTextField(
            value = selectedEntry.completedOn,
            onValidValue = { value -> viewModel.updateSelectedEntry { it.copy(completedOn = value) } },
            label = { Text("Completed on (YYYY-MM-DD)") },
          )
          OutlinedTextField(
            value = selectedEntry.evidence,
            onValueChange = { value -> viewModel.updateSelectedEntry { it.copy(evidence = value) } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Evidence / confirmation notes") },
            minLines = 2,
          )
          OutlinedTextField(
            value = selectedEntry.notes,
            onValueChange = { value -> viewModel.updateSelectedEntry { it.copy(notes = value) } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Internal notes") },
            minLines = 3,
          )
          TextButton(onClick = viewModel::removeSelectedEntry) { Text("Delete entry") }
        }
      }

      ElevatedCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Request draft", style = MaterialTheme.typography.titleMedium)
          OutlinedTextField(
            value = selectedEntry.requestSubject,
            onValueChange = { value -> viewModel.updateSelectedEntry { it.copy(requestSubject = value) } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email subject") },
          )
          OutlinedTextField(
            value = selectedEntry.requestBody,
            onValueChange = { value -> viewModel.updateSelectedEntry { it.copy(requestBody = value) } },
            modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
            label = { Text("Request body") },
            minLines = 8,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Button(
              onClick = {
                clipboardManager.setText(
                  AnnotatedString("${selectedEntry.requestSubject}\n\n${selectedEntry.requestBody}")
                )
                viewModel.onDraftCopied()
              }
            ) {
              Text("Copy draft")
            }
            Button(
              onClick = {
                val error =
                  openEmailDraft(
                    context = context,
                    address = selectedEntry.requestEmail,
                    subject = selectedEntry.requestSubject,
                    body = selectedEntry.requestBody,
                  )
                if (error == null) {
                  viewModel.onEmailOpened()
                } else {
                  viewModel.onExternalActionFailed(error)
                }
              },
              enabled = uiState.guidanceAccepted && selectedEntry.requestEmail.isNotBlank(),
            ) {
              Text("Open email")
            }
            Button(
              onClick = {
                val error = openBrowser(context, selectedEntry.websiteUrl)
                if (error == null) {
                  viewModel.onPortalOpened()
                } else {
                  viewModel.onExternalActionFailed(error)
                }
              },
              enabled = uiState.guidanceAccepted && selectedEntry.websiteUrl.isNotBlank(),
            ) {
              Text("Open portal")
            }
          }
        }
      }

      ElevatedCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("History", style = MaterialTheme.typography.titleMedium)
          if (selectedEntry.history.isEmpty()) {
            Text("No history recorded yet.", style = MaterialTheme.typography.bodyMedium)
          } else {
            selectedEntry.history.reversed().forEach { item ->
              Card {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(item.title, style = MaterialTheme.typography.titleSmall)
                  Text(item.timestamp, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  if (item.notes.isNotBlank()) {
                    Text(item.notes, style = MaterialTheme.typography.bodySmall)
                  }
                }
              }
            }
          }
        }
      }
    }

    ElevatedCard {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Import / export", style = MaterialTheme.typography.titleMedium)
        Text(
          "Use JSON export for backups or moving this tracker to another device. Imports replace the current tracker state.",
          style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
          value = uiState.importExportText,
          onValueChange = viewModel::updateImportExportText,
          modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
          label = { Text("Tracker JSON") },
          minLines = 8,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
          Button(onClick = viewModel::exportEntries) { Text("Refresh export") }
          Button(onClick = viewModel::importEntries) { Text("Import replace") }
          TextButton(onClick = {
            clipboardManager.setText(AnnotatedString(uiState.importExportText))
          }) {
            Text("Copy JSON")
          }
        }
      }
    }
  }
}

@Composable
private fun SummaryChip(label: String, value: String) {
  AssistChip(onClick = {}, label = { Text("$label: $value") })
}

@Composable
private fun DateTextField(
  value: String,
  onValidValue: (String) -> Unit,
  label: @Composable () -> Unit,
) {
  var draft by remember(value) { mutableStateOf(value) }
  OutlinedTextField(
    value = draft,
    onValueChange = { newValue ->
      draft = newValue
      if (newValue.isBlank() || (newValue.length == 10 && isValidIsoDate(newValue))) {
        onValidValue(newValue)
      }
    },
    modifier = Modifier.fillMaxWidth(),
    label = label,
  )
}

@Composable
private fun BrokerListCard(
  entry: PrivacyBrokerEntry,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val overdue = isOverdue(entry)
  ElevatedCard(
    onClick = onClick,
    colors =
      CardDefaults.elevatedCardColors(
        containerColor =
          if (selected) MaterialTheme.colorScheme.secondaryContainer
          else MaterialTheme.colorScheme.surface
      ),
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(entry.brokerName, style = MaterialTheme.typography.titleSmall)
      Text("${entry.requestType.label()} • ${entry.status.label()}", style = MaterialTheme.typography.bodySmall)
      if (entry.followUpDueOn.isNotBlank()) {
        Text(
          "Follow-up: ${entry.followUpDueOn}${if (overdue) " (overdue)" else ""}",
          style = MaterialTheme.typography.bodySmall,
          color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (entry.requestEmail.isNotBlank()) {
        Text(entry.requestEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

private fun PrivacyRequestType.label(): String {
  return when (this) {
    PrivacyRequestType.OPT_OUT -> "Opt-out"
    PrivacyRequestType.DELETION -> "Deletion"
    PrivacyRequestType.ACCESS -> "Access"
  }
}

private fun PrivacyRequestStatus.label(): String {
  return when (this) {
    PrivacyRequestStatus.NOT_STARTED -> "Not started"
    PrivacyRequestStatus.DRAFT_READY -> "Draft ready"
    PrivacyRequestStatus.SUBMITTED -> "Submitted"
    PrivacyRequestStatus.FOLLOW_UP_DUE -> "Follow-up due"
    PrivacyRequestStatus.COMPLETED -> "Completed"
  }
}

private fun openEmailDraft(
  context: android.content.Context,
  address: String,
  subject: String,
  body: String,
): String? {
  val intent =
    Intent(Intent.ACTION_SENDTO).apply {
      data = Uri.fromParts("mailto", address, null)
      putExtra(Intent.EXTRA_SUBJECT, subject)
      putExtra(Intent.EXTRA_TEXT, body)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  return runCatching {
      context.startActivity(intent)
      null
    }
    .getOrElse { "No mail app available to open the request draft." }
}

private fun openBrowser(context: android.content.Context, url: String): String? {
  if (url.isBlank()) return "Add a destination URL or search input first."
  val normalizedUrl =
    when {
      url.startsWith("https://") -> url
      url.startsWith("http://") -> return "Replace http:// with https:// before opening this link."
      else -> "https://$url"
    }
  val intent =
    Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl)).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  return runCatching {
      context.startActivity(intent)
      null
    }
    .getOrElse { "No browser is available to open the link." }
}

private fun filterEntries(
  entries: List<PrivacyBrokerEntry>,
  filter: PrivacyStatusFilter,
): List<PrivacyBrokerEntry> {
  return when (filter) {
    PrivacyStatusFilter.ALL -> entries
    PrivacyStatusFilter.ACTION_NEEDED ->
      entries.filter {
        it.status == PrivacyRequestStatus.NOT_STARTED ||
          it.status == PrivacyRequestStatus.DRAFT_READY ||
          it.status == PrivacyRequestStatus.FOLLOW_UP_DUE ||
          isOverdue(it)
      }
    PrivacyStatusFilter.SUBMITTED ->
      entries.filter {
        it.status == PrivacyRequestStatus.SUBMITTED || it.status == PrivacyRequestStatus.FOLLOW_UP_DUE
      }
    PrivacyStatusFilter.COMPLETED -> entries.filter { it.status == PrivacyRequestStatus.COMPLETED }
  }
}

private fun buildLegalHelpChecklist(profile: LegalHelpProfile): String {
  return buildString {
    appendLine("Low-cost legal help search")
    appendLine()
    appendLine("State: ${profile.state}")
    appendLine("County: ${profile.county}")
    appendLine("Case: ${profile.chargeLabel}")
    appendLine("Budget: ${profile.budgetNotes}")
    appendLine("Next court date: ${profile.nextCourtDate}")
    appendLine("Public defender already requested: ${if (profile.publicDefenderRequested) "Yes" else "No"}")
    appendLine()
    appendLine("Documents to gather:")
    appendLine(profile.gatherDocuments)
    appendLine()
    appendLine("Intake script:")
    appendLine(profile.contactScript)
    appendLine()
    appendLine("Notes:")
    append(profile.notes)
  }
}

private fun publicDefenderSearchUrl(profile: LegalHelpProfile): String {
  return googleSearchUrl("${profile.county} ${profile.state} public defender office criminal defense")
}

private fun legalAidSearchUrl(profile: LegalHelpProfile): String {
  return googleSearchUrl("${profile.county} ${profile.state} legal aid criminal defense low income")
}

private fun barReferralSearchUrl(profile: LegalHelpProfile): String {
  return googleSearchUrl("${profile.state} state bar lawyer referral criminal defense")
}

private fun lowCostAttorneySearchUrl(profile: LegalHelpProfile): String {
  return googleSearchUrl(
    "${profile.county} ${profile.state} ${profile.chargeLabel.ifBlank { "criminal defense" }} attorney free consultation payment plan"
  )
}

private fun googleSearchUrl(query: String): String {
  return "https://www.google.com/search?q=${Uri.encode(query)}"
}

private fun hasLegalHelpProfileContent(profile: LegalHelpProfile): Boolean {
  return profile.state.isNotBlank() ||
    profile.county.isNotBlank() ||
    profile.chargeLabel.isNotBlank() ||
    profile.budgetNotes.isNotBlank() ||
    profile.nextCourtDate.isNotBlank() ||
    profile.gatherDocuments.isNotBlank() ||
    profile.contactScript.isNotBlank() ||
    profile.notes.isNotBlank()
}
