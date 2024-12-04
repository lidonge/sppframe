package free.servpp.sppframe.common;

/**
 * @author lidong@date 2024-08-08@version 1.0
 */
public interface IAtomicAfter {
    void process(ISppContext context, Object[] args, Throwable throwable, ISppException error);
}
