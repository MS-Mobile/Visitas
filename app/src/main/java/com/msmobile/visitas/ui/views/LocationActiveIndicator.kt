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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
import com.msmobile.visitas.ui.theme.LocationLive
import com.msmobile.visitas.ui.theme.LocationLiveDark
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
 * breathing dot and stays there for as long as tracking lasts: noticed once, present afterwards.
 * Tapping the dot brings the sentence back for another dwell, which is what keeps the collapsed
 * state honest — the explanation is always one tap away rather than gone.
 *
 * Nothing about where it sits belongs here — the caller anchors it (above the floating bar on the
 * visit list, at the top of the visits map), so neither surface has to know about the other.
 *
 * The caller supplies the sentence TalkBack reads: the reason differs by surface, and only the
 * caller knows whether the nearby filter or the map turned tracking on.
 */
@Composable
fun LocationActiveIndicator(
    isTracking: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val startsCollapsed = LocalDensity.current.fontScale > COLLAPSE_FONT_SCALE
    // Previews and screenshot tests never run the effects below, so the indicator would render as
    // nothing at all and the list baselines would not show it. They open on the state the user
    // spends the first seconds looking at instead.
    val isInspecting = LocalInspectionMode.current
    var isShown by remember { mutableStateOf(isTracking && isInspecting) }
    var isExpanded by remember { mutableStateOf(isTracking && isInspecting) }
    var shownAt by remember { mutableStateOf<TimeMark?>(null) }
    // Bumped by the entry and by every tap, so both open the label on the same dwell.
    var expansions by remember { mutableIntStateOf(0) }

    LaunchedEffect(isTracking, startsCollapsed) {
        if (isTracking) {
            // Swallows the short sessions: opening and closing the map straight away flashes nothing.
            delay(ENTRY_DELAY)
            shownAt = TimeSource.Monotonic.markNow()
            isShown = true
            if (!startsCollapsed) expansions++
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

    LaunchedEffect(expansions) {
        if (expansions == 0) return@LaunchedEffect
        isExpanded = true
        delay(EXPANDED_DWELL)
        isExpanded = false
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
        LocationActiveIndicatorPill(
            isExpanded = isExpanded,
            contentDescription = contentDescription,
            onClick = { expansions++ }
        )
    }
}

/**
 * The pill with no timing of its own, so previews and screenshot tests can render either state.
 */
@Composable
internal fun LocationActiveIndicatorPill(
    isExpanded: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            // Collapsing is visual, not semantic: TalkBack reads the same sentence either way,
            // once, without interrupting whatever is being read.
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
                liveRegion = LiveRegionMode.Polite
            }
            // Collapsed, the pill is a 36.dp circle; this gives it the 48.dp target a tap needs
            // without growing what is drawn.
            .minimumInteractiveComponentSize()
            .heightIn(min = INDICATOR_HEIGHT)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(
                onClickLabel = stringResource(R.string.location_active_indicator_expand_action),
                onClick = onClick
            )
            .animateContentSize(animationSpec = tween(COLLAPSE_DURATION_MILLIS))
            .padding(horizontal = INDICATOR_HORIZONTAL_PADDING, vertical = INDICATOR_VERTICAL_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(INDICATOR_CONTENT_SPACING)
    ) {
        LiveDot(isBreathing = !isExpanded)
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = tween(ENTER_DURATION_MILLIS)),
            // The label leaves before the width closes, otherwise it gets squeezed on the way out.
            exit = fadeOut(animationSpec = tween(LABEL_FADE_OUT_MILLIS))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(INDICATOR_CONTENT_SPACING)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MyLocation,
                    contentDescription = null,
                    // Blue against the grey label, as the design has it: the icon is the mark that
                    // carries the meaning, the label is supporting text.
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(INDICATOR_ICON_SIZE)
                )
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
}

/**
 * Breathes only while collapsed, and only when the system animation scale allows it: infinite
 * motion is a known vestibular-discomfort trigger, and the dot reads the same standing still.
 */
@Composable
private fun LiveDot(isBreathing: Boolean) {
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

    Box(
        modifier = Modifier
            .size(INDICATOR_DOT_SIZE)
            .alpha(alpha)
            .clip(CircleShape)
            .background(liveDotColor())
    )
}

/**
 * Green against whichever scheme is actually in use. Reading the system dark-mode setting instead
 * would paint the dark green onto a light pill whenever the two disagree, previews included.
 */
@Composable
private fun liveDotColor(): Color {
    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE_LUMINANCE
    return if (isDarkSurface) LocationLiveDark else LocationLive
}

/**
 * Whether the system is running animations at all. Previews have no real settings to read, and
 * a screenshot of a half-faded dot is not a stable baseline, so they render the still state.
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
private const val DARK_SURFACE_LUMINANCE = .5f

/** Above this the expanded pill cannot fit its sentence without truncating, so it starts collapsed. */
private const val COLLAPSE_FONT_SCALE = 1.3f

private val INDICATOR_HEIGHT = 36.dp
private val INDICATOR_ICON_SIZE = 18.dp
private val INDICATOR_DOT_SIZE = 8.dp

/** Half the height less half the dot, so the collapsed pill closes into a circle. */
private val INDICATOR_HORIZONTAL_PADDING = 14.dp
private val INDICATOR_VERTICAL_PADDING = 6.dp
private val INDICATOR_CONTENT_SPACING = 8.dp

@PreviewPhone
@Composable
internal fun LocationActiveIndicatorPreview(
    @PreviewParameter(LocationActiveIndicatorPreviewConfigProvider::class) config: LocationActiveIndicatorPreviewConfig
) {
    VisitasTheme(config.isDarkMode) {
        LocationActiveIndicatorPill(
            isExpanded = config.isExpanded,
            contentDescription = stringResource(R.string.location_active_indicator_nearby_content_description)
        )
    }
}
