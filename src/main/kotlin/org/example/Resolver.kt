package org.example

import java.util.Stack

class Resolver(private val interpreter: Interpreter) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {
    private val scopes = Stack<MutableMap<String, Boolean>>()
    private var currentFn = FunctionType.NONE

    override fun visitExpr(expr: Expr) {
        when (expr) {
            is Expr.Variable -> {
                if (scopes.isNotEmpty() && scopes.peek()[expr.name.lexeme] == false) {
                    Lox.error(expr.name, "Can't read local variable in its own initializer.")
                }

                resolveLocal(expr, expr.name)
            }
            is Expr.Assign -> {
                resolve(expr.value)
                resolveLocal(expr, expr.name)
            }
            is Expr.Binary -> {
                resolve(expr.l)
                resolve(expr.r)
            }
            is Expr.Call -> {
                resolve(expr.callee)
                expr.arguments.forEach(::resolve)
            }
            is Expr.Grouping -> {
                resolve(expr.expr)
            }
            is Expr.Literal -> {}
            is Expr.Logical -> {
                resolve(expr.l)
                resolve(expr.r)
            }
            is Expr.Unary -> {
                resolve(expr.r)
            }
        }
    }

    override fun visitStmt(stmt: Stmt) {
        when (stmt) {
            is Stmt.Block -> {
                beginScope()
                resolve(stmt.statements)
                endScope()
            }
            is Stmt.Var -> {
                declare(stmt.name)
                stmt.initializer?.also(::resolve)
                define(stmt.name)
            }
            is Stmt.Function -> {
                declare(stmt.name)
                define(stmt.name)

                resolveFunction(stmt, FunctionType.FUNCTION)
            }
            is Stmt.Expression -> {
                resolve(stmt.expr)
            }
            is Stmt.If -> {
                resolve(stmt.cond)
                resolve(stmt.thenStmt)
                resolve(stmt.elseStmt)
            }
            is Stmt.Print -> {
                resolve(stmt.expr)
            }
            is Stmt.Return -> {
                if (currentFn == FunctionType.NONE) {
                    Lox.error(stmt.keyword, "Can't return from top-level code.")
                }
                resolve(stmt.value)
            }
            is Stmt.While -> {
                resolve(stmt.cond)
                resolve(stmt.body)
            }
        }
    }

    private fun resolve(stmt: Stmt?) = stmt?.accept(this)
    private fun resolve(expr: Expr?) = expr?.accept(this)

    private fun beginScope() {
        scopes.push(mutableMapOf())
    }

    private fun endScope() {
        scopes.pop()
    }

    private fun declare(name: Token) {
        if (scopes.isEmpty()) return

        val scope = scopes.peek()
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name, "Already a variable with this name in this scope.")
        }
        scope[name.lexeme] = false
    }

    private fun define(name: Token) {
        if (scopes.isEmpty()) return
        scopes.peek()[name.lexeme] = true
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        for (i in scopes.size downTo 0) {
            if (scopes[i].containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size - 1 - i)
                return
            }
        }
    }

    private fun resolveFunction(fn: Stmt.Function, type: FunctionType) {
        var enclosingFn = currentFn
        currentFn = type

        beginScope()
        fn.params.forEach {
            declare(it)
            define(it)
        }
        resolve(fn.body)
        endScope()

        currentFn = enclosingFn
    }

    fun resolve(statements: List<Stmt?>) = statements.forEach { resolve(it) }
}