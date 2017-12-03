package org.lattelang.idea.psi;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;

import static org.lattelang.idea.psi.LatteTokenTypes.*;

%%

%class _LatteLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

// white space
WHITE_SPACE = [\ \r\n\t\f]
// bool
BOOL=true | false | yes | no
// modifier
MODIFIER = "public" | "protected" | "private" | "internal"
         | "abstract" | "val" | "native" | "synchronized" | "transient" | "volatile" | "strictfp"
         | "data" | "var" | "def" | "nonnull" | "nonempty" | "implicit"
// number
__NUMBER_INT_PART = [1-9][0-9]*
__NUMBER_DIGIT_PART = \.[0-9]+
NUMBER = {__NUMBER_INT_PART} | {__NUMBER_INT_PART}{__NUMBER_DIGIT_PART}
// string
__STRING_SINGLE = \'
__STRING_DOUBLE = \"
__STRING_ESCAPE = \\n | \\r | \\t | \\b | \\f | "\\""\\" | \\\" | \\\'
__STRING_CONTENT = {__STRING_ESCAPE} | .
STRING = {__STRING_SINGLE}{__STRING_CONTENT}*{__STRING_SINGLE}
       | {__STRING_DOUBLE}{__STRING_CONTENT}*{__STRING_DOUBLE}
// key
__INVALID_KEY = "boolean"
__JAVA_KEY = "abstract" | "assert" | "boolean" | "break" | "byte" | "case"
           | "catch" | "char" | "class" | "const" | "continue" | "default"
           | "do" | "double" | "else" | "enum" | "extends" | "final" | "finally"
           | "float" | "for" | "if" | "implements" | "import" | "instanceof"
           | "int" | "interface" | "long" | "native" | "new" | "null" | "package"
           | "private" | "protected" | "public" | "return" | "short" | "static"
           | "strictfp" | "throw" | "try" | "while" | "void"

__LATTE_KEY = "is" | "not" | "bool" | "yes" | "no" | "type" | "as"
            | "in" | "elseif" | "package" | "import"
            | "break" | "continue" | "return" | "fun" | "require"
            | "new" | "object" | "implicit" | "match" | "case"
            | "annotation"
KEY = {__JAVA_KEY} | {__LATTE_KEY}
// symbol
__TWO_VAR_OP = ":::" | "^^" | "*" | "/" | "%" | "+" | "-"
             | "<<" | ">>" | ">>>" | ">" | "<" | ">=" | "<="
             | "==" | "!=" | "===" | "!==" | "is" | "not"
             | "in" | "&" | "^" | "|" | "&&", "and" | "||", "or"
             | ":="
__ONE_VAR_OP = "++" | "--" | "!" | "~" | "+" | "-"
__ASSIGN_OP = {__TWO_VAR_OP}"="
__DESTRUCTING = "<-"
__PATTERN_MATCHING = "=>"
    // braces
__BRACES = "(" | "{" | "[" | "]" | "}" | ")"
__OTHER_THINGS = "." | ":" | "::" | "=" | "@" | "..." | ":::" | ":=" | "#" | ","
SYMBOL = {__TWO_VAR_OP} | {__ONE_VAR_OP} | {__ASSIGN_OP} | {__DESTRUCTING} | {__PATTERN_MATCHING} | {__BRACES} | {__OTHER_THINGS}
// valid name
__VALID_STARTER = (\$|\_|[a-zA-Z])
__VALID_FOLLOW = {__VALID_STARTER}|[0-9]
VALID_NAME = {__VALID_STARTER}{__VALID_FOLLOW}*
// end
END = ";"
// comment
SINGLE_LINE_COMMENT = "//".*
MULTIPLE_LINE_COMMENT = "/*".*[\r\n]*.*"*/"

%%

{END}                                                       { yybegin(YYINITIAL); return END_MARK; }

{SINGLE_LINE_COMMENT} {MULTIPLE_LINE_COMMENT}               { return COMMENT;}

{BOOL}                                                      { return BOOL; }

{MODIFIER}                                                  { return MODIFIER; }

{NUMBER}                                                    { return NUMBER; }

{STRING}                                                    { return STRING; }

{__INVALID_KEY}                                             { return ERR; }
{KEY}                                                       { return KEY; }

{SYMBOL}                                                    { return SYMBOL; }

{VALID_NAME}                                                { return VALID_NAME; }

({WHITE_SPACE})+                                            { yybegin(YYINITIAL); return WHITE_SPACE; }

.                                                           { yybegin(YYINITIAL); return ERR; }
