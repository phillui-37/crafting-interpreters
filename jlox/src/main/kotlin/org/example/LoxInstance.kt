package org.example

data class LoxInstance(
    private val klass: LoxClass
) {
    private val instanceMethods = mapOf<String, LoxCallable>(
        "toString" to object: LoxCallable {
            override fun arity() = 0

            override fun invoke(
                interpreter: Interpreter,
                arguments: List<Any?>
            ) = this@LoxInstance.toString()

            override fun toString() = this@LoxInstance.toString()
        }
    )
    private val fields = mutableMapOf<String, Any?>()
        .also {
            it.putAll(instanceMethods)
        }

    override fun toString() = "${klass.name} instance"

    fun get(name: Token): Any? {
        return when {
            fields.containsKey(name.lexeme) -> fields[name.lexeme]
            else -> klass.findMethod(name.lexeme)?.bind(this) ?: run {
                throw RuntimeError(name, "Undefined property '${name.lexeme}'.")
            }
        }
    }

    fun set(name: Token, value: Any?) {
        fields[name.lexeme] = value
    }
}