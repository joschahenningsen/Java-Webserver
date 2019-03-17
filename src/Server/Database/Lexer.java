package Server.Database;

import java.io.*;

/**
 * Based on https://github.com/unnikkedga.
 * MIT licenced, big thanks!
 */
public class Lexer {
    private StreamTokenizer input;

    private int symbol = NONE;
    public static final int EOL = -3;
    public static final int EOF = -2;
    public static final int INVALID = -1;

    public static final int NONE = 0;

    public static final int OR = 1;
    public static final int AND = 2;
    public static final int NOT = 3;

    public static final int TRUE = 4;
    public static final int FALSE = 5;

    public static final int LEFT = 6;
    public static final int RIGHT = 7;

    public static final String TRUE_LITERAL = "true";
    public static final String FALSE_LITERAL = "false";

    public Lexer(InputStream in) {
        Reader r = new BufferedReader(new InputStreamReader(in));
        input = new StreamTokenizer(r);

        input.resetSyntax();
        input.wordChars('a', 'z');
        input.wordChars('A', 'Z');
        input.wordChars('\'', '\'');
        input.wordChars('0', '9');
        input.wordChars('.', '.');
        input.whitespaceChars('\u0000', ' ');
        input.whitespaceChars('\n', '\t');

        input.ordinaryChar('(');
        input.ordinaryChar(')');
        input.ordinaryChar('&');
        input.ordinaryChar('|');
        input.ordinaryChar('!');

        input.ordinaryChar('<');
        input.ordinaryChar('>');
        input.ordinaryChar('=');
    }

    public int nextSymbol() {
        try {
            switch (input.nextToken()) {
                case StreamTokenizer.TT_EOL:
                    symbol = EOL;
                    break;
                case StreamTokenizer.TT_EOF:
                    symbol = EOF;
                    break;
                case StreamTokenizer.TT_WORD: {
                    if (input.sval.equalsIgnoreCase(TRUE_LITERAL)) symbol = TRUE;
                    else if (input.sval.equalsIgnoreCase(FALSE_LITERAL)) symbol = FALSE;
                    break;
                }
                case '(':
                    symbol = LEFT;
                    break;
                case ')':
                    symbol = RIGHT;
                    break;
                case '&':
                    symbol = AND;
                    break;
                case '|':
                    symbol = OR;
                    break;
                case '!':
                    symbol = NOT;
                    break;
                default:
                    symbol = INVALID;
            }
        } catch (IOException e) {
            symbol = EOF;
        }

        return symbol;
    }

    public String toString() {
        String ret=input.toString();
        ret = ret.split(",")[0].substring(6);
        ret = ret.substring(0, ret.length()-1);
        return ret;
    }
}