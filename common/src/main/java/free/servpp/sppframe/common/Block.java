package free.servpp.sppframe.common;

import free.servpp.sppframe.common.IBlock;

/**
 * @author lidong@date 2024-08-09@version 1.0
 */
public class Block implements IBlock {
    private BlockType type;
    private String name;

    public Block(BlockType type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public BlockType getType() {
        return type;
    }

    @Override
    public String getName() {
        return name;
    }
}
