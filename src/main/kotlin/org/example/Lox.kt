package org.example

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

object Lox {
    // MARK: - var
    var hadError = false
    var hadRuntimeError = false
    private val interpreter = Interpreter()

    // MARK: - fun
    fun runFile(path: String) {
        val bytes = Files.readAllBytes(Paths.get(path))
        run(String(bytes, Charset.defaultCharset()))
        if (hadError) exitProcess(65)
        if (hadRuntimeError) exitProcess(70)
    }

    fun runPrompt() = InputStreamReader(System.`in`).use { input ->
        BufferedReader(input).use { reader ->
            while (true) {
                print("> ")
                val line = reader.readLine() ?: break
                run(line)
                hadError = false
            }
        }
    }

    fun run(src: String) {
        val scanner = Scanner(src)
        val parser = Parser(scanner.scanTokens())
        val stmts = parser.parse()

        if (hadError) return

        val resolver = Resolver(interpreter)
        resolver.resolve(stmts)

        if (hadError) return

        interpreter.interpret(stmts)
    }

    fun error(line: Int, msg: String) = report(line, "", msg)

    fun error(token: Token, msg: String) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", msg)
        } else {
            report(token.line, " at '${token.lexeme}'",  msg)
        }
    }

    fun report(line: Int, where: String, msg: String) {
        System.err.println("[line $line] Error$where: $msg")
        hadError = true
    }

    fun runtimeError(error: RuntimeError) {
        System.err.println("${error.msg}\n[line${error.token.line}]")
        hadRuntimeError = true
    }
}