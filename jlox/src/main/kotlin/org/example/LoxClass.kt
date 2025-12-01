package org.example

data class LoxClass(
    val name: String,
    val superClass: LoxClass?,
    private val methods: Map<String, LoxFunction> = emptyMap()
) : LoxCallable {
    override fun toString() = name
    override fun arity() = findMethod("init")?.arity() ?: 0

    override operator fun invoke(
        interpreter: Interpreter,
        arguments: List<Any?>
    ): Any? {
        val instance = LoxInstance(this)
        findMethod("init")?.bind(instance)?.invoke(interpreter, arguments)
        return instance
    }

    fun findMethod(name: String): LoxFunction? = when {
        methods.containsKey(name) -> methods[name]
        superClass != null -> superClass.findMethod(name)
        else -> null
    }
}
