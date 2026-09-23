package free.cobol2java.java;

/** The same Java COMMAREA after LINK, together with the CICS response status. */
public record CicsLinkResult<T>(T commarea, int resp, int resp2) {
}
