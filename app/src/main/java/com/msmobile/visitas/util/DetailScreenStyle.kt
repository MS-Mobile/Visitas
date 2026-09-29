package com.msmobile.visitas.util

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.navigation3.ui.NavDisplay

/**
 * Detail screens slide up over the screen below them and slide back down when popped.
 */
object DetailScreenStyle {
    private const val ANIMATION_DURATION = 500

    val metadata: Map<String, Any> =
        NavDisplay.transitionSpec {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Up,
                animationSpec = tween(ANIMATION_DURATION)
            ) togetherWith ExitTransition.KeepUntilTransitionsFinished
        } + NavDisplay.popTransitionSpec {
            slideDown()
        } + NavDisplay.predictivePopTransitionSpec {
            slideDown()
        }

    private fun AnimatedContentTransitionScope<*>.slideDown(): ContentTransform {
        return (
            EnterTransition.None togetherWith slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Down,
                animationSpec = tween(ANIMATION_DURATION)
            )
        ).apply { targetContentZIndex = -1f }
    }
}
