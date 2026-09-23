package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CheckerboardDark
import com.example.ui.theme.CheckerboardLight

@Composable
fun CheckerboardBackground(
    modifier: Modifier = Modifier,
    squareSizePx: Float = 32f,
    lightColor: Color = CheckerboardLight,
    darkColor: Color = CheckerboardDark
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val numCols = (size.width / squareSizePx).toInt() + 1
        val numRows = (size.height / squareSizePx).toInt() + 1

        for (row in 0 until numRows) {
            for (col in 0 until numCols) {
                val color = if ((row + col) % 2 == 0) lightColor else darkColor
                drawRect(
                    color = color,
                    topLeft = Offset(col * squareSizePx, row * squareSizePx),
                    size = Size(squareSizePx, squareSizePx)
                )
            }
        }
    }
}
