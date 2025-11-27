package org.example

import kotlin.system.exitProcess

fun main(vararg args: String) {
    when {
        args.size > 1 -> {
            println("Usage: jlox [script]")
            exitProcess(64)
        }
        args.size == 1 -> { Lox.runFile(args[0]) }
        else -> { Lox.runPrompt() }
    }
}