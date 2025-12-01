package org.example

sealed class Stmt {
    interface Visitor<R> {
        fun visitStmt(stmt: Stmt): R
    }

    fun <R> accept(visitor: Visitor<R>) = visitor.visitStmt(this)

    data class Print(val expr: Expr): Stmt()
    data class Expression(val expr: Expr): Stmt()
    data class Var(val name: Token, val initializer: Expr?): Stmt()
    data class Block(val statements: List<Stmt?>): Stmt()
    data class If(val cond: Expr, val thenStmt: Stmt, val elseStmt: Stmt?): Stmt()
    data class While(val cond: Expr, val body: Stmt): Stmt()
    data class Function(val name: Token, val params: List<Token>, val body: List<Stmt?>): Stmt()
    data class Return(val keyword: Token, val value: Expr?): Stmt()
    data class Class(val name: Token, val superClass: Expr.Variable?,  val methods: List<Stmt.Function>): Stmt()
}