package com.msmobile.visitas.util

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.spec.DestinationStyle

object ListScreenStyle : DestinationStyle.Animated() {
    private const val ANIMATION_DURATION = 500
    override val enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)?
        get() {
            return { fadeIn(animationSpec = tween(ANIMATION_DURATION)) }
        }

    override val exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)?
        get() {
            return { fadeOut(animationSpec = tween(ANIMATION_DURATION)) }
        }
}
