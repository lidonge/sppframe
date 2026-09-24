package free.cobol2java.java;

import java.util.Objects;

/** Source-derived resource plus an optional statically known repository type. */
public record DataAccessResource(Object name, Class<?> repositoryType) {
    public DataAccessResource {
        Objects.requireNonNull(name, "name");
    }
}
