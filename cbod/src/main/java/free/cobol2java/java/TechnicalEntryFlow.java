package free.cobol2java.java;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a generated COBOL program whose execute entry uses configured flow stages. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TechnicalEntryFlow { }
