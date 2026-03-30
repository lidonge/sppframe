package free.cobol2java.java;

/**
 * @author lidong@date 2024-10-12@version 1.0
 * Runtime helper for COBOL SEARCH / SEARCH ALL style traversal.
 */
public class Searcher {
    /**
     * Moves the index one element at a time and evaluates the compare() result
     * against the requested relational operator.
     * The current implementation uses sequential scanning and sets the index
     * to the array end when no match is found.
     */
    public static boolean search(Object[] target, ISearchIndex index, ISearchComparable comparable, RelationalOperator relationalOperator) {
        if (target == null || index == null || comparable == null || relationalOperator == null) {
            return false;
        }

        for (int i = 0; i < target.length; i++) {
            index.setIndex(i);
            if (matches(comparable.compare(), relationalOperator)) {
                return true;
            }
        }

        index.setIndex(target.length);
        return false;
    }

    /**
     * Moves the index one element at a time until all conditions match.
     * This fits the runtime behavior of a regular SEARCH ... WHEN ... evaluation.
     */
    public static boolean search(Object[] target, ISearchIndex index, ISearchCondition... condition) {
        if (target == null || index == null) {
            return false;
        }

        for (int i = 0; i < target.length; i++) {
            index.setIndex(i);
            if (allMatch(condition)) {
                return true;
            }
        }

        index.setIndex(target.length);
        return false;
    }

    /**
     * Conditions are combined with AND semantics.
     */
    private static boolean allMatch(ISearchCondition... conditions) {
        if (conditions == null || conditions.length == 0) {
            return false;
        }

        for (ISearchCondition condition : conditions) {
            if (condition == null || !condition.isMatch()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Maps the integer result of compare() to the COBOL relational operator.
     */
    private static boolean matches(int compareResult, RelationalOperator relationalOperator) {
        return switch (relationalOperator) {
            case GREATER, MORETHANCHAR -> compareResult > 0;
            case GREATER_OR_EQUAL, MORETHANOREQUAL -> compareResult >= 0;
            case LESS, LESSTHANCHAR -> compareResult < 0;
            case LESS_OR_EQUAL, LESSTHANOREQUAL -> compareResult <= 0;
            case EQUAL, EQUALCHAR -> compareResult == 0;
            case NOT_EQUAL, NOTEQUALCHAR -> compareResult != 0;
        };
    }
}
