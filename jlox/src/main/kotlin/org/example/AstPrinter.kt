package org.example

class AstPrinter : Expr.Visitor<String> {
    override fun visitExpr(expr: Expr) = when (expr) {
        is Expr.Binary -> parenthesize(expr.op.lexeme, expr.l, expr.r)
        is Expr.Grouping -> parenthesize("group", expr.expr)
        is Expr.Literal -> expr.v?.toString() ?: "nil"
        is Expr.Unary -> parenthesize(expr.op.lexeme, expr.r)
        else -> "Unsupported"
    }

    private fun parenthesize(name: String, vararg exprs: Expr): String {
        val builder = StringBuilder()
        builder.append("(").append(name)
        exprs.forEach {
            builder.append(" ${it.accept(this)}")
        }
        builder.append(")")
        return builder.toString()
    }

    fun print(expr: Expr?) = expr?.accept(this)
}

fun main(vararg args: String) {
    val expr = Expr.Binary(
        Expr.Unary(
            Token(TokenType.MINUS, "-", null, 1),
            Expr.Literal(123)
        ),
        Token(TokenType.STAR, "*", null, 1),
        Expr.Grouping(Expr.Literal(45.67))
    )

    println(AstPrinter().print(expr))
}