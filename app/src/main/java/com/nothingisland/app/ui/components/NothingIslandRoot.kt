package com.nothingisland.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Root Composable for Nothing Phone (2a) Dynamic Island.
 * Implements Apple Fluid Motion via Jetpack Compose unified Transition and Spring physics,
 * 1:1 direct manipulation gesture tracking with rubber-banding,
 * native Nothing OS tactile haptics, and industrial aesthetics.
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

    // Unified coordinated transition — all dimensions animate together without desynchronization
    val transition = updateTransition(targetState = state, label = "IslandMotionTransition")

    // Notify state manager when animation is fully settled
    val isSettled = transition.currentState == transition.targetState
    LaunchedEffect(isSettled) {
        stateManager.setTransitionSettled(isSettled)
    }

    val animatedWidth by transition.animateDp(
        transitionSpec = {
            when {
                targetState is IslandState.Idle -> spring(
                    dampingRatio = 1.0f,
                    stiffness = Spring.StiffnessMedium
                )
                initialState is IslandState.Compact && targetState is IslandState.Compact -> spring(
                    dampingRatio = 1.0f, // Critically damped: zero overshoot/wobble between compact states
                    stiffness = Spring.StiffnessMediumLow // Silky-smooth glide
                )
                targetState is IslandState.Expanded -> spring(
                    dampingRatio = 0.82f, // Gentle natural pop for expanded card
                    stiffness = Spring.StiffnessMedium
                )
                initialState is IslandState.Expanded && targetState is IslandState.Compact -> spring(
                    dampingRatio = 0.95f, // Crisp return from card to pill
                    stiffness = Spring.StiffnessMedium
                )
                else -> spring(
                    dampingRatio = 1.0f,
                    stiffness = Spring.StiffnessMedium
                )
            }
        },
        label = "IslandWidth"
    ) { s ->
        when (s) {
            is IslandState.Idle -> config.cameraDiameterDp.dp
            is IslandState.Compact.Media -> config.compactMediaWidthDp.dp
            is IslandState.Compact.Notification -> config.compactNotifWidthDp.dp
            is IslandState.Compact.Battery -> config.compactBatteryWidthDp.dp
            is IslandState.Compact.Timer -> config.compactTimerWidthDp.dp
            is IslandState.Compact.Volume -> config.compactVolumeWidthDp.dp
            is IslandState.Compact -> config.compactPillWidthDp.dp
            is IslandState.Expanded -> config.expandedCardWidthDp.dp
        }
    }

    val animatedHeight by transition.animateDp(
        transitionSpec = {
            when {
                targetState is IslandState.Idle -> spring(
                    dampingRatio = 1.0f,
                    stiffness = Spring.StiffnessMedium
                )
                targetState is IslandState.Expanded -> spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMedium
                )
                initialState is IslandState.Expanded && targetState is IslandState.Compact -> spring(
                    dampingRatio = 0.95f,
                    stiffness = Spring.StiffnessMedium
                )
                else -> spring(
                    dampingRatio = 1.0f,
                    stiffness = Spring.StiffnessMedium
                )
            }
        },
        label = "IslandHeight"
    ) { s ->
        when (s) {
            is IslandState.Idle -> config.cameraDiameterDp.dp
            is IslandState.Compact -> config.compactPillHeightDp.dp
            is IslandState.Expanded -> config.expandedCardHeightDp.dp
        }
    }

    val animatedTopMargin by transition.animateDp(
        transitionSpec = {
            spring(dampingRatio = 1.0f, stiffness = Spring.StiffnessMedium)
        },
        label = "IslandTopMargin"
    ) { s ->
        when (s) {
            is IslandState.Idle -> config.cameraTopMarginDp.dp
            is IslandState.Compact, is IslandState.Expanded -> config.pillTopMarginDp.dp
        }
    }

    val animatedCornerRadius by transition.animateDp(
        transitionSpec = {
            when {
                targetState is IslandState.Idle -> spring(dampingRatio = 1.0f, stiffness = Spring.StiffnessMedium)
                targetState is IslandState.Expanded -> spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)
                initialState is IslandState.Expanded && targetState is IslandState.Compact -> spring(dampingRatio = 0.95f, stiffness = Spring.StiffnessMedium)
                else -> spring(dampingRatio = 1.0f, stiffness = Spring.StiffnessMedium)
            }
        },
        label = "IslandCornerRadius"
    ) { s ->
        when (s) {
            is IslandState.Idle -> (config.cameraDiameterDp / 2f).dp
            is IslandState.Compact -> (config.compactPillHeightDp / 2f).dp
            is IslandState.Expanded -> 28.dp
        }
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
                elevation = if (state is IslandState.Expanded) 14.dp else 0.dp,
                shape = RoundedCornerShape(animatedCornerRadius)
            )
            .clip(RoundedCornerShape(animatedCornerRadius))
            .background(NothingBlack)
            .border(
                width = 1.dp,
                color = NothingCardBorder,
                shape = RoundedCornerShape(animatedCornerRadius)
            )
            .pointerInput(state) {
                detectTapGestures(
                    onTap = { stateManager.onPillClicked() },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        stateManager.onPillLongClicked()
                    }
                )
            }
            .pointerInput(state) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            val newX = dragOffsetX.value + dragAmount.x
                            val newY = dragOffsetY.value + dragAmount.y
                            // Rubber-band damping
                            val dampedY = if (state is IslandState.Compact && newY < 0) {
                                newY * 0.25f
                            } else if (state is IslandState.Expanded && newY > 0) {
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
                        when (state) {
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
        AnimatedContent(
            targetState = state,
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
                val isIntraCompact = initialState is IslandState.Compact && targetState is IslandState.Compact
                val isToExpanded = targetState is IslandState.Expanded
                val isFromExpanded = initialState is IslandState.Expanded

                val transform = when {
                    isIntraCompact -> {
                        // Intra-compact transitions: pure crossfade without scale jitter or size clashes
                        fadeIn(animationSpec = androidx.compose.animation.core.tween(durationMillis = 140, delayMillis = 30)) togetherWith
                        fadeOut(animationSpec = androidx.compose.animation.core.tween(durationMillis = 90))
                    }
                    isToExpanded -> {
                        // Expanding: smooth fade with gentle scale-up
                        (fadeIn(animationSpec = androidx.compose.animation.core.tween(durationMillis = 180, delayMillis = 40)) +
                         scaleIn(initialScale = 0.94f, animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium))) togetherWith
                        (fadeOut(animationSpec = androidx.compose.animation.core.tween(durationMillis = 100)) +
                         scaleOut(targetScale = 0.94f, animationSpec = androidx.compose.animation.core.tween(durationMillis = 100)))
                    }
                    isFromExpanded -> {
                        // Collapsing back to compact: clean exit
                        fadeIn(animationSpec = androidx.compose.animation.core.tween(durationMillis = 140, delayMillis = 30)) togetherWith
                        (fadeOut(animationSpec = androidx.compose.animation.core.tween(durationMillis = 90)) +
                         scaleOut(targetScale = 0.95f, animationSpec = androidx.compose.animation.core.tween(durationMillis = 90)))
                    }
                    else -> {
                        // Idle <-> Compact
                        (fadeIn(animationSpec = androidx.compose.animation.core.tween(durationMillis = 160, delayMillis = 30)) +
                         scaleIn(initialScale = 0.92f, animationSpec = spring(dampingRatio = 1.0f, stiffness = Spring.StiffnessMedium))) togetherWith
                        (fadeOut(animationSpec = androidx.compose.animation.core.tween(durationMillis = 110)) +
                         scaleOut(targetScale = 0.92f, animationSpec = androidx.compose.animation.core.tween(durationMillis = 110)))
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
