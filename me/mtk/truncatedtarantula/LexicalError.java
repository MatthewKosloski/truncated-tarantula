package me.mtk.truncatedtarantula;

public class LexicalError extends InterpreterError
{

    final int line;
    final int column;

    public LexicalError(String msg, int line, int column)
    {
        super(null, msg);
        this.line = line;
        this.column = column;
    }

    @Override
    public String getErrorName()
    {
        return "LexicalError";
    }
}