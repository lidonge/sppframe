package free.cobol2java.cics;

public class CicsSendResult {
    private final int resp;
    private final int resp2;

    public CicsSendResult(int resp, int resp2) {
        this.resp = resp;
        this.resp2 = resp2;
    }

    public int getResp() {
        return resp;
    }

    public int getResp2() {
        return resp2;
    }
}