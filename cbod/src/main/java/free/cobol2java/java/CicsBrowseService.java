package free.cobol2java.java;

import free.cobol2java.cics.CicsCrudRepository;
import org.springframework.stereotype.Component;

/** Replaceable typed service boundary for CICS file browse commands. */
@Component
public class CicsBrowseService implements DataAccessService {
    @Override
    public <K> void begin(AccessContext context, DataAccessResource resource, K key, boolean gteq,
            boolean equal, boolean generic, Integer keyLength, AccessStatusSink status) {
        var response = start(browseContext(context), browseResource(resource), key,
                gteq, equal, generic, keyLength);
        status.publish(response.resp(), response.resp2());
    }

    @Override
    public <R, K> K next(AccessContext context, DataAccessResource resource, R into, K key,
            Integer length, AccessStatusSink status) {
        var response = readNext(browseContext(context), browseResource(resource), into, key, length);
        status.publish(response.resp(), response.resp2());
        return response.value();
    }

    @Override
    public <R, K> K previous(AccessContext context, DataAccessResource resource, R into, K key,
            Integer length, AccessStatusSink status) {
        var response = readPrev(browseContext(context), browseResource(resource), into, key, length);
        status.publish(response.resp(), response.resp2());
        return response.value();
    }

    @Override
    public void end(AccessContext context, DataAccessResource resource, AccessStatusSink status) {
        var response = end(browseContext(context), browseResource(resource));
        status.publish(response.resp(), response.resp2());
    }

    @Override
    public boolean dispatchControlFlow(AccessStatusSink status) {
        return CicsRuntime.dispatchHandle(status.status());
    }

    private CicsBrowseContext browseContext(AccessContext context) {
        return context.state(CicsBrowseService.class, CicsBrowseContext.class, CicsBrowseContext::new);
    }

    private CicsBrowseResource browseResource(DataAccessResource resource) {
        return new CicsBrowseResource(resource.name(), resource.repositoryType());
    }

    public <K> CicsBrowseResult<K> start(CicsBrowseContext context, CicsBrowseResource resource, K ridfld,
            boolean gteq, boolean equal, boolean generic, Integer keyLength) {
        var response = CicsBrowseUtil.startBrowse(context, resource.name().toString(), repository(resource), ridfld,
                gteq, equal, generic, keyLength);
        return new CicsBrowseResult<>(null, response.resp(), response.resp2());
    }

    public <R, K> CicsBrowseResult<K> readNext(CicsBrowseContext context, CicsBrowseResource resource, R into,
            K ridfld, Integer length) {
        var response = CicsBrowseUtil.readNext(context, resource.name().toString(), repository(resource), into, ridfld, length);
        return new CicsBrowseResult<>((K) response.value(), response.resp(), response.resp2());
    }

    public <R, K> CicsBrowseResult<K> readPrev(CicsBrowseContext context, CicsBrowseResource resource, R into,
            K ridfld, Integer length) {
        var response = CicsBrowseUtil.readPrev(context, resource.name().toString(), repository(resource), into, ridfld, length);
        return new CicsBrowseResult<>((K) response.value(), response.resp(), response.resp2());
    }

    public CicsBrowseResult<Void> end(CicsBrowseContext context, CicsBrowseResource resource) {
        var response = CicsBrowseUtil.endBrowse(context, resource.name().toString(), repository(resource));
        return new CicsBrowseResult<>(null, response.resp(), response.resp2());
    }

    @SuppressWarnings("unchecked")
    private CicsCrudRepository<Object, Object> repository(CicsBrowseResource resource) {
        return resource.repositoryType() == null
                ? ServiceManager.repositoryByName(resource.name().toString())
                : ServiceManager.repositoryByType(resource.repositoryType());
    }
}
