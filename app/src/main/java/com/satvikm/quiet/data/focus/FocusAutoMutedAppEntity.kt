package com.satvikm.quiet.data.focus

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Packages the [FocusModeOrchestrator] muted for the current focus window — separate from the user's own mute list so it can be cleared wholesale without touching what the user curated. */
@Entity(tableName = "focus_auto_muted_apps")
data class FocusAutoMutedAppEntity(
    @PrimaryKey val packageName: String,
)
