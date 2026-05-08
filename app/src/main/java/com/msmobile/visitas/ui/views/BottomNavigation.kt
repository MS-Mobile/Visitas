package com.msmobile.visitas.ui.views

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults.iconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.msmobile.visitas.R
import com.msmobile.visitas.ui.theme.PreviewFoldable
import com.msmobile.visitas.ui.theme.PreviewPhone
import com.msmobile.visitas.ui.theme.VisitasTheme
import com.ramcosta.composedestinations.generated.destinations.ConversationListScreenDestination
import com.ramcosta.composedestinations.generated.destinations.VisitListScreenDestination
import com.ramcosta.composedestinations.spec.DestinationSpec
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec

@Composable
fun BottomNavigationTabs(
    currentDestination: DestinationSpec,
    onNavigateToTab: (DirectionDestinationSpec) -> Unit
) {
    var activeTab by remember { mutableStateOf(currentDestination) }
    Row {
        BottomNavigationTab.entries.map { tab ->
            IconButton(
                colors = if (activeTab == tab.destination) {
                    iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                } else {
                    iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                onClick = {
                    if (currentDestination != tab.destination) {
                        onNavigateToTab(tab.destination)
                        activeTab = tab.destination
                    }
                },
                content = {
                    Icon(tab.icon, contentDescription = stringResource(id = tab.textId))
                }
            )
        }
    }
}

@Composable
@PreviewPhone
@PreviewFoldable
private fun BottomNavigationTabsPreview() {
    VisitasTheme {
        BottomNavigationTabs(
            currentDestination = VisitListScreenDestination,
            onNavigateToTab = {}
        )
    }
}

private enum class BottomNavigationTab(
    val destination: DirectionDestinationSpec,
    val icon: ImageVector,
    @param:StringRes val textId: Int
) {
    Visits(VisitListScreenDestination, Icons.Rounded.Home, R.string.visits),
    Conversation(
        ConversationListScreenDestination,
        Icons.AutoMirrored.Default.MenuBook,
        R.string.conversations
    ),
}
