package free.cobol2java.java;

/** Explicitly distinguishes a compile-time bound LINK target from a dynamic program name. */
public sealed interface CicsLinkTarget permits CicsLinkTarget.Resolved, CicsLinkTarget.Dynamic {
    String program();

    record Resolved(String program, Class<? extends IService> serviceType) implements CicsLinkTarget {
    }

    record Dynamic(String program) implements CicsLinkTarget {
    }
}
