package com.nothingisland.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.nothingisland.app.core.IslandStateManager
import com.nothingisland.app.model.CutoutConfig
import com.nothingisland.app.model.IslandState
import com.nothingisland.app.ui.theme.NothingBlack
import com.nothingisland.app.ui.theme.NothingCardBorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Root Composable for Nothing Phone (2a) Dynamic Island.
 *
 * Implements two-phase squeeze-and-bloom physical motion:
 * - Phase 1 (Squeeze / Схлопування): Island softly collapses into the camera punch-hole circle
 * - Phase 2 (Bloom / Вкраплення): Island springs outward from behind the camera cutout with dynamicSpot
 *   Overshoot spring physics (dampingRatio = 0.74f, stiffness = Spring.StiffnessMediumLow).
 *
 * Preserves 1:1 direct manipulation gestures with rubber-banding and Nothing OS tactile haptics.
 */
@Composable
fun NothingIslandRoot(
    stateManager: IslandStateManager,
    config: CutoutConfig = CutoutConfig(),
    applyHorizontalCutoutOffset: Boolean = true,
    modifier: Modifier = Modifier
) {
    val state by stateManager.state.collectAsState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Displayed state currently held inside the island
    var morphState by remember { mutableStateOf(state) }
    var isSqueezing by remember { mutableStateOf(false) }

    // Context switch detection
    LaunchedEffect(state) {
        val prev = morphState
        val next = state
        if (prev == next) return@LaunchedEffect

        val isPrevCompact = prev is IslandState.Compact
        val isNextCompact = next is IslandState.Compact
        val isSame = isSameContext(prev, next)

        if (isPrevCompact && isNextCompact && !isSame) {
            // Two-phase transition between different compact contexts:
            // Phase 1: Soft squeeze into camera cutout circle
            isSqueezing = true
            delay(120L)
            // Phase 2: Pop out with new context
            morphState = next
            isSqueezing = false
        } else if (isPrevCompact && next is IslandState.Idle) {
            // Squeeze into camera cutout before disappearing
            isSqueezing = true
            delay(130L)
            morphState = next
            isSqueezing = false
        } else {
            // Direct transition (Idle -> Compact, Compact <-> Expanded, or intra-context update)
            isSqueezing = false
            morphState = next
        }
    }

    // Geometry targets based on morphState and squeeze phase
    val targetWidth = when {
        isSqueezing || morphState is IslandState.Idle -> config.cameraDiameterDp.dp
        morphState is IslandState.Compact.Media -> config.compactMediaWidthDp.dp
        morphState is IslandState.Compact.Notification -> config.compactNotifWidthDp.dp
        morphState is IslandState.Compact.Battery -> config.compactBatteryWidthDp.dp
        morphState is IslandState.Compact.Timer -> config.compactTimerWidthDp.dp
        morphState is IslandState.Compact.Volume -> config.compactVolumeWidthDp.dp
        morphState is IslandState.Compact -> config.compactPillWidthDp.dp
        morphState is IslandState.Expanded -> config.expandedCardWidthDp.dp
    }

    val targetHeight = when {
        isSqueezing || morphState is IslandState.Idle -> config.cameraDiameterDp.dp
        morphState is IslandState.Compact -> config.compactPillHeightDp.dp
        morphState is IslandState.Expanded -> config.expandedCardHeightDp.dp
    }

    val targetCornerRadius = when {
        isSqueezing || morphState is IslandState.Idle -> (config.cameraDiameterDp / 2f).dp
        morphState is IslandState.Compact -> (config.compactPillHeightDp / 2f).dp
        morphState is IslandState.Expanded -> 28.dp
    }

    val targetTopMargin = when {
        isSqueezing || morphState is IslandState.Idle -> config.cameraTopMarginDp.dp
        else -> config.pillTopMarginDp.dp
    }

    // DynamicSpot Overshoot spring specs
    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = if (isSqueezing) {
            spring(dampingRatio = 0.95f, stiffness = Spring.StiffnessHigh)
        } else if (morphState is IslandState.Expanded) {
            spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium)
        } else {
            // DynamicSpot pop_in OvershootInterpolator(1.5f) equivalent
            spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "IslandWidth"
    )

    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = if (isSqueezing) {
            spring(dampingRatio = 0.95f, stiffness = Spring.StiffnessHigh)
        } else {
            spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "IslandHeight"
    )

    val animatedCornerRadius by animateDpAsState(
        targetValue = targetCornerRadius,
        animationSpec = spring(dampingRatio = 0.95f, stiffness = Spring.StiffnessHigh),
        label = "IslandCornerRadius"
    )

    val animatedTopMargin by animateDpAsState(
        targetValue = targetTopMargin,
        animationSpec = spring(dampingRatio = 1.0f, stiffness = Spring.StiffnessMedium),
        label = "IslandTopMargin"
    )

    // Content pop-in / squeeze-out scale and alpha centered on camera punch hole
    val contentScale by animateFloatAsState(
        targetValue = if (isSqueezing || morphState is IslandState.Idle) 0.38f else 1.0f,
        animationSpec = if (isSqueezing) {
            tween(durationMillis = 110, easing = FastOutSlowInEasing)
        } else {
            spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "IslandContentScale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (isSqueezing || morphState is IslandState.Idle) 0.0f else 1.0f,
        animationSpec = if (isSqueezing) {
            tween(durationMillis = 90)
        } else {
            tween(durationMillis = 140, delayMillis = 20)
        },
        label = "IslandContentAlpha"
    )

    // Notify state manager when animation is fully settled
    val isSettled = !isSqueezing && morphState == state && abs(animatedWidth.value - targetWidth.value) < 0.5f
    LaunchedEffect(isSettled) {
        stateManager.setTransitionSettled(isSettled)
    }

    // Direct manipulation drag physics
    val dragOffsetX = remember { Animatable(0f) }
    val dragOffsetY = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .offset(
                x = if (applyHorizontalCutoutOffset) config.cameraCenterXOffsetDp.dp else 0.dp,
                y = animatedTopMargin
            )
            .graphicsLayer {
                translationX = dragOffsetX.value
                translationY = dragOffsetY.value
            }
            .width(animatedWidth)
            .height(animatedHeight)
            .shadow(
                elevation = if (morphState is IslandState.Expanded) 14.dp else 0.dp,
                shape = RoundedCornerShape(animatedCornerRadius)
            )
            .clip(RoundedCornerShape(animatedCornerRadius))
            .background(NothingBlack)
            .border(
                width = 1.dp,
                color = NothingCardBorder,
                shape = RoundedCornerShape(animatedCornerRadius)
            )
            .pointerInput(morphState) {
                detectTapGestures(
                    onTap = { stateManager.onPillClicked() },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        stateManager.onPillLongClicked()
                    }
                )
            }
            .pointerInput(morphState) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            val newX = dragOffsetX.value + dragAmount.x
                            val newY = dragOffsetY.value + dragAmount.y
                            // Rubber-band damping
                            val dampedY = if (morphState is IslandState.Compact && newY < 0) {
                                newY * 0.25f
                            } else if (morphState is IslandState.Expanded && newY > 0) {
                                newY * 0.25f
                            } else {
                                newY
                            }
                            dragOffsetX.snapTo(newX)
                            dragOffsetY.snapTo(dampedY)
                        }
                    },
                    onDragEnd = {
                        val finalY = dragOffsetY.value
                        val finalX = dragOffsetX.value
                        when (morphState) {
                            is IslandState.Compact -> {
                                if (finalY > 36f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    stateManager.expand()
                                } else if (abs(finalX) > 65f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    stateManager.onDismissSwiped()
                                }
                            }
                            is IslandState.Expanded -> {
                                if (finalY < -40f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    stateManager.collapse()
                                }
                            }
                            else -> Unit
                        }
                        // Smooth spring back to anchor
                        scope.launch {
                            launch {
                                dragOffsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)
                                )
                            }
                            launch {
                                dragOffsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)
                                )
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = contentScale
                    scaleY = contentScale
                    alpha = contentAlpha
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = morphState,
                modifier = Modifier.fillMaxSize(),
                contentKey = { targetState ->
                    when (targetState) {
                        is IslandState.Idle -> "idle"
                        is IslandState.Compact.Media -> "compact_media"
                        is IslandState.Compact.Notification -> "compact_notif"
                        is IslandState.Compact.Battery -> "compact_battery"
                        is IslandState.Compact.Volume -> "compact_volume"
                        is IslandState.Compact.Timer -> "compact_timer"
                        is IslandState.Expanded.Media -> "expanded_media"
                        is IslandState.Expanded.Notification -> "expanded_notif"
                        is IslandState.Expanded.Battery -> "expanded_battery"
                        is IslandState.Expanded.Timer -> "expanded_timer"
                    }
                },
                transitionSpec = {
                    val isToExpanded = targetState is IslandState.Expanded
                    val isFromExpanded = initialState is IslandState.Expanded

                    val transform = when {
                        isToExpanded -> {
                            (fadeIn(animationSpec = tween(durationMillis = 180, delayMillis = 40)) +
                             scaleIn(initialScale = 0.94f, animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium))) togetherWith
                            (fadeOut(animationSpec = tween(durationMillis = 100)) +
                             scaleOut(targetScale = 0.94f, animationSpec = tween(durationMillis = 100)))
                        }
                        isFromExpanded -> {
                            fadeIn(animationSpec = tween(durationMillis = 140, delayMillis = 30)) togetherWith
                            (fadeOut(animationSpec = tween(durationMillis = 90)) +
                             scaleOut(targetScale = 0.95f, animationSpec = tween(durationMillis = 90)))
                        }
                        else -> {
                            // Intra-compact transitions: scale and alpha are coordinated by outer container
                            fadeIn(animationSpec = tween(durationMillis = 120)) togetherWith
                            fadeOut(animationSpec = tween(durationMillis = 80))
                        }
                    }
                    transform.using(null)
                },
                label = "IslandContentMorph"
            ) { targetState ->
                when (targetState) {
                    is IslandState.Idle -> {
                        Box(modifier = Modifier.size(0.dp))
                    }
                    is IslandState.Compact -> {
                        CompactPillContent(
                            state = targetState,
                            config = config
                        )
                    }
                    is IslandState.Expanded -> {
                        ExpandedCardContent(
                            state = targetState,
                            config = config,
                            stateManager = stateManager
                        )
                    }
                }
            }
        }
    }
}

private fun isSameContext(a: IslandState, b: IslandState): Boolean {
    if (a::class != b::class) return false
    return when {
        a is IslandState.Compact.Media && b is IslandState.Compact.Media -> true
        a is IslandState.Compact.Battery && b is IslandState.Compact.Battery -> true
        a is IslandState.Compact.Volume && b is IslandState.Compact.Volume -> true
        a is IslandState.Compact.Timer && b is IslandState.Compact.Timer -> true
        a is IslandState.Compact.Notification && b is IslandState.Compact.Notification -> a.notification.id == b.notification.id
        a is IslandState.Expanded.Media && b is IslandState.Expanded.Media -> true
        a is IslandState.Expanded.Battery && b is IslandState.Expanded.Battery -> true
        a is IslandState.Expanded.Timer && b is IslandState.Expanded.Timer -> true
        a is IslandState.Expanded.Notification && b is IslandState.Expanded.Notification -> a.notification.id == b.notification.id
        a is IslandState.Idle && b is IslandState.Idle -> true
        else -> false
    }
}
