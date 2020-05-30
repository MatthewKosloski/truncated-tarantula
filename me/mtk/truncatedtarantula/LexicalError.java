package me.mtk.truncatedtarantula;

public class LexicalError extends InterpreterError
{
    public LexicalError(Token token, String msg)
    {
        super(token, msg);
    }

    @Override
    public String getErrorName()
    {
        return "LexicalError";
    }
}