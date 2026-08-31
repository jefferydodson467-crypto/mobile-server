package com.server.edge.gallery.customtasks.privacytracker

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class PrivacyOptOutViewModel @Inject constructor(
  private val repository: PrivacyOptOutRepository
) : ViewModel() {
  private val _uiState = MutableStateFlow(PrivacyOptOutUiState())
  val uiState = _uiState.asStateFlow()

  init {
    val hasSavedEntries = repository.hasSavedEntries()
    val savedEntries = repository.loadEntries()
    val initialEntries =
      when {
        !hasSavedEntries -> starterPrivacyBrokerEntries()
        savedEntries.isSuccess -> savedEntries.getOrDefault(emptyList())
        else -> emptyList()
      }
    if (!hasSavedEntries) {
      repository.saveEntries(initialEntries)
    }
    _uiState.value =
      PrivacyOptOutUiState(
        entries = initialEntries,
        selectedEntryId = initialEntries.firstOrNull()?.id,
        errorMessage =
          if (hasSavedEntries && savedEntries.isFailure) {
            "Saved tracker data could not be loaded. Import a backup or restore starter entries."
          } else {
            ""
          },
      )
  }

  fun selectEntry(entryId: String) {
    _uiState.update { it.copy(selectedEntryId = entryId, errorMessage = "", infoMessage = "") }
  }

  fun setStatusFilter(filter: PrivacyStatusFilter) {
    _uiState.update { it.copy(statusFilter = filter) }
  }

  fun setGuidanceAccepted(accepted: Boolean) {
    _uiState.update { it.copy(guidanceAccepted = accepted) }
  }

  fun updateImportExportText(value: String) {
    _uiState.update { it.copy(importExportText = value, errorMessage = "", infoMessage = "") }
  }

  fun addEntry() {
    val newEntry =
      PrivacyBrokerEntry(
        brokerName = "New broker",
        requestType = PrivacyRequestType.OPT_OUT,
        status = PrivacyRequestStatus.NOT_STARTED,
        requestSubject = defaultSubject("New broker", PrivacyRequestType.OPT_OUT),
        requestBody = defaultRequestBody("New broker", PrivacyRequestType.OPT_OUT),
        followUpDueOn = defaultFollowUpDate(),
        history = listOf(PrivacyHistoryEntry(title = "Entry created")),
      )
    saveEntries(entries = listOf(newEntry) + uiState.value.entries, selectedEntryId = newEntry.id)
    showInfoMessage("Added a new tracker entry.")
  }

  fun restoreStarterEntries() {
    val entries = starterPrivacyBrokerEntries()
    saveEntries(entries = entries, selectedEntryId = entries.firstOrNull()?.id)
    showInfoMessage("Restored starter broker entries.")
  }

  fun removeSelectedEntry() {
    val selectedId = uiState.value.selectedEntryId ?: return
    val remaining = uiState.value.entries.filterNot { it.id == selectedId }
    repository.saveEntries(remaining)
    _uiState.update {
      it.copy(
        entries = remaining,
        selectedEntryId = remaining.firstOrNull()?.id,
        infoMessage = "Removed tracker entry.",
        errorMessage = "",
      )
    }
  }

  fun updateSelectedEntry(transform: (PrivacyBrokerEntry) -> PrivacyBrokerEntry) {
    val selectedId = uiState.value.selectedEntryId ?: return
    val updatedEntries =
      uiState.value.entries.map { entry ->
        if (entry.id == selectedId) {
          transform(entry).copy(updatedAt = nowTimestamp())
        } else {
          entry
        }
      }
    saveEntries(entries = updatedEntries, selectedEntryId = selectedId, message = null)
  }

  fun markDraftReady() {
    updateSelectedEntry { entry ->
      entry.copy(
        status = PrivacyRequestStatus.DRAFT_READY,
        history = entry.history + PrivacyHistoryEntry(title = "Draft updated"),
      )
    }
    showInfoMessage("Saved draft changes.")
  }

  fun markSubmitted() {
    updateSelectedEntry { entry ->
      entry.copy(
        status = PrivacyRequestStatus.SUBMITTED,
        requestedOn = if (entry.requestedOn.isBlank()) todayIsoDate() else entry.requestedOn,
        followUpDueOn = if (entry.followUpDueOn.isBlank()) defaultFollowUpDate() else entry.followUpDueOn,
        history =
          entry.history + PrivacyHistoryEntry(title = "Marked as submitted", notes = "Manual submission recorded."),
      )
    }
    showInfoMessage("Recorded manual submission.")
  }

  fun markFollowUpDue() {
    updateSelectedEntry { entry ->
      entry.copy(
        status = PrivacyRequestStatus.FOLLOW_UP_DUE,
        history =
          entry.history +
            PrivacyHistoryEntry(title = "Follow-up needed", notes = "Reminder created for follow-up."),
      )
    }
    showInfoMessage("Marked follow-up as due.")
  }

  fun markCompleted() {
    updateSelectedEntry { entry ->
      entry.copy(
        status = PrivacyRequestStatus.COMPLETED,
        completedOn = if (entry.completedOn.isBlank()) todayIsoDate() else entry.completedOn,
        history = entry.history + PrivacyHistoryEntry(title = "Marked as completed"),
      )
    }
    showInfoMessage("Recorded completion.")
  }

  fun setDueInDays(days: Long) {
    updateSelectedEntry { entry -> entry.copy(followUpDueOn = defaultFollowUpDate(days)) }
    showInfoMessage("Updated reminder date.")
  }

  fun exportEntries() {
    val exported = repository.exportEntries(uiState.value.entries)
    _uiState.update {
      it.copy(importExportText = exported, infoMessage = "Exported tracker data to JSON.", errorMessage = "")
    }
  }

  fun importEntries() {
    val raw = uiState.value.importExportText.trim()
    if (raw.isEmpty()) {
      showErrorMessage("Paste exported JSON before importing.")
      return
    }
    val imported =
      runCatching { repository.importEntries(raw) }
        .getOrElse {
          showErrorMessage(it.message ?: "Failed to import tracker data.")
          return
        }
    if (imported.isEmpty()) {
      showErrorMessage("Import did not contain any tracker entries.")
      return
    }
    saveEntries(entries = imported, selectedEntryId = imported.first().id)
    showInfoMessage("Imported tracker data.")
  }

  private fun saveEntries(
    entries: List<PrivacyBrokerEntry>,
    selectedEntryId: String?,
    message: String? = null,
  ) {
    repository.saveEntries(entries)
    _uiState.update {
      it.copy(
        entries = entries,
        selectedEntryId = selectedEntryId ?: entries.firstOrNull()?.id,
        infoMessage = message ?: "",
        errorMessage = "",
      )
    }
  }

  fun onDraftCopied() {
    setInfoMessage("Copied request draft.")
  }

  fun onEmailOpened() {
    setInfoMessage("Opened email draft.")
  }

  fun onPortalOpened() {
    setInfoMessage("Opened privacy portal.")
  }

  fun onExternalActionFailed(message: String) {
    setErrorMessage(message)
  }

  private fun setInfoMessage(message: String) {
    _uiState.update { it.copy(infoMessage = message, errorMessage = "") }
  }

  private fun setErrorMessage(message: String) {
    _uiState.update { it.copy(errorMessage = message, infoMessage = "") }
  }
}
