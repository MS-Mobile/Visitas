package com.msmobile.visitas.extension

import android.Manifest
import android.graphics.Rect
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.msmobile.visitas.util.StringResource
import kotlin.collections.remove

@Composable
fun stringResource(resource: StringResource): String {
    return if (resource.arguments.isEmpty()) {
        androidx.compose.ui.res.stringResource(
            id = resource.textResId
        )
    } else {
        androidx.compose.ui.res.stringResource(
            id = resource.textResId,
            formatArgs = resource.arguments.toTypedArray()
        )
    }
}

@Composable
fun isKeyboardOpen(): State<Boolean> {
    val keyboardState = remember { mutableStateOf(false) }
    val view = LocalView.current
    DisposableEffect(view) {
        val onGlobalListener = ViewTreeObserver.OnGlobalLayoutListener {
            val visibleRect = Rect()
            view.getWindowVisibleDisplayFrame(visibleRect)
            val screenHeight = view.rootView.height
            val keyboardHeight = screenHeight - visibleRect.bottom
            keyboardState.value = keyboardHeight > screenHeight * 0.15
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(onGlobalListener)

        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalListener)
        }
    }

    return keyboardState
}

@Composable
fun OnBackPressed(block: () -> Unit) {
    val backDispatcherOwner = LocalOnBackPressedDispatcherOwner.current ?: return
    val currentBlock = rememberUpdatedState(block)
    val callback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                currentBlock.value()
            }
        }
    }
    DisposableEffect(backDispatcherOwner) {
        // Explicitly bind the callback to the lifecycle owner
        backDispatcherOwner.onBackPressedDispatcher.addCallback(backDispatcherOwner, callback)
        onDispose {
            callback.remove()
        }
    }
}

@Composable
fun RequestLocationPermission(onPermissionGranted: () -> Unit) {
    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { permission -> permission }) {
            onPermissionGranted()
        }
    }

    LaunchedEffect(key1 = Unit) {
        permissionLauncher.launch(locationPermissions)
    }
}

@Composable
fun RequestCalendarPermission(onPermissionGranted: () -> Unit) {
    val calendarPermissions = arrayOf(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR
    )
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { permission -> permission }) {
            onPermissionGranted()
        }
    }

    LaunchedEffect(key1 = Unit) {
        permissionLauncher.launch(calendarPermissions)
    }
}

@Composable
fun tonalButtonColors(): IconButtonColors {
    return IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
}

fun Modifier.textShimmer(apply: Boolean) = composed {
    if (!apply) return@composed this
    val currentStyle = LocalTextStyle.current
    val lineHeight = currentStyle.lineHeight.value
    val fontHeight = currentStyle.fontSize.value
    val textPadding = (lineHeight - fontHeight) / 2
    shimmer(apply, fontHeight.dp, textPadding.dp)
}

private fun Modifier.shimmer(apply: Boolean, height: Dp, padding: Dp, width: Dp = 60.dp) =
    composed {
        if (!apply) return@composed this
        val brush = shimmerBrush()
        this
            .padding(vertical = padding)
            .background(brush = brush, shape = MaterialTheme.shapes.medium)
            .widthIn(min = width)
            .height(height)
            .then(object : DrawModifier {
                override fun ContentDrawScope.draw() {
                    // Do not call drawContent() to avoid drawing the text
                }
            })
    }

@Composable
private fun shimmerBrush(): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
    )

    val transition = rememberInfiniteTransition()
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
    return brush
}

val Shapes.sharp: CornerBasedShape
    get() = RoundedCornerShape(0.dp)

val Shapes.textField: CornerBasedShape
    get() = large

fun CornerBasedShape.removeBottomCorner(): CornerBasedShape {
    return copy(
        bottomStart = CornerSize(0.dp),
        bottomEnd = CornerSize(0.dp)
    )
}

fun CornerBasedShape.removeTopCorner(): CornerBasedShape {
    return copy(
        topStart = CornerSize(0.dp),
        topEnd = CornerSize(0.dp)
    )
}

val EditableTextFieldColors: TextFieldColors
    @Composable
    get() = TextFieldDefaults.colors(
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
    )

val ReadOnlyTextFieldColors: TextFieldColors
    @Composable
    get() = EditableTextFieldColors.copy(
        disabledTextColor = MaterialTheme.colorScheme.onBackground,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        disabledIndicatorColor = Color.Transparent,
        disabledLabelColor = EditableTextFieldColors.unfocusedLabelColor,
        disabledTrailingIconColor = EditableTextFieldColors.unfocusedTrailingIconColor
    )