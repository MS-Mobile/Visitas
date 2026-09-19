package com.msmobile.visitas.ui.views

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.msmobile.visitas.R
import com.msmobile.visitas.ui.theme.PreviewPhone
import com.msmobile.visitas.ui.theme.VisitasTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * The gentle reminder that the app is reading the user's position right now.
 *
 * It says what is happening, holds the sentence long enough to be read, then collapses to the
 * breathing icon and stays there for as long as tracking lasts: noticed once, present afterwards.
 * Nothing about where it sits belongs here — the caller anchors it (above the floating bar on the
 * visit list, at the top of the visits map), so neither surface has to know about the other.
 *
 * The icon never leaves. The collapsed state is the one the user looks at the longest, and a
 * coloured dot on its own would carry the whole meaning in colour — the shape keeps it legible in
 * greyscale and with colour vision deficiency.
 */
@Composable
fun LocationActiveIndicator(
    isTracking: Boolean,
    modifier: Modifier = Modifier
) {
    val startsCollapsed = LocalDensity.current.fontScale > COLLAPSE_FONT_SCALE
    var isShown by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    var shownAt by remember { mutableStateOf<TimeMark?>(null) }

    LaunchedEffect(isTracking, startsCollapsed) {
        if (isTracking) {
            // Swallows the short sessions: opening and closing the map straight away flashes nothing.
            delay(ENTRY_DELAY)
            shownAt = TimeSource.Monotonic.markNow()
            isExpanded = !startsCollapsed
            isShown = true
            if (isExpanded) {
                delay(EXPANDED_DWELL)
                isExpanded = false
            }
        } else {
            val visibleFor = shownAt?.elapsedNow()
            if (visibleFor != null && visibleFor < MINIMUM_VISIBILITY) {
                delay(MINIMUM_VISIBILITY - visibleFor)
            }
            isShown = false
            isExpanded = false
            shownAt = null
        }
    }

    AnimatedVisibility(
        visible = isShown,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(ENTER_DURATION_MILLIS, easing = FastOutSlowInEasing)) +
            scaleIn(
                animationSpec = tween(ENTER_DURATION_MILLIS, easing = FastOutSlowInEasing),
                initialScale = ENTER_SCALE
            ),
        // Leaves faster than it arrives: an exit should not ask for attention.
        exit = fadeOut(animationSpec = tween(EXIT_DURATION_MILLIS)) +
            scaleOut(animationSpec = tween(EXIT_DURATION_MILLIS), targetScale = EXIT_SCALE)
    ) {
        LocationActiveIndicatorPill(isExpanded = isExpanded)
    }
}

/**
 * The pill with no timing of its own, so previews and screenshot tests can render either state.
 */
@Composable
internal fun LocationActiveIndicatorPill(
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    val description = stringResource(R.string.location_active_indicator_content_description)
    Row(
        modifier = modifier
            // Collapsing is visual, not semantic: TalkBack reads the same sentence either way,
            // once, without interrupting whatever is being read.
            .semantics(mergeDescendants = true) {
                this.contentDescription = description
                liveRegion = LiveRegionMode.Polite
            }
            .heightIn(min = INDICATOR_HEIGHT)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .animateContentSize(animationSpec = tween(COLLAPSE_DURATION_MILLIS))
            .padding(horizontal = INDICATOR_HORIZONTAL_PADDING, vertical = INDICATOR_VERTICAL_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(INDICATOR_CONTENT_SPACING)
    ) {
        BreathingLocationIcon(isBreathing = !isExpanded)
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = tween(ENTER_DURATION_MILLIS)),
            // The label leaves before the width closes, otherwise it gets squeezed on the way out.
            exit = fadeOut(animationSpec = tween(LABEL_FADE_OUT_MILLIS))
        ) {
            Text(
                // The label is short in every locale so the pill never wraps to a second line,
                // which would turn a reminder into a block of text sitting over the content.
                text = stringResource(R.string.location_active_indicator_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Breathes only while collapsed, and only when the system animation scale allows it: infinite
 * motion is a known vestibular-discomfort trigger, and the indicator reads the same standing still.
 */
@Composable
private fun BreathingLocationIcon(isBreathing: Boolean) {
    val alpha = if (isBreathing && isMotionEnabled()) {
        val transition = rememberInfiniteTransition(label = "locationIndicatorBreath")
        val animatedAlpha by transition.animateFloat(
            initialValue = BREATH_MIN_ALPHA,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(BREATH_DURATION_MILLIS, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "locationIndicatorBreathAlpha"
        )
        animatedAlpha
    } else {
        1f
    }

    Icon(
        imageVector = Icons.Rounded.MyLocation,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .size(INDICATOR_ICON_SIZE)
            .alpha(alpha)
    )
}

/**
 * Whether the system is running animations at all. Previews have no real settings to read, and
 * a screenshot of a half-faded icon is not a stable baseline, so they render the still state.
 */
@Composable
private fun isMotionEnabled(): Boolean {
    if (LocalInspectionMode.current) return false
    val contentResolver = LocalContext.current.contentResolver
    return remember(contentResolver) {
        Settings.Global.getFloat(
            contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            DEFAULT_ANIMATOR_DURATION_SCALE
        ) != 0f
    }
}

private val ENTRY_DELAY = 400.milliseconds
private val EXPANDED_DWELL = 4000.milliseconds
private val MINIMUM_VISIBILITY = 1200.milliseconds
private const val ENTER_DURATION_MILLIS = 220
private const val EXIT_DURATION_MILLIS = 160
private const val COLLAPSE_DURATION_MILLIS = 180
private const val LABEL_FADE_OUT_MILLIS = 120
private const val BREATH_DURATION_MILLIS = 2000
private const val BREATH_MIN_ALPHA = .45f
private const val ENTER_SCALE = .92f
private const val EXIT_SCALE = .96f
private const val DEFAULT_ANIMATOR_DURATION_SCALE = 1f

/** Above this the expanded pill cannot fit its sentence without truncating, so it starts collapsed. */
private const val COLLAPSE_FONT_SCALE = 1.3f

private val INDICATOR_HEIGHT = 28.dp
private val INDICATOR_ICON_SIZE = 16.dp
private val INDICATOR_HORIZONTAL_PADDING = 10.dp
private val INDICATOR_VERTICAL_PADDING = 4.dp
private val INDICATOR_CONTENT_SPACING = 6.dp

@PreviewPhone
@Composable
internal fun LocationActiveIndicatorPreview(
    @PreviewParameter(LocationActiveIndicatorPreviewConfigProvider::class) config: LocationActiveIndicatorPreviewConfig
) {
    VisitasTheme(config.isDarkMode) {
        LocationActiveIndicatorPill(isExpanded = config.isExpanded)
    }
}
