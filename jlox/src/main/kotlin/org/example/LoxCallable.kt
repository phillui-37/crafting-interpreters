package org.example

interface LoxCallable {
    fun arity(): Int;
    operator fun invoke(interpreter: Interpreter, arguments: List<Any?>): Any?
}

data class LoxFunction(
    val declaration: Stmt.Function,
    private val closure: Environment,
    private val isCstr: Boolean
): LoxCallable {
    override fun arity() = declaration.params.size

    override operator fun invoke(
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
            if (isCstr) return closure.getAt(0, "this")
            return ret.value
        }
        if (isCstr) return closure.getAt(0, "this")
        return null
    }

    override fun toString() = "<fn ${declaration.name.lexeme}>"

    fun bind(instance: LoxInstance): LoxFunction {
        val env = Environment(closure)
        env.define("this", instance)
        return LoxFunction(declaration, env, isCstr)
    }
}