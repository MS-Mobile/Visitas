package com.msmobile.visitas.util

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MotionScheme
import androidx.navigation3.ui.NavDisplay

/**
 * Detail screens slide in horizontally: entering, the screen moves in from the end edge (right to
 * left in LTR) and pushes the screen below out towards the start; leaving (back, including the
 * predictive back gesture), both move the opposite way. Start/End keep the motion mirrored in RTL.
 *
 * The motion follows the theme's [MotionScheme] rather than a fixed duration. During the predictive
 * back gesture the transition tracks the finger instead; the scheme drives regular navigation and
 * the settle once the gesture is released or cancelled.
 */
object DetailScreenStyle {
    fun metadata(motionScheme: MotionScheme): Map<String, Any> =
        NavDisplay.transitionSpec {
            slideTowards(SlideDirection.Start, motionScheme)
        } + NavDisplay.popTransitionSpec {
            slideTowards(SlideDirection.End, motionScheme)
        } + NavDisplay.predictivePopTransitionSpec {
            slideTowards(SlideDirection.End, motionScheme)
        }

    private fun AnimatedContentTransitionScope<*>.slideTowards(
        direction: SlideDirection,
        motionScheme: MotionScheme
    ): ContentTransform {
        return slideIntoContainer(
            towards = direction,
            animationSpec = motionScheme.defaultSpatialSpec()
        ) togetherWith slideOutOfContainer(
            towards = direction,
            animationSpec = motionScheme.defaultSpatialSpec()
        )
    }
}
