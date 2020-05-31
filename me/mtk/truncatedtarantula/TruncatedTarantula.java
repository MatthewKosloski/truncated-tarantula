package me.mtk.truncatedtarantula;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class TruncatedTarantula
{

    // An instance of an interpreter. Is static final
    // because it is to be reused to store programa state.
    static final Interpreter interpreter = new Interpreter();

    // Indicates if there is a known error
    // and prevents the execution of the code.
    static boolean hadError = false;

    // Indicates if a RuntimeError was thrown while
    // the Interpreter was evaluating the program.
    static boolean hadRuntimeError = false;

    // Indicates whether the interpreter is running
    // in interactive mode or non-interactive mode (with
    // a file).
    static boolean isInteractive = false;

    // The name of the file when running in 
    // non-interactive mode.
    static String filename;

    public static void main(String[] args) throws IOException
    {
        if (args.length > 1) 
        {
            System.out.println("Usage: truncatedtarantula [script]");
            System.exit(64);
        }
        else if (args.length == 1)
        {
            filename = args[0];
            runFile(args[0]);
        }
        else
        {
            isInteractive = true;
            runPrompt();
        }
    }

    /**
     * Scans the source program for tokens,
     * creates an AST from the tokens, and 
     * executes the AST.
     * 
     * @param source A source program writtin in the
     * language being interpreted.
     */
    private static void run(String source)
    {
        Lexer lexer = null;
        List<Token> tokens = null;

        try
        {
            lexer = new Lexer(source);
            tokens = lexer.getTokens();
        }
        catch (LexicalError err)
        {
            reportError(err, err.line, err.column);
            hadError = true;
            return;
        }

        try
        {
            Parser parser = new Parser(tokens);
            List<Expr> expressions = parser.parse();
            System.out.print("");
            interpreter.interpret(expressions);
        }
        catch (ParseError err)
        {
            String line = lexer.getLine(err.getToken().line);
            reportAndShowError(err, line);
            hadError = true;
        }
        catch (RuntimeError err)
        {
            String line = lexer.getLine(err.getToken().line);
            reportAndShowError(err, line);
            hadRuntimeError = true;
        }
    }

    /*
     * Reads and executes the file at the given path. 
     * 
     * @param path A path to a file.
     * @throws IOException 
     */
    private static void runFile(String path) throws IOException
    {
        // Read the input file and construct a String object
        // from the contents.
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        String source = new String(bytes, Charset.defaultCharset());

		run(source);
		
		// Indicate an error in the exit code.
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    /*
     * Runs the interpreter in interactive mode, allowing
     * the user to type source language into the console
     * and execute it directly.
     * 
     * @throws IOException
     */
    private static void runPrompt() throws IOException
    {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        while (true)
        {
            System.out.print("> ");               
			run(reader.readLine());
			hadError = false;
        }
    }

    /*
     * Reports an error to the standard error stream and 
     * shows the user the line at which the error is located.
     * 
     * @param err The type of error.
     * @param line The line 
     */
    private static void reportAndShowError(InterpreterError err, String line)
    {
        Token token = err.getToken();
        int lineNumber = token.line;
        int columnNumber = token.column;

        reportError(err, lineNumber, columnNumber);

        int offset = countLeadingWhitespace(line);

        // Print out line of code with the error
        System.out.format("\nLn %d, Col %d:\n", lineNumber, columnNumber);
        System.out.format("\t\"%s\"\n", line.trim());
        
        // Underline part of the line with the error
        String underline = "";
        for (int i = 0; i < columnNumber - offset; i++) underline += " ";
        
        for (int i = columnNumber; i < columnNumber + token.lexeme.length(); i++)
            underline += "^";
        System.out.format("\t%s\n", underline);
    }

    /*
     * Reports an error to the standard error stream.
     * 
     * @param err The type of error.
     * @param lineNumber lineNumber The line number of the error.
     * @param colNumber colNumber The column number of the error.
     */
    private static void reportError(InterpreterError err, int lineNumber,
        int colNumber)
    {
        String errorName = err.getErrorName();
        String message = err.getMessage();
        
        if (isInteractive)
            System.err.format("%s on column %d: %s\n", errorName, colNumber, 
            message);
        else
            System.err.format("%s [Ln %d, Col %d]: %s: %s\n", filename, 
            lineNumber, colNumber, errorName, message);
    }

    /*
     * Returns the number of leading white space characters
     * in the provided string.
     * 
     * @param str A string
     * @return The number of leading white space characters
     * in str.
     */
    private static int countLeadingWhitespace(String str)
    {
        int count = 0;
        while (Character.isWhitespace(str.charAt(count)))
            count++;
        
        return count;
    }
}