package com.msmobile.visitas.di

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.msmobile.visitas.OnIntentStateHandled
import com.msmobile.visitas.OnScaffoldConfigurationChanged
import com.msmobile.visitas.conversation.ConversationDetailViewModel
import com.msmobile.visitas.conversation.ConversationListViewModel
import com.msmobile.visitas.backup.BackupViewModel
import com.msmobile.visitas.summary.SummaryViewModel
import com.msmobile.visitas.util.IntentState
import com.msmobile.visitas.visit.VisitDetailViewModel
import com.msmobile.visitas.visit.VisitListViewModel
import com.ramcosta.composedestinations.generated.destinations.ConversationDetailScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ConversationListScreenDestination
import com.ramcosta.composedestinations.generated.destinations.VisitDetailScreenDestination
import com.ramcosta.composedestinations.generated.destinations.VisitListScreenDestination
import com.ramcosta.composedestinations.navigation.DependenciesContainerBuilder
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.navigation.destination

fun navigationDependencies(
    intentState: IntentState,
    scaffoldConfigurationChanged: OnScaffoldConfigurationChanged,
    intentStateHandled: OnIntentStateHandled,
    paddingValues: PaddingValues
): @Composable (DependenciesContainerBuilder<*>.() -> Unit) =
    {
        destination(VisitListScreenDestination) {
            dependency(hiltViewModel<SummaryViewModel>())
            dependency(hiltViewModel<VisitListViewModel>())
            dependency(hiltViewModel<BackupViewModel>())
            dependency(scaffoldConfigurationChanged)
            dependency(intentState)
            dependency(intentStateHandled)
            dependency(paddingValues)
        }
        destination(VisitDetailScreenDestination) {
            dependency(hiltViewModel<VisitDetailViewModel>())
            dependency(scaffoldConfigurationChanged)
        }
        destination(ConversationListScreenDestination) {
            dependency(hiltViewModel<ConversationListViewModel>())
            dependency(scaffoldConfigurationChanged)
            dependency(paddingValues)
        }
        destination(ConversationDetailScreenDestination) {
            dependency(hiltViewModel<ConversationDetailViewModel>())
            dependency(scaffoldConfigurationChanged)
        }
    }