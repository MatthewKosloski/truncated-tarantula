package me.mtk.truncatedtarantula;

import java.util.List;
import java.util.ArrayList;

// The Parser is the part of the interpreter that takes
// a list of Token objects as input and, from those tokens, 
// constructs an abstract syntax tree (AST). The construction
// of the AST is based on the grammar of the language of 
// the source program. That is, the Parser is an implementation
// of the grammar. Each nonterminal symbol of the grammar
// is implemented as a method.
// 
// The Parser is also responsible for reporting syntax errors
// to the user.
public class Parser 
{
    // The tokens of the source program. These come from
    // the Lexer.
    private final List<Token> tokens;

    // The current position in the token list (an index in tokens).
    // This member can take on any value in the range [0, n - 1], where
    // n is the size of tokens. This is the index in tokens of the next
    // Token that is to be processed. That is, the index of the Token
    // that is currently being processed is one less than this value.
    private int position = 0;

    /**
     * Constructs a new Parser object, initializing
     * it with a list of tokens.
     * 
     * @param tokens A list of tokens.
     */
    public Parser(List<Token> tokens)
    {
        this.tokens = tokens;
    }

    /**
     * Parses the program.
     * 
     * @return A list of expressions representing the program.
     */
    public List<Expr> parse() throws ParseError
    {
        return program();
    }

    /*
     * Implements the following production rule:
     * program -> expression* EOF ;
     *
     * @return A list of expressions to be interpreted.
     */
    private List<Expr> program()
    {
        List<Expr> expressions = new ArrayList<>();
        
        while (hasTokens()) 
            expressions.add(expression());
        
        return expressions;
    }

    // expression -> equality 
    //            | let 
    //            | print 
    //            | if
    //            | cond 
    //            | logical ;
    private Expr expression()
    {
        if (peek(TokenType.LPAREN) && peekNext(TokenType.LET))
        {
            // expression -> let ;
            return let();
        }
        else if (peek(TokenType.LPAREN) && 
            peekNext(TokenType.PRINT, TokenType.PRINTLN))
        {
            // expression -> print ;
            return print();
        }
        else if (peek(TokenType.LPAREN) && peekNext(TokenType.IF))
        {
            // expression -> if ;
            return ifExpr();
        }
        else if (peek(TokenType.LPAREN) && peekNext(TokenType.COND))
        {
            // expression -> cond ;
            return cond();
        }
        else if (peek(TokenType.LPAREN) && 
            peekNext(TokenType.AND, TokenType.OR))
        {
            // expression -> logical ;
            return logical();
        }

        // expression -> equality ;
        return equality();
    }

    private Expr logical()
    {
        // Consume (
        nextToken();

        Token operator = nextToken();
        Expr first = expression();
        Expr second = expression();
        Expr expr = new Expr.Logical(operator, first, second);

        while (peekExpr())
        {
            second = expression();
            expr = new Expr.Logical(operator, expr, second);
        }

        consumeRightParen(operator.lexeme);
        return expr;
    }

    // cond -> "(" "cond" clause+ else? ")" ;
    private Expr cond()
    {
        // Consume (cond
        nextToken();
        nextToken();

        List<Expr.Clause> clauses = new ArrayList<>();

        while (peekExpr() && !peekElse() && hasTokens())
            clauses.add(clause());

        Expr elseExpr = null;

        if (peekElse())
        {
            // Consume (else
            nextToken();
            nextToken();
            
            elseExpr = body();
            consumeRightParen("else");
        }

        consumeRightParen("cond");

        return new Expr.Cond(clauses, elseExpr);
    }

    // clause -> "(" expression body ")" ;
    private Expr.Clause clause()
    {
        consume(TokenType.LPAREN, 
            "Expected a '(' to start a clause expression");

        if (!peekExpr())
            throw new ParseError(peek(), "Expected an expression");

        Expr condition = expression();

        Expr.Body body = body();

        consumeRightParen("");

        return new Expr.Clause(condition, body);
    }

    // if -> "(" "if" expression then else? ")" ;
    private Expr ifExpr()
    {
        // Consume (if
        nextToken();
        nextToken();

        Expr cond = expression();

        consume(TokenType.LPAREN, "Expected '(' to begin 'then' expression");
        consume(TokenType.THEN, "Expected 'then' expression");

        Expr thenExpr = body();

        consumeRightParen("then");

        Expr elseExpr = null;

        if (peekElse())
        {
            // Consume (else
            nextToken();
            nextToken();
            
            elseExpr = body();
            consumeRightParen("else");
        }

        consumeRightParen("if");

        return new Expr.IfExpr(cond, thenExpr, elseExpr);
    }

    // let -> "(" "let" bindings body ")" ;
    private Expr let()
    {
        // Consume (let
        nextToken();
        nextToken();

        // bindings -> "[" binding+ "]" ;
        consume(TokenType.LBRACKET, String.format("Expected a '[' to start the " + 
            "identifier initialization list but got '%s' instead", peek().lexeme));

        if (!peek(TokenType.IDENTIFIER))
            throw new ParseError(peek(), String.format("Expected an identifier " +
            "after '[' but got '%s' instead", peek().lexeme));

        List<Expr.Binding> bindings = new ArrayList<>();
        while (!match(TokenType.RBRACKET) && hasTokens())
        {
            if (peek(TokenType.RPAREN))
            {
                throw new ParseError(peek(), String.format("Expected a ']' to " +
                    "end the identifier initialization list but got '%s' instead",
                    peek().lexeme));
            }

            consume(TokenType.IDENTIFIER, "Expected an identifier");
            Token identifier = previous();

            if (!peekExpr())
            {
                throw new ParseError(peek(), String.format("Expected an " + 
                "expression after identifier '%s' but got '%s' instead",
                previous().lexeme, peek().lexeme));
            }

            Expr value = expression();
            bindings.add(new Expr.Binding(identifier, value));
        }

        Expr body = body();

        consumeRightParen("let");
        
        return new Expr.Let(bindings, body);
    }

    // body -> expression* ;
    private Expr.Body body()
    {
        List<Expr> exprs = new ArrayList<>();

        while (peekExpr() && hasTokens())
            exprs.add(expression());
        

            return new Expr.Body(exprs);
    }

    // print -> "(" ("print" | "println") equality+ ")" ;
    private Expr print()
    {
        // Consume (
        nextToken();

        // print or println
        Token operator = nextToken();

        Expr.Body body = body();

        consumeRightParen(operator.lexeme);
        return new Expr.Print(operator, body);
    }

    // equality -> "(" ("==" | "!=") comparison comparison+ ")" ;
    private Expr equality()
    {
        if (peek(TokenType.LPAREN) && peekNext(TokenType.EQUAL_PREDICATE,
            TokenType.NEQUAL_PREDICATE))
        {
            // Consume ( 
            nextToken();

            Token operator = nextToken();
            Expr first = comparison();
            Expr second = comparison();
            Expr expr = new Expr.Binary(operator, first, second);

            while (peekExpr())
            {
                second = comparison();
                expr = new Expr.Binary(operator, expr, second);
            }

            consumeRightParen(operator.lexeme);
            return expr;
        }

        return comparison();
    }

    // comparison -> "(" ( ">" | ">=" | "<" | "<=" ) binary binary+ ")" ; 
    private Expr comparison()
    {
        if (peek(TokenType.LPAREN) && peekNext(TokenType.GREATER_THAN, 
            TokenType.GREATER_THAN_OR_EQUAL_TO, TokenType.LESS_THAN, 
            TokenType.LESS_THAN_OR_EQUAL_TO))
        {
            // Consume (
            nextToken();

            Token operator = nextToken();
            Expr first = binary();
            Expr second = binary();
            Expr expr = new Expr.Binary(operator, first, second);

            while (peekExpr())
            {
                second = binary();
                expr = new Expr.Binary(operator, expr, second);
            }

            consumeRightParen(operator.lexeme);
            return expr;
        }

        return binary();
    }

    /*
     * Implements the following production rule:
     * binary -> "(" ("+" | "-" | "*" | "/" | "//" | "%") unary unary+ ")" ;
     *
     * @return A binary expression.
     */
    private Expr binary()
    {
        if (hasBinaryExpression())
        {
            // Consume (
            nextToken();

            Token operator = nextToken();
            Expr first = unary();
            Expr second = unary();
            Expr expr = new Expr.Binary(operator, first, second);

            while (peekExpr())
            {
                second = unary();
                expr = new Expr.Binary(operator, expr, second);
            }

            consumeRightParen(operator.lexeme);
            return expr;
        }

        return unary();
    }

     /*
     * Implements the following production rule:
     * unary -> ("+" | "-" | "not" | "true?" | "typeof") expression | literal ;
     *
     * @return A unary expression.
     */
    private Expr unary()
    {
        // TODO: Make sure next token is the start of
        // or is an expression
        if (peek(TokenType.PLUS, TokenType.MINUS) && !peekNext(TokenType.PLUS,
            TokenType.MINUS))
        {
            Token operator = nextToken();
            Expr right = expression();

            return new Expr.Unary(operator, right);
        }
        else if (peek(TokenType.LPAREN) && peekNext(TokenType.NOT, 
            TokenType.TRUE_PREDICATE, TokenType.TYPEOF))
        {
            // Consume (
            nextToken();

            Token operator = nextToken();
            Expr right = expression();

            consumeRightParen(operator.lexeme);
            return new Expr.Unary(operator, right);
        }
        else if (hasBinaryExpression())
        {
            // unary -> binary ;
            return expression();
        }
        
        // unary -> literal ;
        return literal();
    }

    // literal -> string | number | identifier | boolean | "null" ;
    private Expr literal()
    {
        if (match(TokenType.STRING, TokenType.NUMBER))
            // literal -> string | number ;
            return new Expr.Literal(previous().literal);
        else if (match(TokenType.IDENTIFIER))
            // literal -> identifier ;
            return new Expr.Variable(previous());
        else if (match(TokenType.TRUE))
            // literal -> boolean ;
            return new Expr.Literal(true);
        else if (match(TokenType.FALSE))
            // literal -> boolean ;
            return new Expr.Literal(false);
        else if (match(TokenType.NULL))
            // literal -> "null" ;
            return new Expr.Literal(null);

        if (peek(TokenType.IDENTIFIER))
        {
            throwUndefinedIdentifierException(peek());
        }
        else if (peekNext(TokenType.IDENTIFIER))
        {
            throwUndefinedIdentifierException(peekNext());
        } 
        else if (peek(TokenType.UNIDENTIFIED))
        {
            throwBadTokenException(peek());
        }
        else if (peekNext(TokenType.UNIDENTIFIED))
        {
            throwBadTokenException(peekNext());
        }

        throw new ParseError(peek(), String.format("Expected one of the " +
            "following but got '%s' instead:\n * An expression starting " + 
            "with '('\n * A unary expression starting with '+' or '-'\n * " + 
            "String\n * Number\n * Identifier\n * Boolean \n * null", 
            peek().lexeme));
    }

    /*
     * If the next token's type matches at least one 
     * of the provided types, consume it and return true.
     *  
     * @param types A variable number of token types.
     * @return True if the token type of the next token
     * matches at least one of the provided types; False
     * otherwise.
     */
    private boolean match(TokenType... types)
    {
        for (TokenType type : types)
        {
            if (peekType(type))
            {
                nextToken();
                return true;
            }
        }
        return false;
    }

    /*
     * Returns the next token if its type is of the provided type.
     * 
     * @param type The type of the token.
     * @param msg The error message to display if the next token
     * is not of the provided type.
     * @return The next token
     * @throws ParseError if next token is not of the provided type.
     */
    private Token consume(TokenType type, String msg)
    {
        if (peekType(type)) return nextToken();
        throw new ParseError(peek(), msg);
    }

    /*
     * Gets the next token.
     * 
     * @return The next token.
     */
    private Token nextToken()
    {
        if (hasTokens())
            return tokens.get(position++);
        else
            return previous();
    }

    /*
     * Indicates if there are more tokens to process.
     * 
     * @return True if there are no more tokens to process;
     * False otherwise.
     */
    private boolean hasTokens()
    {
        return peek().type != TokenType.EOF;
    }

    /*
     * Returns the next token.
     * 
     * @return The next token.
     */
    private Token peek()
    {
        return tokens.get(position);
    }

    /*
     * Returns the next-next token.
     * 
     * @return The next-next token.
     */
    private Token peekNext()
    {
        return tokens.get(position + 1);
    }

    /*
     * Indicates if the next token is of the provided type.
     * 
     * @param type The type of the token.
     * @return True if the next token's TokenType is equal to
     * type; False otherwise.
     */
    private boolean peekType(TokenType type)
    {
        return peek().type == type;
    }

    /*
     * Indicates if the next-next token is of the provided type.
     * 
     * @param type The type of the token.
     * @return True if the next-next token's TokenType is equal to
     * type; False otherwise.
     */
    private boolean peekNextType(TokenType type)
    {
        return peekNext().type == type;
    }

    /*
     * Indicates whether the next token is one of 
     * the provided token types.
     *  
     * @param types A variable number of token types.
     * @return True if the next token is one of the provided
     * token types; False otherwise.
     */
    private boolean peek(TokenType... types)
    {
        for (TokenType type : types)
        {
            if (peekType(type)) return true;
        }
        return false;
    }

    /*
     * Indicates whether the next-next token is one of 
     * the provided token types.
     *  
     * @param types A variable number of token types.
     * @return True if the next-next token is one of the provided
     * token types; False otherwise.
     */
    private boolean peekNext(TokenType... types)
    {
        for (TokenType type : types)
        {
            if (peekNextType(type)) return true;
        }
        return false;
    }

    /*
     * Returns the previously consumed token.
     * 
     * @return The previously consumed token.
     */
    private Token previous()
    {
        return tokens.get(position - 1);
    }

    /*
     * Indicates if the first token of lookahead is an expression or
     * the start of an expression.
     * 
     * @return True if the first token of lookahead is an expression
     * or the start of an expression; False otherwise.
     */
    private boolean peekExpr()
    {
        return peek(
            TokenType.LPAREN, TokenType.NUMBER, 
            TokenType.MINUS, TokenType.PLUS, 
            TokenType.TRUE, TokenType.FALSE,
            TokenType.NULL, TokenType.IDENTIFIER,
            TokenType.STRING
        );
    }

    /*
     * Indicates if the next expression is an else expression.
     * 
     * @return True if the next expression is an else expression;
     * False otherwise.
     */
    private boolean peekElse()
    {
        return peek(TokenType.LPAREN) && peekNext(TokenType.ELSE);
    }

    /*
     * Indicates if the next expression is a binary arithmetic expression.
     *  
     * @return True if the next expression is a binary arithmetic expression;
     * False otherwise.
     */
    private boolean hasBinaryExpression()
    {
        return peek(TokenType.LPAREN) && peekNext(TokenType.PLUS, 
            TokenType.MINUS, TokenType.STAR, TokenType.SLASH,
            TokenType.PERCENT, TokenType.SLASHSLASH);
    }

    /**
     * If the next token is a right parenthesis, it is consumed. Else,
     * a ParseError is thrown.
     */
    private void consumeRightParen(String exprName)
    {
        consume(TokenType.RPAREN, String.format("Expression '%s' is missing " + 
            " a closing ')'", exprName));
    }

    /*
     * Throws a ParseError with a message indicating
     * a bad token.
     *  
     * @param token The bad token.
     * @throws ParseError
     */
    private void throwBadTokenException(Token token)
    {
        throw new ParseError(token, String.format("Bad token '%s'", 
            token.lexeme));
    }

    /*
     * Throws a ParseError with a message indicating
     * an undefined identifier.
     *  
     * @param token The undefined identifier.
     * @throws ParseError
     */
    private void throwUndefinedIdentifierException(Token token)
    {
        throw new ParseError(token, String.format("Undefined identifier '%s'",
            token.lexeme));
    }
}