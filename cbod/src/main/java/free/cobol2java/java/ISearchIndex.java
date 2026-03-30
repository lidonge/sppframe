package free.cobol2java.java;

/**
 * @author lidong@date 2024-10-12@version 1.0
 * Receives the current index during SEARCH traversal.
 */
public interface ISearchIndex {
    /**
     * Updates the caller-visible index to the current search position.
     */
    void setIndex(int index);
}
