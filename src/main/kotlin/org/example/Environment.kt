package org.example

class Environment(private val enclosing: Environment? = null) {
    val values = mutableMapOf<String, Any?>()

    operator fun get(name: Token): Any {
        return values[name.lexeme] ?: enclosing?.get(name) ?: throw RuntimeError(
            name,
            "Undefined variable '${name.lexeme}'."
        )
    }

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun assign(name: Token, value: Any?) {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = value
            return
        } else if (enclosing != null) {
            enclosing.assign(name, value);
            return
        }

        throw RuntimeError(
            name,
            "Undefined variable '${name.lexeme}'."
        )
    }

    fun getAt(distance: Int, name: String): Any? {
        return ancestor(distance)?.values[name]
    }

    fun ancestor(distance: Int): Environment? {
        var env: Environment? = this
        (0..<distance).forEach {
            env = env?.enclosing
        }

        return env
    }

    fun assignAt(distance: Int, name: Token, value: Any?) {
        ancestor(distance)?.values?.put(name.lexeme, value)
    }
}