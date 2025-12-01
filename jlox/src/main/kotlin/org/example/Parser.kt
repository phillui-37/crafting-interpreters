package org.example

import org.example.Expr.Assign


class Parser(private val tokens: List<Token>) {
    // MARK: - static
    companion object {
        private class ParseError(msg: String?) : RuntimeException(msg) {
            constructor() : this(null)
        }
    }


    // MARK: - val

    private var current = 0

    // MARK: - private fun

    private fun match(vararg types: TokenType): Boolean {
        val target = types.find(::check)
        if (target != null) {
            advance()
        }
        return target != null
    }

    private fun check(type: TokenType) = !isAtEnd() && peek().type == type

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun isAtEnd() = peek().type == TokenType.EOF

    private fun peek() = tokens[current]

    private fun previous() = tokens[current - 1]

    private fun expression() = assignment()

    private fun statement() = when {
        match(TokenType.FOR) -> forStatement()
        match(TokenType.IF) -> ifStatement()
        match(TokenType.WHILE) -> whileStatement()
        match(TokenType.PRINT) -> printStatement()
        match(TokenType.RETURN) -> returnStatement()
        match(TokenType.LEFT_PAREN) -> Stmt.Block(block())
        else -> expressionStatement()
    }

    private fun returnStatement(): Stmt {
        val keyword = previous()
        val value = if (check(TokenType.SEMICOLON)) null else expression()
        consume(TokenType.SEMICOLON, "Expect ';' after return value;")
        return Stmt.Return(keyword, value)
    }

    private fun forStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.")

        val initializer = when {
            match(TokenType.SEMICOLON) -> null
            match(TokenType.VAR) -> varDeclaration()
            else -> expressionStatement()
        }

        var cond = if (check(TokenType.SEMICOLON)) null else expression()
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition.")

        val increment = if(check(TokenType.RIGHT_PAREN)) null else expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.")

        var body = statement()

        if (increment != null)
            body = Stmt.Block(listOf(body, Stmt.Expression(increment)))

        if (cond == null) cond = Expr.Literal(true)
        body = Stmt.While(cond, body)

        if (initializer != null)
            body = Stmt.Block(listOf(initializer, body))

        return body
    }

    private fun whileStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.")
        val cond = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.")
        val body = statement()

        return Stmt.While(cond, body)
    }

    private fun ifStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.")
        val cond = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.")

        val thenStmt = statement()
        val elseStmt = if (match(TokenType.ELSE)) statement() else null
        return Stmt.If(cond, thenStmt, elseStmt)
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(TokenType.SEMICOLON, "Expect ';' after value.")
        return Stmt.Print(value)
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        consume(TokenType.SEMICOLON, "Expect ';' after expression.")
        return Stmt.Expression(expr)
    }

    private fun block(): List<Stmt?> {
        val statements: MutableList<Stmt?> = ArrayList()

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration())
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after block.")
        return statements
    }

    private fun assignment(): Expr {
        val expr = or()

        if (match(TokenType.EQUAL)) {
            val equals = previous()
            val value = assignment()

            if (expr is Expr.Variable) {
                val name = expr.name
                return Assign(name, value)
            } else if (expr is Expr.Get) {
                return Expr.Set(expr.obj, expr.name, value)
            }

            error(equals, "Invalid assignment target.")
        }

        return expr
    }

    private fun or(): Expr {
        var expr = and()

        while (match(TokenType.OR)) {
            val op = previous()
            val r = and()
            expr = Expr.Logical(expr, op, r)
        }
        return expr
    }

    private fun and(): Expr {
        var expr = equality()

        while (match(TokenType.AND)) {
            val op = previous()
            val r = equality()
            expr = Expr.Logical(expr, op, r)
        }

        return expr
    }

    private fun equality(): Expr {
        var expr = comparison()

        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            val op = previous()
            val r = comparison()
            expr = Expr.Binary(expr, op, r)
        }

        return expr
    }

    private fun comparison(): Expr {
        var expr = term()

        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            val op = previous()
            val r = term()
            expr = Expr.Binary(expr, op, r)
        }

        return expr
    }

    private fun term(): Expr {
        var expr = factor()

        while (match(TokenType.MINUS, TokenType.PLUS)) {
            val op = previous()
            val r = factor()
            expr = Expr.Binary(expr, op, r)
        }

        return expr
    }

    private fun factor(): Expr {
        var expr = unary()

        while (match(TokenType.SLASH, TokenType.STAR)) {
            val op = previous()
            val r = unary()
            expr = Expr.Binary(expr, op, r)
        }

        return expr
    }

    private fun unary(): Expr {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            val op = previous()
            val r = unary()
            return Expr.Unary(op, r)
        }
        return call()
    }

    private fun finishCall(callee: Expr): Expr {
        val arguments = mutableListOf<Expr>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size >= 255) {
                    error(peek(), "Can't have more than 255 arguments.")
                }
                arguments.add(expression())
            } while (match(TokenType.COMMA))
        }

        val paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.")
        return Expr.Call(callee, paren, arguments)
    }

    private fun call(): Expr {
        var expr = primary()
        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr)
            } else if (match(TokenType.DOT)) {
                val name = consume(TokenType.IDENTIFIER, "Expect property name after '.'.")
                expr = Expr.Get(expr, name)
            } else {
                break
            }
        }
        return expr
    }

    private fun primary() = when {
        match(TokenType.FALSE) -> Expr.Literal(false)
        match(TokenType.TRUE) -> Expr.Literal(true)
        match(TokenType.NIL) -> Expr.Literal(null)
        match(TokenType.NUMBER, TokenType.STRING) -> Expr.Literal(previous().literal)
        match(TokenType.IDENTIFIER) -> Expr.Variable(previous())
        match(TokenType.LEFT_PAREN) -> {
            val expr = expression()
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
            Expr.Grouping(expr)
        }
        match(TokenType.SUPER) -> {
            val keyword = previous()
            consume(TokenType.DOT, "Expect '.' after 'super'.")
            val method = consume(TokenType.IDENTIFIER, "Expect superclass method name.");
            Expr.Super(keyword, method)
        }
        match(TokenType.THIS) -> Expr.This(previous())

        else -> throw error(peek(), "Expect expression.")
    }

    private fun consume(type: TokenType, msg: String): Token {
        if (check(type)) return advance()
        throw error(peek(), msg)
    }

    private fun error(token: Token, msg: String): ParseError {
        Lox.error(token, msg)
        return ParseError()
    }

    private fun synchronize() {
        advance()

        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return
            when (peek().type) {
                TokenType.CLASS, TokenType.FUN, TokenType.VAR, TokenType.FOR, TokenType.IF, TokenType.WHILE, TokenType.PRINT, TokenType.RETURN -> return
                else -> {}
            }
        }

        advance()
    }

    private fun declaration(): Stmt? {
        try {
            return when {
                match(TokenType.CLASS) -> classDeclaration()
                match(TokenType.FUN) -> function("function")
                match(TokenType.VAR) -> varDeclaration()
                else -> statement()
            }
        } catch (error: ParseError) {
            synchronize()
            return null
        }
    }

    private fun classDeclaration(): Stmt.Class {
        val name = consume(TokenType.IDENTIFIER, "Expect class name.")

        val superClass = if (match(TokenType.LESS)) run {
            consume(TokenType.IDENTIFIER, "Expect superclass name.")
            Expr.Variable(previous())
        } else null

        consume(TokenType.LEFT_BRACE, "Expect '{' before class body.")

        val methods = mutableListOf<Stmt.Function>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"))
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.")
        return Stmt.Class(name, superClass, methods)
    }

    private fun function(kind: String): Stmt.Function {
        val name = consume(TokenType.IDENTIFIER, "Expect $kind name.")
        consume(TokenType.LEFT_PAREN, "Expect '(' after $kind name.")
        val parameters = mutableListOf<Token>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size >= 255) {
                    error(peek(), "Can't have more than 255 parameters.")
                }

                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."))
            } while(match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters")

        consume(TokenType.LEFT_BRACE, "Expect '{' before $kind body.")
        val body = block()
        return Stmt.Function(name, parameters, body)
    }

    private fun varDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect variable name.")

        val initializer = if (match(TokenType.EQUAL)) expression() else null

        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.")
        return Stmt.Var(name, initializer)
    }

    // MARK: - public fun

    fun parse(): List<Stmt?> {
        val statements: MutableList<Stmt?> = ArrayList()
        while (!isAtEnd()) {
            statements.add(declaration())
        }

        return statements
    }
}