package org.example

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

    private fun expression() = equality()

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
        return primary()
    }

    private fun primary() = when {
        match(TokenType.FALSE) -> Expr.Literal(false)
        match(TokenType.TRUE) -> Expr.Literal(true)
        match(TokenType.NIL) -> Expr.Literal(null)
        match(TokenType.NUMBER, TokenType.STRING) -> Expr.Literal(previous().literal)
        match(TokenType.LEFT_PAREN) -> {
            val expr = expression()
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
            Expr.Grouping(expr)
        }

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

    // MARK: - public fun

    fun parse() = try {
        expression()
    } catch (e: ParseError) {
        null
    }
}