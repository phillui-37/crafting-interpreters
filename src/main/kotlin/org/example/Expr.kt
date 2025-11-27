package org.example

sealed class Expr {
    fun <R> accept(visitor: Visitor<R>) = visitor.visit(this)
    interface Visitor<R> {
        fun visit(expr: Expr): R;
    }

    data class Binary(val l: Expr, val op: Token, val r: Expr): Expr()
    data class Grouping(val expr: Expr): Expr()
    data class Literal(val v: Any?): Expr()
    data class Unary(val op: Token, val r: Expr): Expr()
}