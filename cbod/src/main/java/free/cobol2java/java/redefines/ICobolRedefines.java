package free.cobol2java.java.redefines;

/**
 * @author lidong@date 2024-09-04@version 1.0
 */
public interface ICobolRedefines<T> {
    void setRedefines(int start,int length);
    T get();
    void set(T value);
    byte[] getBytes();
}