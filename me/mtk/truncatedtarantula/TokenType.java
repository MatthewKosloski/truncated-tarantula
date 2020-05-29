package me.mtk.truncatedtarantula;

enum TokenType 
{
    // Grouping tokens
    LPAREN, RPAREN, LBRACKET, RBRACKET,

    // Binary arithmetic tokens
    PLUS, MINUS, STAR, SLASH, SLASHSLASH, PERCENT,

    // Unary logical negation operator
    NOT,

    // Literal token
    NUMBER,

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

    // Booleans
    TRUE, FALSE,

    // Null
    NULL,

    // Identifier (e.g., variable name)
    IDENTIFIER,
    
    // End of file token
    EOF
}