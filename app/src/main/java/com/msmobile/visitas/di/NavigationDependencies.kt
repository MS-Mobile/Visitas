package com.msmobile.visitas.di

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.msmobile.visitas.util.scaffold.AppScaffoldState
import com.msmobile.visitas.OnIntentStateHandled
import com.msmobile.visitas.backup.BackupViewModel
import com.msmobile.visitas.conversation.ConversationDetailViewModel
import com.msmobile.visitas.conversation.ConversationListViewModel
import com.msmobile.visitas.settings.SettingsDetailViewModel
import com.msmobile.visitas.summary.SummaryViewModel
import com.msmobile.visitas.util.IntentState
import com.msmobile.visitas.visit.VisitDetailViewModel
import com.msmobile.visitas.visit.VisitListViewModel
import com.ramcosta.composedestinations.generated.destinations.ConversationDetailScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ConversationListScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.VisitDetailScreenDestination
import com.ramcosta.composedestinations.generated.destinations.VisitListScreenDestination
import com.ramcosta.composedestinations.navigation.DependenciesContainerBuilder
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.navigation.destination

fun navigationDependencies(
    intentState: IntentState,
    intentStateHandled: OnIntentStateHandled,
    appScaffoldState: AppScaffoldState
): @Composable (DependenciesContainerBuilder<*>.() -> Unit) =
    {
        destination(VisitListScreenDestination) {
            dependency(hiltViewModel<SummaryViewModel>())
            dependency(hiltViewModel<VisitListViewModel>())
            dependency(hiltViewModel<BackupViewModel>())
            dependency(intentState)
            dependency(intentStateHandled)
            dependency(appScaffoldState)
        }
        destination(VisitDetailScreenDestination) {
            dependency(hiltViewModel<VisitDetailViewModel>())
            dependency(appScaffoldState)
        }
        destination(ConversationListScreenDestination) {
            dependency(hiltViewModel<ConversationListViewModel>())
            dependency(appScaffoldState)
        }
        destination(ConversationDetailScreenDestination) {
            dependency(hiltViewModel<ConversationDetailViewModel>())
            dependency(appScaffoldState)
        }
        destination(SettingsScreenDestination) {
            dependency(hiltViewModel<SettingsDetailViewModel>())
            dependency(appScaffoldState)
        }
    }