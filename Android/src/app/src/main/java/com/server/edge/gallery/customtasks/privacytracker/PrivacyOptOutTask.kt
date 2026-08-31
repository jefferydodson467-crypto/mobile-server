package com.server.edge.gallery.customtasks.privacytracker

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.runtime.Composable
import com.server.edge.gallery.customtasks.common.CustomTask
import com.server.edge.gallery.customtasks.common.CustomTaskData
import com.server.edge.gallery.data.CategoryInfo
import com.server.edge.gallery.data.Model
import com.server.edge.gallery.data.Task
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

class PrivacyOptOutTask @Inject constructor() : CustomTask {
  override val task: Task =
    Task(
      id = "privacy_opt_out_tracker",
      label = "Privacy Requests",
      category = CategoryInfo(id = "privacy", label = "Privacy"),
      icon = Icons.Outlined.VerifiedUser,
      description =
        "Track lawful privacy requests on-device and organize low-cost legal-help outreach. Draft request text, record manual submissions, prepare attorney intake details, and export your audit trail without sending data automatically.",
      shortDescription = "Privacy and legal help",
      sourceCodeUrl =
        "https://github.com/jefferydodson467-crypto/mobile-server/tree/main/Android/src/app/src/main/java/com/server/edge/gallery/customtasks/privacytracker",
      models =
        mutableListOf(
          Model(
            name = "Privacy Tracker Workspace",
            displayName = "Privacy Tracker Workspace",
            info =
              "Local-only workspace for privacy requests and legal-help preparation. No model download is required and all outreach stays manual.",
            localFileRelativeDirPathOverride = "privacy_tracker/",
          )
        ),
      experimental = true,
      useThemeColor = true,
    )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: (String) -> Unit,
  ) {
    onDone("")
  }

  override fun cleanUpModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: () -> Unit,
  ) {
    onDone()
  }

  @Composable
  override fun MainScreen(data: Any) {
    val customTaskData = data as CustomTaskData
    PrivacyOptOutScreen(bottomPadding = customTaskData.bottomPadding)
  }
}
