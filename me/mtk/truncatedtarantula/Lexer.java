package me.mtk.truncatedtarantula;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

// The Lexer is the part of the interpreter that takes
// a source program (written in the language that is
// being interpreted) as input and outputs a sequence
// of Token objects. These tokens will be used by the
// Parser to construct an abstract syntax tree (AST).
public class Lexer
{
    
    // Stores the begin index and end index
    // of a line in source. That is, a line
    // is a substring of source.
    private class Line
    {
        private final int beginIndex;
        private final int endIndex;

        public Line(int beginIndex, int endIndex)
        {
            this.beginIndex = beginIndex;
            this.endIndex = endIndex;
        }
    }

    // The source program, written in the language being interpreted.
    private String source;

    // Stores the accumulated tokens, which are to be given to the 
    // Parser as input.
    private List<Token> tokens = new ArrayList<>();

    // Stores the lines of source.
    private List<Line> lines = new ArrayList<>();

    // The line in source that is currently being processed.
    private int currentLineNumber = 1;

    // The current column of the line in source that is currently
    // being processed. This is reset whenever a new line
    // whitespace character is encountered.
    private int currentColumnNumber = 0;

    // The index (in source) of the first character of the
    // lexeme currently being processed. This gets reset when
    // the Lexer begins constructing another Token.
    private int lexemeStart = 0;

    // The index (in source) of the first character of the current line.
    private int lineStart = 0;

    // The current position in the source string (an index in source).
    // This member can take on any value in the range [0, n - 1], where
    // n is the length of source. This is the index in source of the next
    // character that is to be processed. That is, the index of the character
    // that is currently being processed is one less than this value.
    private int position = 0;

    // Stores the reserved identifiers of the language.
    private static final Map<String, TokenType> keywords;

    static 
    {
        keywords = new HashMap<>();
        keywords.put("print", TokenType.PRINT);
        keywords.put("println", TokenType.PRINTLN);
        keywords.put("let", TokenType.LET);
        keywords.put("true", TokenType.TRUE);
        keywords.put("false", TokenType.FALSE);
        keywords.put("null", TokenType.NULL);
        keywords.put("not", TokenType.NOT);
        keywords.put("equal?", TokenType.EQUAL_PREDICATE);
        keywords.put("nequal?", TokenType.NEQUAL_PREDICATE);
        keywords.put("true?", TokenType.TRUE_PREDICATE);
        keywords.put("if", TokenType.IF);
        keywords.put("then", TokenType.THEN);
        keywords.put("else", TokenType.ELSE);
        keywords.put("cond", TokenType.COND);
        keywords.put("and", TokenType.AND);
        keywords.put("or", TokenType.OR);
    }

    /**
     * Initializes a new Lexer with a source program.
     * 
     * @param source A source program written in the language
     * being interpreted.
     */
    public Lexer(String source)
    {
        this.source = source;
    }

    /**
     * Scans the input program and returns the tokens.
     * 
     * @return A list of tokens obtained by scanning the source program.
     */
    public List<Token> getTokens()
    {
        while (!isEndOfFile())
        {
            // We are at the beginning of the next lexeme
            lexemeStart = position;

            scanToken();
        }

        // Add the last line
        lines.add(new Line(lineStart, position));

        // Append the end-of-file token to the list
        tokens.add(new Token(TokenType.EOF, "", null,
            currentLineNumber, ++currentColumnNumber));
        
        return tokens;
    }

    /**
     * Returns the nth line of source.
     * 
     * @param n A line number.
     * @return The nth line of the source program.
     */
    public String getLine(int n)
    {
        int lineIndex = n - 1;

        if (lineIndex < 0 || lineIndex > lines.size() - 1)
        {
            throw new IllegalArgumentException("Argument n must be an integer " +
                "in the range [0, m - 1], where m is the amount of " +
                "lines in the source program");
        }

        Line line = lines.get(lineIndex);

        return source.substring(line.beginIndex, line.endIndex);
    }

    /*
     * Starting at the current position in the source program,
     * scans the source program for a Token with the help of
     * lookahead characters.
     */
    private void scanToken()
    {
        char currentChar = nextChar();

        switch (currentChar)
        {
            // Grouping characters
            case '(': addToken(TokenType.LPAREN); break;
            case ')': addToken(TokenType.RPAREN); break;
            case '[': addToken(TokenType.LBRACKET); break;
            case ']': addToken(TokenType.RBRACKET); break;
            case '"': string(); break;

            // Binary arithmetic operators
            case '+': addToken(TokenType.PLUS); break;
            case '-': addToken(TokenType.MINUS); break;
            case '*': addToken(TokenType.STAR); break;
            case '%': addToken(TokenType.PERCENT); break;
            case ';': consumeInlineComment(); break;

            // > and >= operators
            case '>':
                if (match('='))
                {
                    addToken(TokenType.GREATER_THAN_OR_EQUAL_TO, 
                        currentColumnNumber - 1);
                }
                else
                    addToken(TokenType.GREATER_THAN);
            break;

            // < and <= operators
            case '<':
                if (match('='))
                {
                    addToken(TokenType.LESS_THAN_OR_EQUAL_TO, 
                        currentColumnNumber - 1);
                }
                else
                    addToken(TokenType.LESS_THAN);
            break;

            // Binary division operators
            case '/': 
                if (match('/')) 
                    // integer (floor) division operator
                    addToken(TokenType.SLASHSLASH);
                else 
                    // division operator
                    addToken(TokenType.SLASH);
            break;
            
            case '`':
                if (match('`')) consumeBlockComment(); break;

            default:
                if (getLastToken().type == TokenType.DQUOTE)
                    string();
                else if (isDigit(currentChar))
                    number();
                else if (isStartOfIdentifier(currentChar))
                    identifier();
                else if (!isWhitespace(currentChar))
                    addToken(TokenType.UNIDENTIFIED);

                break;
        }
    }

    /*
     * Returns the most recent token that was added to the list.
     * @param Token the most recently added token.
     */
    private Token getLastToken()
    {
        if (tokens.size() == 0)
            return tokens.get(0);
        else
            return tokens.get(tokens.size() - 1);
    }

    /*
     * Returns the second most recent token that was added to the list.
     * @param Token the second most recently added token.
     */
    private Token getSecondToLastToken()
    {
        if (tokens.size() == 0)
            return tokens.get(0);
        else
            return tokens.get(tokens.size() - 2);
    }

    /*
     * Handles the scanning of strings.
     *  
     * @throws LexicalError if an unterminated string is encountered.
     */
    private void string()
    {
        // Cache line and column of first char of string
        int startLine = currentLineNumber;
        int startColumn = currentColumnNumber;

        while (peek() != '"' && !isEndOfFile()) nextChar();

        String str = source.substring(lexemeStart, position + 1);
        
        // remove quotes
        str = str.substring(1, str.length() - 1);

        Token strToken = new Token(TokenType.STRING, str, str, startLine, 
            startColumn);
        tokens.add(strToken);

        if (isEndOfFile())
        {
            // The opening quote of the unterminated string
            Token beginQuote = getSecondToLastToken();

            throw new LexicalError(beginQuote, String.format("Unterminated " + 
                "string starting at ln %d, col %d", beginQuote.line, 
                beginQuote.column));
        }

        // consume "
        nextChar();
    }

    /*
     * Handles the scanning of identifiers. If the identifier is a keyword, 
     * then a token with the type of that keyword is added to the token list. 
     * If the identifier is not a keyword, then an IDENTIFIER token is added.
     */
    private void identifier()
    {
        // Cache the column number
        int startColumn = currentColumnNumber;

        // Consume the identifier
        while (isPartOfIdentifier(peek())) nextChar();

        String identifier = source.substring(lexemeStart, position);
        TokenType type = keywords.get(identifier);

        addToken(type == null ? TokenType.IDENTIFIER : type, startColumn);
    }

    /*
     * Returns the next character of the source program that is to be
     * scanned by the Lexer and advances the position in the source program.
     * 
     * @return The next character to be scanned by the Lexer.
     */
    private char nextChar()
    {
        char nextChar = source.charAt(position++);
        
        if (nextChar == '\n')
        {
            lines.add(new Line(lineStart, position - 1));
            currentLineNumber++;
            currentColumnNumber = 0;
            lineStart = position;
        }
        else
            currentColumnNumber++;

        return nextChar;
    }

     /*
     * Returns the next character to be scanned (one character
     * of lookahead).
     * 
     * @return The first character of lookahead. 
     */
    private char peek()
    {
        // There is no next character, so return 
        // the null character.
        if (isEndOfFile()) return '\0';

        char peek = source.charAt(position);
        
        return peek;
    }

    /*
     * Returns the second character of lookahead.
     * 
     * @return The first character of lookahead. 
     */
    private char peekNext()
    {
        if (position + 1 >= source.length())
        {
            // There is no next character, so return 
            // the null character.
            return '\0';
        }
        
        char peekNext = source.charAt(position + 1);

        return peekNext;
    }

    /*
     * If the next character is c, then consume it and return true. 
     *  
     * @param c The character for which we would like to find a match.
     * @return True if the first character of lookahead is the provided
     * character c; False otherwise.
     */
    private boolean match(char c)
    {
        if (peek() == c)
        {
            nextChar();
            return true;
        }

        return false;
    }

    /*
     * If the next character is a (first character of lookahead)
     * and the next next character is b (second character of lookahead),
     * then consume the characters and return true.
     * 
     * @param a The first character for which we would like to find a match.
     * @param b The character after a for which we would like to find a match.
     * @return True if the first character of lookahead is a and the second
     * character of lookahead is b; False otherwise.
     */
    private boolean match(char a, char b)
    {
        if (peek() == a && peekNext() == b)
        {
            nextChar();
            nextChar();
            return true;
        }
        
        return false;
    }

    /*
     * Consumes an inline comment, silently advancing the position
     * in the source program. 
     */
    private void consumeInlineComment()
    {
        while (peek() != '\n' && !isEndOfFile()) nextChar();
    }

    /*
     * Consumes a block comment, silently advancing the position
     * in the source program.
     */
    private void consumeBlockComment()
    {
        while (!match('`', '`') && !isEndOfFile()) nextChar();
    }

    /*
     * Handles the scanning of numbers, both integer
     * and decimal. 
     */
    private void number()
    {
        // Cache the column number at this point
        // because subsequent calls to nextChar()
        // will update the column number.
        int startColumn = currentColumnNumber;

        // Consume the integer, or if a decimal number,
        // the left-hand side.
        while (isDigit(peek())) nextChar();

        if (peek() == '.' && isDigit(peekNext()))
        {
            // Consume the decimal
            nextChar();

            // Consume the right hand side of the decimal
            while (isDigit(peek())) nextChar();
        }

        double literal = Double.parseDouble(getLexeme());
        addToken(TokenType.NUMBER, literal, startColumn);
    }

    /*
     * Indicates if the end of file has been reached. That is,
     * the character that is currently being processed is the last
     * character in the source program.
     * 
     * @return True if EOF; False otherwise.
     */
    private boolean isEndOfFile()
    {
        return position >= source.length();
    }

    /*
     * Returns the current lexeme being processed.
     *  
     * @return The lexeme being processed.
     */
    private String getLexeme()
    {
        return source.substring(lexemeStart, position);
    }

    /*
     * Adds a token to the accumulated list of tokens with the 
     * provided type.
     * 
     * @param type The type of the token
     * @returns The Token that was recently added
     */
    private Token addToken(TokenType type)
    {
        Token token = new Token(type, getLexeme(), null, currentLineNumber,
            currentColumnNumber); 
        
        tokens.add(token);
        
        return token;
    }

    /*
     * Adds a token to the accumulated list of tokens with the
     * provided type and column number.
     * 
     * @param type The type of the token
     * @param column The column number on the current line
     * where the token occurs
     * @returns The Token that was recently added
     */
    private Token addToken(TokenType type, int column)
    {
        Token token = new Token(type, getLexeme(), null, currentLineNumber,
            column);
        
        tokens.add(token);

        return token;
    }

    /*
     * Adds a token to the accumulated list of tokens with the 
     * provided type, literal, and column.
     * 
     * @param type The type of the token 
     * @param literal The literal value (used by the interpreter
     * for quick evaluation)
     * @param column The column number on the current line
     * where the token occurs
     * @returns The Token that was recently added
     */
    private Token addToken(TokenType type, Object literal, int column)
    {
        Token token = new Token(type, getLexeme(), literal, currentLineNumber,
            column);
        
        tokens.add(token);

        return token;
    }

    /*
     * Indicates if the provided character is a digit as
     * specified by the regular expression [0-9].
     * 
     * @param c A character
     * @return True if c is a digit; False otherwise.
     */
    private boolean isDigit(char c)
    {
        return c >= '0' && c <= '9';
    }

    /*
     * Indicates if the provided character is a white space
     * character.
     * 
     * @param c A character 
     * @return True if c is white space; False otherwise.
     */
    private boolean isWhitespace(char c)
    {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    /*
     * Indicates if the provided character is the start of
     * an identifier.
     * 
     * @param c A character from the source program.
     * @return True if the character is the start of an identifer;
     * False otherwise.
     */
    private boolean isStartOfIdentifier(char c)
    {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
            (c == '_') || (c == '$');  
    }

    /*
     * Indicates if the provided character is part
     * of an identifier.
     * 
     * @param c A character from the source program.
     * @return True if the character is part of an identifier;
     * False otherwise.
     */
    private boolean isPartOfIdentifier(char c)
    {
        return isStartOfIdentifier(c) || isDigit(c) || c == '?' || c == '-';
    }
}