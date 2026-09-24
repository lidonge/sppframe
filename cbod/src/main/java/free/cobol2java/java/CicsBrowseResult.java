package free.cobol2java.java;

/** Business-facing CICS browse result without exposing CicsRuntime.Response. */
public record CicsBrowseResult<T>(T value, int resp, int resp2) {
}
