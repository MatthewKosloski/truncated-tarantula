package me.mtk.truncatedtarantula;

public class InterpreterError extends RuntimeException
{
    private Token token;

    public InterpreterError(Token token, String message)
    {
        super(message);
        this.token = token;
    }

    public Token getToken()
    {
        return this.token;
    }

    public String getErrorName()
    {
        return "Error";
    }
}