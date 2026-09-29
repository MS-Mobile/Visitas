package com.msmobile.visitas.util

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.navigation3.ui.NavDisplay

/**
 * List (tab) screens cross-fade in and out.
 */
object ListScreenStyle {
    private const val ANIMATION_DURATION = 500

    val metadata: Map<String, Any> =
        NavDisplay.transitionSpec { crossFade() } +
            NavDisplay.popTransitionSpec { crossFade() } +
            NavDisplay.predictivePopTransitionSpec { crossFade() }

    private fun crossFade(): ContentTransform {
        return fadeIn(animationSpec = tween(ANIMATION_DURATION)) togetherWith
            fadeOut(animationSpec = tween(ANIMATION_DURATION))
    }
}
