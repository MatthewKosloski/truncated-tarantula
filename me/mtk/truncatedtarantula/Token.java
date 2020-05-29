package me.mtk.truncatedtarantula;

/**
 * Represents a unit of output from the Lexer. Holds
 * information about the token for parsing and error handling.
 */
public class Token
{
    final TokenType type;
    final String lexeme;
    final Object literal;
    final int line;
    final int column;

    Token(TokenType type, String lexeme, Object literal, int line, int column)
    {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
        this.column = column;
    }

    public String toString()
    {
        String result = "";
        result += "Type: " + type + "\n";
        result += "Lexeme: " + lexeme + "\n";
        result += "Literal: " + literal + "\n";
        result += "Line: " + line + "\n";
        result += "Column: " + column + "\n";
        return result;
    }
}