package free.cobol2java.java;

import free.cobol2java.java.redefines.CobolRedefinesBuffer;
import free.cobol2java.java.redefines.StringCobolRedefines;

/** Runtime contract used by existing generated non-01 COPY constructors. */
public final class StorageViewContractCheck {
    public static void main(String[] args) throws Exception {
        var buffer = new CobolRedefinesBuffer(16);
        var owner = new StringCobolRedefines(buffer, 4, 6);
        var start = owner.getClass().getMethod("storageStart");
        if ((Integer) start.invoke(owner) != 4) throw new AssertionError("Initial offset");
        var alias = new StringCobolRedefines(owner.storageBuffer(), (Integer) start.invoke(owner), 6);
        owner.set("ABCDEF");
        if (!alias.get().equals("ABCDEF")) throw new AssertionError("Shared view");
        owner.setRedefines(7, 3);
        if ((Integer) start.invoke(owner) != 7) throw new AssertionError("Updated offset");
        if (!owner.get().equals("DEF")) throw new AssertionError("Updated view");
        System.out.println("StorageViewContractCheck passed");
    }
}
