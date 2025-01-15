package free.cobol2java.java;

/**
 * @author lidong@date 2024-10-10@version 1.0
 * inspect statement
 *
 * inspectStatement
 *    : INSPECT identifier (inspectTallyingPhrase | inspectReplacingPhrase | inspectTallyingReplacingPhrase | inspectConvertingPhrase)
 *    ;
 *
 * inspectTallyingPhrase
 *    : TALLYING inspectFor+
 *    ;
 *
 * inspectReplacingPhrase
 *    : REPLACING (inspectReplacingCharacters | inspectReplacingAllLeadings)+
 *    ;
 *
 * inspectTallyingReplacingPhrase
 *    : TALLYING inspectFor+ inspectReplacingPhrase+
 *    ;
 *
 * inspectConvertingPhrase
 *    : CONVERTING (identifier | literal) inspectTo inspectBeforeAfter*
 *    ;
 *
 * inspectFor
 *    : identifier FOR (inspectCharacters | inspectAllLeadings)+
 *    ;
 *
 * inspectCharacters
 *    : (CHARACTER | CHARACTERS) inspectBeforeAfter*
 *    ;
 *
 * inspectReplacingCharacters
 *    : (CHARACTER | CHARACTERS) inspectBy inspectBeforeAfter*
 *    ;
 *
 * inspectAllLeadings
 *    : (ALL | LEADING) inspectAllLeading+
 *    ;
 *
 * inspectReplacingAllLeadings
 *    : (ALL | LEADING | FIRST) inspectReplacingAllLeading+
 *    ;
 *
 * inspectAllLeading
 *    : (identifier | literal) inspectBeforeAfter*
 *    ;
 *
 * inspectReplacingAllLeading
 *    : (identifier | literal) inspectBy inspectBeforeAfter*
 *    ;
 *
 * inspectBy
 *    : BY (identifier | literal)
 *    ;
 *
 * inspectTo
 *    : TO (identifier | literal)
 *    ;
 *
 * inspectBeforeAfter
 *    : (BEFORE | AFTER) INITIAL? (identifier | literal)
 *    ;
 */
public class Inspector {
    public enum LeadType{
        ALL , LEADING , FIRST
    }

    public static int tallyingFor(String src, LeadType leadType, TallyingTarget... targets){
        return 0;
    }

    public static String replacingBy(String src,LeadType leadType, ReplaceStruct struct){
        return null;
    }
}
