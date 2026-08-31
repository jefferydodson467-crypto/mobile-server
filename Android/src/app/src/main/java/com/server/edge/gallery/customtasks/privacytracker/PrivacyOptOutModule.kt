package com.server.edge.gallery.customtasks.privacytracker

import com.server.edge.gallery.customtasks.common.CustomTask
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal object PrivacyOptOutModule {
  @Provides
  @IntoSet
  fun provideTask(task: PrivacyOptOutTask): CustomTask {
    return task
  }
}
