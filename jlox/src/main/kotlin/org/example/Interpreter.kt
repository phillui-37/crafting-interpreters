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
        is Expr.Unary -> visitUnaryExpr(expr)
        is Expr.Binary -> visitBinaryExpr(expr)
        is Expr.Variable -> lookUpVariable(expr.name, expr)
        is Expr.Assign -> visitAssignExpr(expr)
        is Expr.Logical -> visitLogicalExpr(expr)
        is Expr.Call -> visitCallExpr(expr)
        is Expr.Get -> visitGetExpr(expr)
        is Expr.Set -> visitSetExpr(expr)
        is Expr.This -> visitThisExpr(expr)
        is Expr.Super -> visitSuperExpr(expr)
    }

    override fun visitStmt(stmt: Stmt) {
        when (stmt) {
            is Stmt.Expression -> visitExprStmt(stmt)
            is Stmt.Print -> visitPrintStmt(stmt)
            is Stmt.Var -> visitVarStmt(stmt)
            is Stmt.Block -> visitBlockStmt(stmt)
            is Stmt.If -> visitIfStmt(stmt)
            is Stmt.While -> visitWhileStmt(stmt)
            is Stmt.Function -> visitFunStmt(stmt)
            is Stmt.Return -> visitReturnStmt(stmt)
            is Stmt.Class -> visitClassStmt(stmt)
        }
    }

    // MARK: - private fun

    private fun visitUnaryExpr(expr: Expr.Unary): Any? {
        val r = evaluate(expr.r)
        return when (expr.op.type) {
            TokenType.MINUS -> checkNumberOperand(expr.op, r) {
                -(r as Double)
            }

            TokenType.BANG -> !isTruthy(r)
            else -> null
        }
    }

    private fun visitBinaryExpr(expr: Expr.Binary): Any? {
        val l = evaluate(expr.l)
        val r = evaluate(expr.r)

        return when (expr.op.type) {
            TokenType.MINUS -> checkNumberOperands(expr.op, l, r) { l as Double - r as Double }
            TokenType.SLASH -> checkNumberOperands(expr.op, l, r) { l as Double / r as Double }
            TokenType.STAR -> checkNumberOperands(expr.op, l, r) { l as Double * r as Double }
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

    private fun visitAssignExpr(expr: Expr.Assign) = evaluate(expr.value).also {
        val distance = locals[expr]
        if (distance != null) {
            environment.assignAt(distance, expr.name, it)
        } else {
            globals.assign(expr.name, it)
        }
    }

    private fun visitLogicalExpr(expr: Expr.Logical): Any? {
        val l = evaluate(expr.l)
        return when ((expr.op.type == TokenType.OR) to isTruthy(l)) {
            true to true, false to false -> l
            else -> evaluate(expr.r)
        }
    }

    private fun visitCallExpr(expr: Expr.Call): Any? {
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
        return fn(this, arguments)
    }

    private fun visitGetExpr(expr: Expr.Get): Any? {
        val obj = evaluate(expr.obj)
        if (obj is LoxInstance) {
            return obj.get(expr.name)
        } else {
            throw RuntimeError(expr.name, "Only instances have properties.")
        }
    }

    private fun visitSetExpr(expr: Expr.Set): Any? {
        val obj = evaluate(expr.obj)
        if (obj !is LoxInstance) {
            throw RuntimeError(expr.name, "Only instances have fields.")
        }
        return evaluate(expr.value).also {
            obj.set(expr.name, it)
        }
    }

    private fun visitThisExpr(expr: Expr.This) = lookUpVariable(expr.keyword, expr)

    private fun visitSuperExpr(expr: Expr.Super): Any? {
        val distance = locals[expr]
        val superClass = environment.getAt(distance!!, "super") as LoxClass
        val obj = environment.getAt(distance - 1, "this") as LoxInstance
        val method = superClass.findMethod(expr.method.lexeme) ?: run {
            throw RuntimeError(expr.method, "Undefined property '${expr.method.lexeme}'.")
        }
        return method.bind(obj)
    }

    private fun visitExprStmt(stmt: Stmt.Expression) {
        evaluate(stmt.expr)
    }

    private fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expr)
        println(stringify(value))
    }

    private fun visitVarStmt(stmt: Stmt.Var) {
        val value = stmt.initializer?.run(::evaluate)
        environment.define(stmt.name.lexeme, value);
    }

    private fun visitBlockStmt(stmt: Stmt.Block) {
        executeBlock(stmt.statements, Environment(environment))
    }

    private fun visitIfStmt(stmt: Stmt.If) {
        if (isTruthy(evaluate(stmt.cond))) {
            execute(stmt.thenStmt)
        } else if (stmt.elseStmt != null) {
            execute(stmt.elseStmt)
        }
    }

    private fun visitWhileStmt(stmt: Stmt.While) {
        while (isTruthy(evaluate(stmt.cond)))
            execute(stmt.body)
    }

    private fun visitReturnStmt(stmt: Stmt.Return) {
        val value = stmt.value?.let { evaluate(it) }
        throw Return(value)
    }

    private fun visitFunStmt(stmt: Stmt.Function) {
        val fn = LoxFunction(stmt, environment, false)
        environment.define(stmt.name.lexeme, fn)
    }

    private fun visitClassStmt(stmt: Stmt.Class) {
        val superClass = stmt.superClass?.let(::evaluate)
        if (superClass != null && superClass !is LoxClass) {
            throw RuntimeError(stmt.superClass.name, "Superclass must be a class.")
        }

        environment.define(stmt.name.lexeme, null)

        if (stmt.superClass != null) {
            environment = Environment(environment)
            environment.define("super", superClass)
        }

        val methods = stmt.methods.fold(mutableMapOf<String, LoxFunction>()) { acc, function ->
            acc.also {
                it[function.name.lexeme] = LoxFunction(function, environment, function.name.lexeme == "init")
            }
        }

        val klass = LoxClass(stmt.name.lexeme, superClass, methods)

        if (superClass != null) {
            environment = environment.enclosing!!
        }

        environment.assign(stmt.name, klass)
    }

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