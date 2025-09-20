package com.emikhalets.minicalculator.data

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.ArrayDeque
import kotlin.math.*

object CalculatorEngine {
    private val mc = MathContext.DECIMAL64
    private const val DIV_SCALE = 12

    private enum class TokType { NUM, OP, LPAR, RPAR, FUNC, CONST, PERCENT }

    private data class Tok(val type: TokType, val text: String)

    private fun precedence(op: String) = when (op) {
        "+", "-" -> 1
        "*", "/" -> 2
        "^" -> 3
        else -> -1
    }

    private fun isRightAssoc(op: String) = op == "^"

    private fun applyOp(a: BigDecimal, b: BigDecimal, op: String): BigDecimal = when (op) {
        "+" -> a.add(b, mc)
        "-" -> a.subtract(b, mc)
        "*" -> a.multiply(b, mc)
        "/" -> a.divide(b, DIV_SCALE, RoundingMode.HALF_UP)
        "^" -> a.toDouble().pow(b.toDouble()).toBigDecimal(mc)
        else -> error("Unknown op $op")
    }

    private fun applyFunc(x: BigDecimal, f: String): BigDecimal = when (f) {
        "sqrt" -> sqrt(x.toDouble()).toBigDecimal(mc)
        "sin" -> sin(x.toDouble()).toBigDecimal(mc)
        "cos" -> cos(x.toDouble()).toBigDecimal(mc)
        "tan" -> tan(x.toDouble()).toBigDecimal(mc)
        "ln" -> ln(x.toDouble()).toBigDecimal(mc)
        "log" -> log10(x.toDouble()).toBigDecimal(mc)
        else -> error("Unknown func $f")
    }

    private fun constValue(c: String) = when (c) {
        "pi" -> Math.PI.toBigDecimal(mc)
        "e" -> Math.E.toBigDecimal(mc)
        else -> error("Unknown const $c")
    }

    private fun tokenize(expr: String): List<Tok> {
        val s = expr.replace(" ", "")
        val list = mutableListOf<Tok>();
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c.isDigit() || c == '.' -> {
                    var j = i
                    while (j < s.length && (s[j].isDigit() || s[j] == '.')) j++
                    list += Tok(TokType.NUM, s.substring(i, j))
                    i = j; continue
                }
                c == '(' -> list += Tok(TokType.LPAR, "(")
                c == ')' -> list += Tok(TokType.RPAR, ")")
                c == '%' -> list += Tok(TokType.PERCENT, "%")
                "+-*/^".contains(c) -> list += Tok(TokType.OP, c.toString())
                c.isLetter() -> {
                    var j = i
                    while (j < s.length && s[j].isLetter()) j++
                    val w = s.substring(i, j)
                    val type = when (w) {
                        "sin","cos","tan","sqrt","ln","log" -> TokType.FUNC
                        "pi","e" -> TokType.CONST
                        else -> throw IllegalArgumentException("Bad token $w")
                    }
                    list += Tok(type, w)
                    i = j; continue
                }
                else -> throw IllegalArgumentException("Bad char $c")
            }
            i++
        }
        return list
    }

    fun eval(exprRaw: String): BigDecimal {
        if (exprRaw.isBlank()) return BigDecimal.ZERO
        val tokens = tokenize(exprRaw)
        val out = ArrayDeque<BigDecimal>()
        val ops = ArrayDeque<String>()

        fun reduce() {
            val op = ops.removeLast()
            if (op == "@func") {
                val fname = ops.removeLast()
                val x = out.removeLast()
                out.add(applyFunc(x, fname))
                return
            }
            val b = out.removeLast(); val a = out.removeLast()
            out.add(applyOp(a, b, op))
        }

        var i = 0
        var expectUnary = true
        while (i < tokens.size) {
            val t = tokens[i]
            when (t.type) {
                TokType.NUM -> { out.add(t.text.toBigDecimal()); expectUnary = false }
                TokType.CONST -> { out.add(constValue(t.text)); expectUnary = false }
                TokType.PERCENT -> { val v = out.removeLast(); out.add(v.movePointLeft(2)); expectUnary = false }
                TokType.LPAR -> { ops.add("("); expectUnary = true }
                TokType.RPAR -> {
                    while (ops.isNotEmpty() && ops.last() != "(") reduce()
                    if (ops.isEmpty()) error("Mismatched parentheses")
                    ops.removeLast()
                    if (ops.isNotEmpty() && ops.last() == "@func") reduce()
                    expectUnary = false
                }
                TokType.FUNC -> { ops.add(t.text); ops.add("@func"); expectUnary = true }
                TokType.OP -> {
                    var op = t.text
                    if (expectUnary && (op == "+" || op == "-")) {
                        out.add(BigDecimal.ZERO)
                    }
                    while (ops.isNotEmpty() && ops.last() != "(" &&
                        (precedence(ops.last()) > precedence(op) ||
                         (precedence(ops.last()) == precedence(op) && !isRightAssoc(op))) ) {
                        reduce()
                    }
                    ops.add(op)
                    expectUnary = true
                }
            }
            i++
        }
        while (ops.isNotEmpty()) {
            val top = ops.last()
            if (top == "(") error("Mismatched parentheses")
            reduce()
        }
        return out.single().stripTrailingZeros()
    }
}