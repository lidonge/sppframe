package free.cobol2java.java;

/**
 * @author lidong@date 2024-10-12@version 1.0
 * Comparison callback used by Searcher when evaluating relational operators.
 */
public interface ISearchComparable {
    /**
     * Returns a comparison result using the same convention as Comparable:
     * negative for less than, zero for equal, positive for greater than.
     */
    int compare();
}
