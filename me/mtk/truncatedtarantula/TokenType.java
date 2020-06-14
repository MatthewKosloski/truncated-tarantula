package me.mtk.truncatedtarantula;

enum TokenType 
{
    // Grouping tokens
    LPAREN, RPAREN, LBRACKET, RBRACKET, DQUOTE,

    // Binary arithmetic tokens
    PLUS, MINUS, STAR, SLASH, SLASHSLASH, PERCENT,

    // Unary logical negation operator
    NOT,

    // Unary typeof operator
    TYPEOF,

    // Literals
    NUMBER, STRING, TRUE, FALSE, IDENTIFIER, NULL,

    // Unidentified token,
    UNIDENTIFIED,

    // Print expressions
    PRINT, PRINTLN,

    // Let expression
    LET,

    // Branching expressions
    IF, THEN, ELSE, COND,

    // Logical operators
    AND, OR,

    // equal? and nequal? operators
    EQUAL_PREDICATE, NEQUAL_PREDICATE,

    // true? operator
    TRUE_PREDICATE,

    // > and >= operators
    GREATER_THAN, GREATER_THAN_OR_EQUAL_TO,

    // < and <= operators
    LESS_THAN, LESS_THAN_OR_EQUAL_TO,

    // End of file token
    EOF
}