package free.cobol2java.java;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class CobolReference<T> {
    private final Supplier<T> getter;
    private final Consumer<T> setter;

    private CobolReference(Supplier<T> getter, Consumer<T> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    public static <T> CobolReference<T> of(Supplier<T> getter, Consumer<T> setter) {
        return new CobolReference<>(getter, setter);
    }

    public T get() {
        return getter == null ? null : getter.get();
    }

    public void set(T value) {
        if (setter != null) {
            setter.accept(value);
        }
    }
}
