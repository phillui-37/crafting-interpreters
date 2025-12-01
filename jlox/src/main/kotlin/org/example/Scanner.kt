package org.example

class Scanner(private val src: String) {
    // MARK: - static
    companion object {
        private val keywords = mapOf(
            "and" to TokenType.AND,
            "class" to TokenType.CLASS,
            "else" to TokenType.ELSE,
            "false" to TokenType.FALSE,
            "for" to TokenType.FOR,
            "fun" to TokenType.FUN,
            "if" to TokenType.IF,
            "nil" to TokenType.NIL,
            "or" to TokenType.OR,
            "print" to TokenType.PRINT,
            "return" to TokenType.RETURN,
            "super" to TokenType.SUPER,
            "this" to TokenType.THIS,
            "true" to TokenType.TRUE,
            "var" to TokenType.VAR,
            "while" to TokenType.WHILE
        )
    }

    // MARK: - var

    private val tokens = mutableListOf<Token>()
    private var start = 0
    private var current = 0
    private var line = 1

    // MARK: - private fun

    private fun isAtEnd() = current >= src.length

    private fun scanToken() {
        fun eqMatch(t: TokenType, f: TokenType) = if (match('=')) t else f
        val c = advance()
        when (c) {
            '(' -> TokenType.LEFT_PAREN
            ')' -> TokenType.RIGHT_PAREN
            '{' -> TokenType.LEFT_BRACE
            '}' -> TokenType.RIGHT_BRACE
            ',' -> TokenType.COMMA
            '.' -> TokenType.DOT
            '-' -> TokenType.MINUS
            '+' -> TokenType.PLUS
            ';' -> TokenType.SEMICOLON
            '*' -> TokenType.STAR
            '!' -> eqMatch(TokenType.BANG_EQUAL, TokenType.BANG)
            '=' -> eqMatch(TokenType.EQUAL_EQUAL, TokenType.EQUAL)
            '<' -> eqMatch(TokenType.LESS_EQUAL, TokenType.LESS)
            '>' -> eqMatch(TokenType.GREATER_EQUAL, TokenType.GREATER)
            '/' -> {
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) advance()
                    null
                } else if (match('*')) {
                    blkComment()
                    null
                } else {
                    TokenType.SLASH
                }
            }

            ' ', '\r', '\t' -> {
                // ignore white space
                null
            }

            '\n' -> {
                line++
                null
            }

            '"' -> {
                string()
                null
            }

            'o' -> if (match('r')) TokenType.OR else null

            'a' -> if (match('n') && match('d')) TokenType.AND else null

            else -> {
                if (isDigit(c)) {
                    number()
                } else if (isAlpha(c)) {
                    identifier()
                } else {
                    Lox.error(line, "Invalid token")
                }
                null
            }
        }?.also(::addToken)
    }

    private fun advance() = src[current++]

    private fun addToken(type: TokenType) = addToken(type, null)

    private fun addToken(type: TokenType, literal: Any?) {
        val text = src.substring(start, current)
        tokens.add(Token(type, text, literal, line))
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd() || src[current] != expected) return false
        current++;
        return true
    }

    private val peek = {
        if (isAtEnd()) '\u0000' else src[current]
    }

    private fun string() {
        while (peek() != '"' && !isAtEnd()) {
            // support multiline string
            if (peek() == '\n') line++
            advance()
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated String")
            return
        }

        advance()
        addToken(TokenType.STRING, src.substring(start + 1, current - 1))
    }

    private fun isDigit(c: Char) = c in '0'..'9'

    private fun number() {
        while (isDigit(peek())) advance()

        if (peek() == '.' && isDigit(peekNext())) {
            advance()

            while (isDigit(peek())) advance()
        }

        addToken(
            TokenType.NUMBER,
            src.substring(start, current).toDouble()
        )
    }

    private fun peekNext() = if (current + 1 >= src.length) '\u0000' else src[current + 1]

    private fun identifier() {
        while (isAlphaNumeric(peek())) advance()
        val text = src.substring(start, current)
        val type = keywords[text] ?: TokenType.IDENTIFIER
        addToken(type)
    }

    private fun isAlpha(c: Char) = (c in 'a'..'z') || (c in 'A'..'Z') || c == '_'

    private fun isAlphaNumeric(c: Char) = isAlpha(c) || isDigit(c)

    private fun blkComment() {
        while (!match('*') && !match('/')) {
            // support multiline string
            if (peek() == '\n') line++
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated String")
            return
        }

        advance()
    }


    // MARK: - public fun

    val scanTokens = {
        while (!isAtEnd()) {
            start = current;
            scanToken()
        }

        tokens.add(Token(TokenType.EOF, "", null, line))
        tokens
    }
}
