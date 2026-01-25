package com.msmobile.visitas.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.spec.DestinationSpec
import com.ramcosta.composedestinations.utils.currentDestinationFlow
import com.ramcosta.composedestinations.utils.startDestination

@Composable
fun NavController.currentDestinationWithLifecycle(): State<DestinationSpec> {
    return currentDestinationFlow.collectAsStateWithLifecycle(initialValue = NavGraphs.root.startDestination)
}