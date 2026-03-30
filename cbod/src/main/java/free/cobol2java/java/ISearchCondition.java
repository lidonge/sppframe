package free.cobol2java.java;

/**
 * @author lidong@date 2024-10-12@version 1.0
 * Predicate used by Searcher for SEARCH ... WHEN ... style evaluation.
 */
public interface ISearchCondition {
    /**
     * Returns true when the current indexed element satisfies this condition.
     */
    boolean isMatch();
}
