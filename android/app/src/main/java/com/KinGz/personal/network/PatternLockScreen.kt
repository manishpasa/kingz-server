package com.KinGz.personal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun PatternLockScreen(
    isSetup: Boolean,
    onUnlocked: () -> Unit
) {
    var pattern by remember { mutableStateOf(listOf<Int>()) }
    var firstPattern by remember { mutableStateOf<List<Int>?>(null) }
    var message by remember {
        mutableStateOf(
            if (isSetup) "Draw a new pattern" else "Draw your pattern"
        )
    }
    var resetKey by remember { mutableIntStateOf(0) }

    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "KinGz",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(Modifier.height(12.dp))

        Text(message)

        Spacer(Modifier.height(28.dp))

        PatternGrid(
            resetKey = resetKey,
            onPatternChanged = { pattern = it },
            onPatternFinished = { drawn ->
                if (drawn.size < 4) {
                    message = "Connect at least 4 dots."
                } else if (isSetup) {
                    val savedFirst = firstPattern

                    if (savedFirst == null) {
                        firstPattern = drawn
                        message = "Draw the same pattern again to confirm."
                    } else if (savedFirst == drawn) {
                        if (LocalPatternStore.savePattern(context, drawn)) {
                            message = "Pattern saved."
                            onUnlocked()
                        } else {
                            message = "Could not save pattern. Try again."
                            firstPattern = null
                        }
                    } else {
                        firstPattern = null
                        message = "Patterns did not match. Start again."
                    }
                } else {
                    if (LocalPatternStore.verifyPattern(context, drawn)) {
                        onUnlocked()
                    } else {
                        message = "Incorrect pattern. Try again."
                    }
                }

                pattern = emptyList()
                resetKey++
            }
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PatternGrid(
    resetKey: Int,
    onPatternChanged: (List<Int>) -> Unit,
    onPatternFinished: (List<Int>) -> Unit
) {
    val selected = remember(resetKey) { mutableStateListOf<Int>() }
    var currentPosition by remember(resetKey) {
        mutableStateOf<Offset?>(null)
    }

    Canvas(
        modifier = Modifier
            .size(300.dp)
            .pointerInput(resetKey) {
                detectDragGestures(
                    onDragStart = { position ->
                        currentPosition = position
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentPosition = change.position

                        val spacingX = size.width / 3f
                        val spacingY = size.height / 3f
                        val radius = minOf(spacingX, spacingY) * 0.30f

                        val column = (change.position.x / spacingX).toInt()
                        val row = (change.position.y / spacingY).toInt()

                        if (column in 0..2 && row in 0..2) {
                            val center = Offset(
                                (column + 0.5f) * spacingX,
                                (row + 0.5f) * spacingY
                            )

                            if ((change.position - center).getDistance() <= radius * 1.8f) {
                                val index = row * 3 + column

                                if (index !in selected) {
                                    selected.add(index)
                                    onPatternChanged(selected.toList())
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        onPatternFinished(selected.toList())
                        currentPosition = null
                    },
                    onDragCancel = {
                        currentPosition = null
                    }
                )
            }
    ) {
        val spacingX = size.width / 3f
        val spacingY = size.height / 3f
        val centers = (0..8).map { index ->
            Offset(
                (index % 3 + 0.5f) * spacingX,
                (index / 3 + 0.5f) * spacingY
            )
        }
        val lineColor = androidx.compose.ui.graphics.Color(0xFF6750A4)
        val dotColor = androidx.compose.ui.graphics.Color(0xFF808080)
        val selectedColor = androidx.compose.ui.graphics.Color(0xFF6750A4)

        for (i in 0 until selected.size - 1) {
            drawLine(
                color = lineColor,
                start = centers[selected[i]],
                end = centers[selected[i + 1]],
                strokeWidth = 7f,
                cap = StrokeCap.Round
            )
        }

        if (selected.isNotEmpty() && currentPosition != null) {
            drawLine(
                color = lineColor,
                start = centers[selected.last()],
                end = currentPosition!!,
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
        }

        centers.forEachIndexed { index, center ->
            drawCircle(
                color = if (index in selected) selectedColor else dotColor,
                radius = if (index in selected) 14f else 10f,
                center = center
            )
        }
    }
}