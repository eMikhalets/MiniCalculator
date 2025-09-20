package com.emikhalets.minicalculator.ui.calculator

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emikhalets.minicalculator.R
import com.emikhalets.minicalculator.core.theme.MiniCalculatorTheme
import com.emikhalets.minicalculator.data.CalculatorEngine
import kotlin.math.max
import kotlin.math.min

@Composable
fun CalculatorScreen() {
    val haptics = LocalHapticFeedback.current

    var expression by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var result by rememberSaveable { mutableStateOf("") }
    val history = remember { mutableStateListOf<String>() }

    fun evaluate() {
        val expr = expression.text
        runCatching { CalculatorEngine.eval(expr).toPlainString() }
            .onSuccess {
                result = it
                if (expr.isNotBlank()) history.add(0, "$expr = $it")
            }
            .onFailure { result = "Error" }
    }

    fun applyInsert(insert: String) {
        expression = replaceSelection(expression, insert)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun backspace() {
        expression = deleteAtCursor(expression)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun press(key: String) {
        when (key) {
            "AC" -> {
                expression = TextFieldValue("")
                result = ""
            }

            "⌫" -> backspace()
            "=" -> evaluate()
            "×" -> applyInsert("*")
            "÷" -> applyInsert("/")
            else -> applyInsert(key)
        }
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    ScreenContent(
        expression = expression,
        result = result,
        onExpressionChange = { expression = it },
        onButtonClick = { press(it) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenContent(
    expression: TextFieldValue,
    result: String,
    onExpressionChange: (TextFieldValue) -> Unit,
    onButtonClick: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.calculator_top_title)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ExpressionBox(
                expression = expression,
                result = result,
                onExpressionChange = onExpressionChange
            )
            ButtonsBox(onClick = onButtonClick)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ColumnScope.ExpressionBox(
    expression: TextFieldValue,
    result: String,
    onExpressionChange: (TextFieldValue) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .weight(1f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            BasicTextField(
                value = expression,
                onValueChange = onExpressionChange,
                textStyle = MaterialTheme.typography.headlineLarge.copy(
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = false,
                maxLines = Int.MAX_VALUE,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomEnd),
            )
        }

        if (result.isNotBlank()) {
            Text(
                text = result,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ButtonsBox(onClick: (String) -> Unit) {
    val buttons = listOf(
        listOf("AC", "%", "⌫", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("00", "0", ".", "="),
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        buttons.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { action ->
                    Button(
                        onClick = { onClick(action) },
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                    ) { Text(action, fontSize = 24.sp) }
                }
            }
        }
    }
}

private fun replaceSelection(value: TextFieldValue, insert: String): TextFieldValue {
    val text = value.text
    val sel = value.selection
    val start = min(sel.start, sel.end).coerceIn(0, text.length)
    val end = max(sel.start, sel.end).coerceIn(0, text.length)
    val newText = text.replaceRange(start, end, insert)
    val newCursor = start + insert.length
    return value.copy(text = newText, selection = TextRange(newCursor))
}

private fun deleteAtCursor(value: TextFieldValue): TextFieldValue {
    val text = value.text
    val sel = value.selection
    val hasSelection = sel.start != sel.end
    return if (hasSelection) {
        // удаляем выделение
        replaceSelection(value, "")
    } else {
        val cursor = sel.start.coerceIn(0, text.length)
        if (cursor == 0) value
        else {
            val newText = text.removeRange(cursor - 1, cursor)
            value.copy(text = newText, selection = TextRange(cursor - 1))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LightPreviewIdle() {
    MiniCalculatorTheme {
        ScreenContent(
            expression = TextFieldValue(""),
            result = "",
            onExpressionChange = {},
            onButtonClick = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DarkPreviewIdle() {
    MiniCalculatorTheme {
        ScreenContent(
            expression = TextFieldValue(""),
            result = "",
            onExpressionChange = {},
            onButtonClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LightPreviewExpression() {
    MiniCalculatorTheme {
        ScreenContent(
            expression = TextFieldValue("215+489/788"),
            result = "",
            onExpressionChange = {},
            onButtonClick = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DarkPreviewExpression() {
    MiniCalculatorTheme {
        ScreenContent(
            expression = TextFieldValue("215+489/788"),
            result = "",
            onExpressionChange = {},
            onButtonClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LightPreviewResult() {
    MiniCalculatorTheme {
        ScreenContent(
            expression = TextFieldValue("215+489/788"),
            result = "12345",
            onExpressionChange = {},
            onButtonClick = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DarkPreviewResult() {
    MiniCalculatorTheme {
        ScreenContent(
            expression = TextFieldValue("215+489/788"),
            result = "12345",
            onExpressionChange = {},
            onButtonClick = {},
        )
    }
}