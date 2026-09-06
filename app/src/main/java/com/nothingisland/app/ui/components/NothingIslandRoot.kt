package com.nothingisland.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.nothingisland.app.core.IslandStateManager
import com.nothingisland.app.model.CutoutConfig
import com.nothingisland.app.model.IslandState
import com.nothingisland.app.ui.theme.NothingBlack
import com.nothingisland.app.ui.theme.NothingCardBorder

/**
 * Root Composable for Nothing Phone (2a) Dynamic Island.
 * Implements Apple Fluid Motion via Jetpack Compose Spring physics,
 * pure OLED black camouflage for the centered punch-hole camera,
 * and Nothing OS industrial aesthetics.
 */
@Composable
fun NothingIslandRoot(
    stateManager: IslandStateManager,
    config: CutoutConfig = CutoutConfig(),
    modifier: Modifier = Modifier
) {
    val state by stateManager.state.collectAsState()

    // Dimensions derived from current state
    val targetWidth = when (state) {
        is IslandState.Idle -> 0.dp
        is IslandState.Compact -> config.compactPillWidthDp.dp
        is IslandState.Expanded -> config.expandedCardWidthDp.dp
    }

    val targetHeight = when (state) {
        is IslandState.Idle -> 0.dp
        is IslandState.Compact -> config.compactPillHeightDp.dp
        is IslandState.Expanded -> config.expandedCardHeightDp.dp
    }

    val targetCornerRadius = when (state) {
        is IslandState.Idle -> 20.dp
        is IslandState.Compact -> (config.compactPillHeightDp / 2).dp
        is IslandState.Expanded -> 28.dp
    }

    // Apple-style interruptible spring physics
    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "IslandWidth"
    )

    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "IslandHeight"
    )

    val animatedCornerRadius by animateDpAsState(
        targetValue = targetCornerRadius,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "IslandCornerRadius"
    )

    if (state is IslandState.Idle && animatedWidth <= 1.dp) {
        // Fully collapsed, render nothing to save GPU cycles
        return
    }

    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .offset(x = config.cameraCenterXOffsetDp.dp, y = config.cameraTopMarginDp.dp)
            .width(animatedWidth)
            .height(animatedHeight)
            .shadow(
                elevation = if (state is IslandState.Expanded) 12.dp else 4.dp,
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
                    onLongPress = { stateManager.onPillLongClicked() }
                )
            }
            .pointerInput(state) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetX += dragAmount.x
                        dragOffsetY += dragAmount.y
                    },
                    onDragEnd = {
                        if (dragOffsetY < -40f || Math.abs(dragOffsetX) > 80f) {
                            stateManager.onDismissSwiped()
                        } else if (state is IslandState.Expanded && dragOffsetY > 40f) {
                            stateManager.collapse()
                        }
                        dragOffsetX = 0f
                        dragOffsetY = 0f
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                fadeIn(animationSpec = spring(stiffness = Spring.StiffnessHigh)) togetherWith
                fadeOut(animationSpec = spring(stiffness = Spring.StiffnessHigh))
            },
            label = "IslandContentTransition"
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
