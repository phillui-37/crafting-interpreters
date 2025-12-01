package org.example

sealed class Expr {
    fun <R> accept(visitor: Visitor<R>) = visitor.visitExpr(this)
    interface Visitor<R> {
        fun visitExpr(expr: Expr): R;
    }

    data class Binary(val l: Expr, val op: Token, val r: Expr): Expr()
    data class Grouping(val expr: Expr): Expr()
    data class Literal(val v: Any?): Expr()
    data class Unary(val op: Token, val r: Expr): Expr()
    data class Variable(val name: Token): Expr()
    data class Assign(val name: Token, val value: Expr): Expr()
    data class Logical(val l: Expr, val op: Token, val r: Expr): Expr()
    data class Call(val callee: Expr, val paren: Token, val arguments: List<Expr>): Expr()
    data class Get(val obj: Expr, val name: Token): Expr()
    data class Set(val obj: Expr, val name: Token, val value: Expr): Expr()
    data class This(val keyword: Token): Expr()
    data class Super(val keyword: Token, val method: Token): Expr()
}