package free.cobol2java.java;

/** Per-program identity of an implicit CICS EIB address supplied through LINKAGE. */
public final class CicsEibAddressBinding {
    private Object address;

    public void bind(Object sourceAddress) {
        address = sourceAddress;
    }

    public Object address() {
        return address;
    }
}
