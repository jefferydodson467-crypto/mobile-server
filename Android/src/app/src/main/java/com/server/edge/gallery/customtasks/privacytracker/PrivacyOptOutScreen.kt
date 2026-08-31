package com.server.edge.gallery.customtasks.privacytracker

import android.content.ActivityNotFoundException
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
  val filteredEntries = viewModel.filteredEntries()
  val selectedEntry = uiState.entries.find { it.id == uiState.selectedEntryId }
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
          "This workflow is limited to lawful privacy management. It drafts requests, opens your mail or browser, and records your manual follow-up history. It does not auto-submit requests or alter third-party systems.",
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
                  requestSubject = if (entry.requestSubject.startsWith("Privacy request:")) defaultSubject(value, refreshedType) else entry.requestSubject,
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

          OutlinedTextField(
            value = selectedEntry.requestedOn,
            onValueChange = { value ->
              if (isValidIsoDate(value)) {
                viewModel.updateSelectedEntry { it.copy(requestedOn = value) }
              }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Requested on (YYYY-MM-DD)") },
          )
          OutlinedTextField(
            value = selectedEntry.followUpDueOn,
            onValueChange = { value ->
              if (isValidIsoDate(value)) {
                viewModel.updateSelectedEntry { it.copy(followUpDueOn = value) }
              }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Follow-up due (YYYY-MM-DD)") },
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            AssistChip(onClick = { viewModel.setDueInDays(14) }, label = { Text("+14d") })
            AssistChip(onClick = { viewModel.setDueInDays(30) }, label = { Text("+30d") })
            AssistChip(onClick = { viewModel.setDueInDays(45) }, label = { Text("+45d") })
          }
          OutlinedTextField(
            value = selectedEntry.completedOn,
            onValueChange = { value ->
              if (isValidIsoDate(value)) {
                viewModel.updateSelectedEntry { it.copy(completedOn = value) }
              }
            },
            modifier = Modifier.fillMaxWidth(),
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
                viewModel.clearMessages()
              }
            ) {
              Text("Copy draft")
            }
            Button(
              onClick = {
                if (uiState.guidanceAccepted) {
                  openEmailDraft(
                    context = context,
                    address = selectedEntry.requestEmail,
                    subject = selectedEntry.requestSubject,
                    body = selectedEntry.requestBody,
                  )
                }
              },
              enabled = uiState.guidanceAccepted && selectedEntry.requestEmail.isNotBlank(),
            ) {
              Text("Open email")
            }
            Button(
              onClick = {
                if (uiState.guidanceAccepted) {
                  openBrowser(context, selectedEntry.websiteUrl)
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
private fun BrokerListCard(
  entry: PrivacyBrokerEntry,
  selected: Boolean,
  onClick: () -> Unit,
) {
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
          "Follow-up: ${entry.followUpDueOn}${if (isOverdue(entry)) " (overdue)" else ""}",
          style = MaterialTheme.typography.bodySmall,
          color = if (isOverdue(entry)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
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
) {
  val intent =
    Intent(Intent.ACTION_SENDTO).apply {
      data = Uri.parse("mailto:${Uri.encode(address)}")
      putExtra(Intent.EXTRA_SUBJECT, subject)
      putExtra(Intent.EXTRA_TEXT, body)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  runCatching { context.startActivity(intent) }.recoverCatching {
    throw ActivityNotFoundException("No mail app available.")
  }
}

private fun openBrowser(context: android.content.Context, url: String) {
  if (url.isBlank()) return
  val normalizedUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
  val intent =
    Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl)).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  runCatching { context.startActivity(intent) }
}
