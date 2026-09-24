package free.cobol2java.java;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Per-generated-program opaque state for replaceable data-access adapters. */
public final class AccessContext {
    private final Map<Class<?>, Object> adapterStates = new HashMap<>();

    public synchronized <T> T state(Class<?> adapterType, Class<T> stateType, Supplier<T> factory) {
        Object value = adapterStates.computeIfAbsent(adapterType, ignored -> stateType.cast(factory.get()));
        return stateType.cast(value);
    }
}
