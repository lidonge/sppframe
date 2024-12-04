package free.servpp.sppframe.common;

/**
 * @author lidong@date 2024-08-09@version 1.0
 */
public interface IBlock {
    public enum BlockType{
        Serial,Parallel,Transaction
    }
    BlockType getType();
    String getName();
}
