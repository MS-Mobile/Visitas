package com.msmobile.visitas.navigation

import androidx.navigation3.runtime.NavKey
import com.msmobile.visitas.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Every screen the app can navigate to. Keys are `@Serializable` so `rememberNavBackStack` can save
 * the back stack across configuration changes and process death.
 */
@Serializable
sealed interface AppDestination : NavKey {
    @Serializable
    data object VisitList : AppDestination

    @Serializable
    data class VisitDetail(
        @Serializable(with = UUIDSerializer::class)
        val householderId: UUID? = null
    ) : AppDestination

    @Serializable
    data object ConversationList : AppDestination

    @Serializable
    data class ConversationDetail(
        @Serializable(with = UUIDSerializer::class)
        val firstConversationId: UUID? = null
    ) : AppDestination

    @Serializable
    data object Settings : AppDestination
}
