package com.msmobile.visitas.ui.views

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SCROLLBAR_FADE_DELAY_MS = 1500L
private const val SCROLLBAR_FADE_IN_DURATION_MS = 150
private const val SCROLLBAR_FADE_OUT_DURATION_MS = 500

@Composable
fun LazyColumnWithScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val totalItemsCount by remember { derivedStateOf { listState.layoutInfo.totalItemsCount } }
    val visibleItemsInfo by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo } }

    val showScrollbar by remember {
        derivedStateOf {
            totalItemsCount > 0 && visibleItemsInfo.isNotEmpty() &&
                visibleItemsInfo.size < totalItemsCount
        }
    }

    val isScrolling by remember { derivedStateOf { listState.isScrollInProgress } }
    var isDragging by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    // Show scrollbar when scrolling or dragging, hide after delay
    LaunchedEffect(isScrolling, isDragging) {
        if (isScrolling || isDragging) {
            isVisible = true
        } else {
            delay(SCROLLBAR_FADE_DELAY_MS)
            isVisible = false
        }
    }

    val scrollbarAlpha by animateFloatAsState(
        targetValue = if (showScrollbar && isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isVisible) SCROLLBAR_FADE_IN_DURATION_MS else SCROLLBAR_FADE_OUT_DURATION_MS
        ),
        label = "scrollbarAlpha"
    )

    Box(modifier = modifier.fillMaxSize()) {
        content()

        if (showScrollbar) {
            val scrollbarHeightDp = 48.dp
            val scrollbarHeightPx = with(density) { scrollbarHeightDp.toPx() }
            val containerHeight = remember { mutableIntStateOf(0) }

            val scrollProgress by remember {
                derivedStateOf {
                    if (totalItemsCount <= 1) 0f
                    else {
                        val firstVisibleIndex = listState.firstVisibleItemIndex
                        val firstVisibleOffset = listState.firstVisibleItemScrollOffset
                        val avgItemHeight = visibleItemsInfo.firstOrNull()?.size ?: 1
                        val scrollOffset = firstVisibleIndex + (firstVisibleOffset.toFloat() / avgItemHeight)
                        (scrollOffset / (totalItemsCount - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(20.dp)
                    .align(Alignment.CenterEnd)
                    .alpha(scrollbarAlpha)
                    .onSizeChanged { containerHeight.intValue = it.height }
                    .padding(vertical = 4.dp, horizontal = 4.dp)
                    .draggable(
                        orientation = Orientation.Vertical,
                        onDragStarted = { isDragging = true },
                        onDragStopped = { isDragging = false },
                        state = rememberDraggableState { delta ->
                            val availableHeight = containerHeight.intValue - scrollbarHeightPx
                            if (availableHeight > 0 && totalItemsCount > 1) {
                                val deltaProgress = delta / availableHeight
                                val targetIndex = ((scrollProgress + deltaProgress) * (totalItemsCount - 1))
                                    .toInt()
                                    .coerceIn(0, totalItemsCount - 1)
                                coroutineScope.launch {
                                    listState.scrollToItem(targetIndex)
                                }
                            }
                        }
                    )
            ) {
                val availableHeight = containerHeight.intValue - scrollbarHeightPx
                val offsetY = if (availableHeight > 0) {
                    (scrollProgress * availableHeight).toInt()
                } else {
                    0
                }

                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(scrollbarHeightDp)
                        .align(Alignment.TopEnd)
                        .offset { IntOffset(0, offsetY) }
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
                )
            }
        }
    }
}
