package free.cobol2java.java;

import java.util.Objects;

/** Source-derived CICS file identity and optional static repository type. */
public record CicsBrowseResource(Object name, Class<?> repositoryType) {
    public CicsBrowseResource {
        Objects.requireNonNull(name, "name");
    }
}
