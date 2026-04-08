package free.cobol2java.cics;

public class CicsReceiveResult<I> {
    private final I data;
    private final int resp;
    private final int resp2;

    public CicsReceiveResult(I data, int resp, int resp2) {
        this.data = data;
        this.resp = resp;
        this.resp2 = resp2;
    }

    public I getData() {
        return data;
    }

    public int getResp() {
        return resp;
    }

    public int getResp2() {
        return resp2;
    }
}