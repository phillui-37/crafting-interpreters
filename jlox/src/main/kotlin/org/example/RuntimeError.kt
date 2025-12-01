package org.example

data class RuntimeError(
    val token: Token,
    val msg: String
): RuntimeException(msg)