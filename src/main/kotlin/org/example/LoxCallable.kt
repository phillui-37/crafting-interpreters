package org.example

interface LoxCallable {
    fun arity(): Int;
    operator fun invoke(interpreter: Interpreter, arguments: List<Any?>): Any?
}

data class LoxFunction(
    val declaration: Stmt.Function,
    val closure: Environment
): LoxCallable {
    override fun arity() = declaration.params.size

    override fun invoke(
        interpreter: Interpreter,
        arguments: List<Any?>
    ): Any? {
        val env = Environment(closure)
        declaration.params.forEachIndexed { index, token ->
            env.define(token.lexeme, arguments[index])
        }

        try {
            interpreter.executeBlock(declaration.body, env)
        } catch (ret: Return) {
            return ret.value
        }
        return null
    }

    override fun toString() = "<fn ${declaration.name.lexeme}>"
}