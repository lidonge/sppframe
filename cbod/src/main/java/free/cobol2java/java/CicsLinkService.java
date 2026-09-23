package free.cobol2java.java;

/** Replaceable typed boundary for CICS LINK operations. */
public class CicsLinkService implements IService {
    public <T> CicsLinkResult<T> link(CicsLinkRequest<T> request) {
        CicsRuntime.Response<?> response;
        if (request.target() instanceof CicsLinkTarget.Resolved resolved) {
            response = CicsRuntime.link(resolved.program(), resolved.serviceType(),
                    request.commarea(), request.length());
        } else if (request.target() instanceof CicsLinkTarget.Dynamic dynamic) {
            response = CicsRuntime.link(dynamic.program(), request.commarea(), request.length());
        } else {
            throw new IllegalStateException("Unsupported CICS LINK target contract: " + request.target());
        }
        return new CicsLinkResult<>(request.commarea(), response.resp(), response.resp2());
    }
}
