package com.emikhalets.minicalculator.ui.calculator

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emikhalets.minicalculator.R
import com.emikhalets.minicalculator.core.theme.MiniCalculatorTheme
import com.emikhalets.minicalculator.data.CalculatorEngine

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalculatorScreen() {
    val haptics = LocalHapticFeedback.current

    var expr by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf("") }
    val history = remember { mutableStateListOf<String>() }

    fun evaluate() {
        runCatching { CalculatorEngine.eval(expr).toPlainString() }
            .onSuccess {
                result = it
                if (expr.isNotBlank()) history.add(0, "$expr = $it")
            }
            .onFailure { result = "Error" }
    }

    fun press(key: String) {
        when (key) {
            "AC" -> {
                expr = ""; result = ""
            }

            "⌫" -> if (expr.isNotEmpty()) expr = expr.dropLast(1)
            "=" -> evaluate()
            "×" -> expr += "*"
            "÷" -> expr += "/"
            else -> expr += key
        }
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val rows = listOf(
        listOf("AC", "(", ")", "⌫"),
        listOf("7", "8", "9", "÷"),
        listOf("4", "5", "6", "×"),
        listOf("1", "2", "3", "-"),
        listOf("0", ".", "%", "+"),
        listOf("=")
    )

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
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    text = expr.ifEmpty { "0" },
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.End
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    result,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            ButtonsBox(
                onClick = { press(it) }
            )
            Spacer(Modifier.height(16.dp))
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    ) { Text(action, fontSize = 18.sp) }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewIdleLight() {
    MiniCalculatorTheme {
        CalculatorScreen()
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewIdleDark() {
    MiniCalculatorTheme {
        CalculatorScreen()
    }
}