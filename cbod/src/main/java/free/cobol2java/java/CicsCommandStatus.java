package free.cobol2java.java;

/** Explicit CICS response, published before source response assignments and handlers. */
public record CicsCommandStatus(int resp, int resp2) {
    public void publish() {
        CicsRuntime.setStatus(resp, resp2);
    }

    public boolean dispatchHandle() {
        return CicsRuntime.dispatchHandle(resp);
    }
}
