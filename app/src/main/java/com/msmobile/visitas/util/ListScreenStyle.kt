package com.msmobile.visitas.util

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MotionScheme
import androidx.navigation3.ui.NavDisplay

/**
 * List (tab) screens cross-fade in and out, following the theme's [MotionScheme].
 */
object ListScreenStyle {
    fun metadata(motionScheme: MotionScheme): Map<String, Any> =
        NavDisplay.transitionSpec { crossFade(motionScheme) } +
            NavDisplay.popTransitionSpec { crossFade(motionScheme) } +
            NavDisplay.predictivePopTransitionSpec { crossFade(motionScheme) }

    private fun crossFade(motionScheme: MotionScheme): ContentTransform {
        return fadeIn(animationSpec = motionScheme.slowEffectsSpec()) togetherWith
            fadeOut(animationSpec = motionScheme.slowEffectsSpec())
    }
}
