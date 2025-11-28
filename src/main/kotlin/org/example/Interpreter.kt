package org.example

import org.example.Lox.runtimeError

val STDLIB = mapOf<String, LoxCallable>(
    "clock" to object : LoxCallable {
        override fun arity() = 0

        override operator fun invoke(
            interpreter: Interpreter,
            arguments: List<Any?>
        ) = System.currentTimeMillis().toDouble() / 1000

        override fun toString() = "<native fn>"
    }
)

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {
    // MARK: - var
    val globals = Environment()
    private var environment = globals
    private val locals = mutableMapOf<Expr, Int>()

    init {
        STDLIB.forEach { globals.define(it.key, it.value) }
    }

    // MARK: - override fun
    override fun visitExpr(expr: Expr) = when (expr) {
        is Expr.Literal -> expr.v
        is Expr.Grouping -> evaluate(expr.expr)
        is Expr.Unary -> {
            val r = evaluate(expr.r)
            when (expr.op.type) {
                TokenType.MINUS -> checkNumberOperand(expr.op, r) {
                    -(r as Double)
                }

                TokenType.BANG -> !isTruthy(r)
                else -> null
            }
        }

        is Expr.Binary -> {
            val l = evaluate(expr.l)
            val r = evaluate(expr.r)

            when (expr.op.type) {
                TokenType.MINUS -> checkNumberOperands(expr.op, l, r) { l as Double - r as Double }
                TokenType.SLASH -> checkNumberOperands(expr.op, l, r) { l as Double - r as Double }
                TokenType.STAR -> checkNumberOperands(expr.op, l, r) { l as Double - r as Double }
                TokenType.PLUS -> {
                    when (l) {
                        is Double if r is Double -> l + r
                        is String if r is String -> l + r
                        else -> {
                            throw RuntimeError(
                                expr.op,
                                "Operands must be two numbers or two strings."
                            );
                        }

                    }
                }

                TokenType.GREATER -> checkNumberOperands(expr.op, l, r) { l as Double > r as Double }
                TokenType.GREATER_EQUAL -> checkNumberOperands(expr.op, l, r) { l as Double >= r as Double }
                TokenType.LESS -> checkNumberOperands(expr.op, l, r) { (l as Double) < r as Double }
                TokenType.LESS_EQUAL -> checkNumberOperands(expr.op, l, r) { l as Double <= r as Double }

                TokenType.BANG_EQUAL -> !isEqual(l, r)
                TokenType.EQUAL_EQUAL -> isEqual(l, r)
                else -> null
            }
        }

        is Expr.Variable -> lookUpVariable(expr.name, expr)
        is Expr.Assign -> {
            val value = evaluate(expr.value)
            val distance = locals[expr]
            if (distance != null) {
                environment.assignAt(distance, expr.name, value)
            } else {
                globals.assign(expr.name, value)
            }
            value
        }

        is Expr.Logical -> {
            val l = evaluate(expr.l)
            when ((expr.op.type == TokenType.OR) to isTruthy(l)) {
                true to true, false to false -> l
                else -> evaluate(expr.r)
            }
        }

        is Expr.Call -> {
            val callee = evaluate(expr.callee)
            val arguments = mutableListOf<Any?>()
            expr.arguments.map(::evaluate)
                .forEach(arguments::add)
            val fn = callee as? LoxCallable ?: run {
                throw RuntimeError(expr.paren, "Can only call functions and classes.")
            }
            if (arguments.size != fn.arity()) {
                throw RuntimeError(
                    expr.paren,
                    "Expected ${fn.arity()} arguments but got ${arguments.size}."
                )
            }
            fn(this, arguments)
        }
    }

    override fun visitStmt(stmt: Stmt) {
        when (stmt) {
            is Stmt.Expression -> {
                evaluate(stmt.expr)
            }

            is Stmt.Print -> {
                val value = evaluate(stmt.expr)
                println(stringify(value))
            }

            is Stmt.Var -> {
                val value = stmt.initializer?.run(::evaluate)
                environment.define(stmt.name.lexeme, value);
            }

            is Stmt.Block -> {
                executeBlock(stmt.statements, Environment(environment))
            }

            is Stmt.If -> {
                if (isTruthy(evaluate(stmt.cond))) {
                    execute(stmt.thenStmt)
                } else if (stmt.elseStmt != null) {
                    execute(stmt.elseStmt)
                }
            }

            is Stmt.While -> {
                while (isTruthy(evaluate(stmt.cond)))
                    execute(stmt.body)
            }

            is Stmt.Function -> {
                val fn = LoxFunction(stmt, environment)
                environment.define(stmt.name.lexeme, fn)
            }

            is Stmt.Return -> {
                val value = stmt.value?.let { evaluate(it) }
                throw Return(value)
            }
        }
    }

    // MARK: - private fun

    private fun evaluate(expr: Expr) = expr.accept(this)

    fun executeBlock(
        statements: List<Stmt?>,
        environment: Environment
    ) {
        val previous: Environment = this.environment
        try {
            this.environment = environment
            statements.forEach(::execute)
        } finally {
            this.environment = previous
        }
    }

    private fun isTruthy(obj: Any?) = when (obj) {
        null -> false
        is Boolean -> obj
        else -> true
    }

    private fun isEqual(a: Any?, b: Any?) = when (a) {
        null -> b == null
        else -> a == b
    }

    private fun <R> checkNumberOperand(op: Token, operand: Any?, cb: () -> R): R {
        if (operand is Double) return cb()
        throw RuntimeError(op, "Operand must be a number.")
    }

    private fun <R> checkNumberOperands(op: Token, l: Any?, r: Any?, cb: () -> R): R {
        if (l is Double && r is Double) return cb()
        throw RuntimeError(op, "Operands must be numbers.")
    }

    private fun stringify(obj: Any?): String {
        if (obj == null) return "nil"

        if (obj is Double) {
            var text = obj.toString()
            if (text.endsWith(".0")) {
                text = text.take(text.length - 2)
            }
            return text
        }

        return obj.toString()
    }

    private fun execute(stmt: Stmt?) {
        stmt?.accept(this)
    }

    private fun lookUpVariable(name: Token, expr: Expr): Any? {
        return locals[expr]?.let { environment.getAt(it, name.lexeme) } ?: globals[name]
    }

    // MARK: - public fun

    fun interpret(statements: List<Stmt?>) {
        try {
            statements.forEach(::execute)
        } catch (error: RuntimeError) {
            runtimeError(error)
        }
    }

    fun resolve(expr: Expr, depth: Int) {
        locals[expr] = depth
    }
}