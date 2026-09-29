package com.msmobile.visitas.util

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.navigation3.ui.NavDisplay

/**
 * Detail screens slide in horizontally: entering, the screen moves in from the end edge (right to
 * left in LTR) and pushes the screen below out towards the start; leaving (back, including the
 * predictive back gesture), both move the opposite way. Start/End keep the motion mirrored in RTL.
 */
object DetailScreenStyle {
    private const val ANIMATION_DURATION = 250

    val metadata: Map<String, Any> =
        NavDisplay.transitionSpec {
            slideTowards(SlideDirection.Start)
        } + NavDisplay.popTransitionSpec {
            slideTowards(SlideDirection.End)
        } + NavDisplay.predictivePopTransitionSpec {
            slideTowards(SlideDirection.End)
        }

    private fun AnimatedContentTransitionScope<*>.slideTowards(
        direction: SlideDirection
    ): ContentTransform {
        return slideIntoContainer(
            towards = direction,
            animationSpec = tween(ANIMATION_DURATION)
        ) togetherWith slideOutOfContainer(
            towards = direction,
            animationSpec = tween(ANIMATION_DURATION)
        )
    }
}
