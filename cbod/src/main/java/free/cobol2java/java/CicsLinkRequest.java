package free.cobol2java.java;

/** Typed Java COMMAREA passed across one explicitly classified CICS LINK boundary. */
public record CicsLinkRequest<T>(CicsLinkTarget target, T commarea, Integer length) {
}
