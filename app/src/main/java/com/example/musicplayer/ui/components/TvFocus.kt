package com.example.musicplayer.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** TV-friendly focus treatment for D-pad navigation. */
fun Modifier.tvFocusable(
    enabled: Boolean = true,
    scaleOnFocus: Float = 1.035f
): Modifier = then(
    if (!enabled) Modifier else Modifier
        .focusable()
        .tvFocusVisual(scaleOnFocus)
)

private fun Modifier.tvFocusVisual(scaleOnFocus: Float): Modifier {
    return composedTvFocus(scaleOnFocus)
}

private fun Modifier.composedTvFocus(scaleOnFocus: Float): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    this
        .onFocusChanged { focused = it.isFocused }
        .scale(if (focused) scaleOnFocus else 1f)
        .border(
            width = if (focused) 2.dp else 0.dp,
            color = if (focused) Color(0xFF00E5FF) else Color.Transparent,
            shape = RoundedCornerShape(10.dp)
        )
}
