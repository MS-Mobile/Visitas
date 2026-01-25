package com.msmobile.visitas.ui.views

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.msmobile.visitas.R
import com.ramcosta.composedestinations.generated.destinations.ConversationListScreenDestination
import com.ramcosta.composedestinations.generated.destinations.VisitListScreenDestination
import com.ramcosta.composedestinations.spec.DestinationSpec
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec

@Composable
fun BottomNavigation(
    currentDestination: DestinationSpec,
    onNavigateToTab: (DirectionDestinationSpec) -> Unit
) {
    var activeTab by remember { mutableStateOf(currentDestination) }
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        BottomNavigationTab.entries.forEach { tab ->
            NavigationBarItem(
                icon = { Icon(tab.icon, contentDescription = stringResource(id = tab.textId)) },
                label = { Text(stringResource(id = tab.textId)) },
                selected = activeTab == tab.destination,
                onClick = {
                    if (currentDestination != tab.destination) {
                        onNavigateToTab(tab.destination)
                        activeTab = tab.destination
                    }
                }
            )
        }
    }
}

private enum class BottomNavigationTab(
    val destination: DirectionDestinationSpec,
    val icon: ImageVector,
    @param:StringRes val textId: Int
) {
    Visits(VisitListScreenDestination, Icons.Rounded.Home, R.string.visits),
    Conversation(ConversationListScreenDestination, Icons.AutoMirrored.Default.MenuBook, R.string.conversations),
}