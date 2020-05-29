package me.mtk.truncatedtarantula;

import java.util.HashMap;
import java.util.Map;

// Implements the lexical scope of the let
// expression.
public class Scope 
{
    private final Scope parent;
    private final Map<String, Object> values = new HashMap<>();

    public Scope(Scope parent)
    {
        this.parent = parent;
    }

    /**
     * Binds a new value to an identifier. If the identifier
     * already exists in the scope, then its value is redefined
     * to the provided value.
     * 
     * @param identifier An indentifier.
     * @param value The value that will be bounded
     * to the identifier.
     */
    public void define(String identifier, Object value)
    {
        values.put(identifier, value);
    }

    /**
     * Looks up the value in the scope that is bounded to the 
     * provided identifier token.
     * 
     * @param identifier The identifier token whose value is to 
     * be returned.
     * @return The value bounded to the provided identifier.
     * @throws RuntimeError If the identifier does not exist
     * in the scope.
     */
    public Object get(Token identifier) 
    {
        if (values.containsKey(identifier.lexeme))
            // The identifier is defined within the local scope
            return values.get(identifier.lexeme);
        else if (parent != null)
        {
            // Traverse up the scope chain to find the identifier
            return parent.get(identifier);
        }
        else
        {
            // The identifier is not defined anywhere
            throw new RuntimeError(identifier, 
                String.format("Undefined identifier '%s'", identifier.lexeme));
        }
    }
}