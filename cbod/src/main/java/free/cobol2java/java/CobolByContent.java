package free.cobol2java.java;

import free.cobol2java.java.redefines.AbstractCobolRedefines;
import free.cobol2java.java.redefines.CobolRedefinesBuffer;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.IdentityHashMap;

/** Copies a generated DATA object graph without sharing its mutable storage with the caller. */
public final class CobolByContent {
    private CobolByContent() {}

    @SuppressWarnings("unchecked")
    public static <T> T copy(T source) {
        return (T) copyValue(source, new IdentityHashMap<>());
    }

    private static Object copyValue(Object source, IdentityHashMap<Object, Object> copies) {
        if (source == null || isImmutable(source)) return source;
        Object existing = copies.get(source);
        if (existing != null) return existing;
        Class<?> type = source.getClass();
        if (type.isArray()) {
            int length = Array.getLength(source);
            Object target = Array.newInstance(type.getComponentType(), length);
            copies.put(source, target);
            if (type.getComponentType().isPrimitive()) {
                System.arraycopy(source, 0, target, 0, length);
            } else {
                for (int i = 0; i < length; i++) {
                    Array.set(target, i, copyValue(Array.get(source, i), copies));
                }
            }
            return target;
        }
        if (source instanceof CobolRedefinesBuffer buffer) {
            CobolRedefinesBuffer target = new CobolRedefinesBuffer((byte[]) copyValue(buffer.bytes(), copies));
            copies.put(source, target);
            return target;
        }
        try {
            Object target;
            if (source instanceof AbstractCobolRedefines<?>) {
                try {
                    target = type.getConstructor().newInstance();
                } catch (NoSuchMethodException noDefaultConstructor) {
                    // Built-in scalar views publish a length constructor; copy their full state below.
                    target = type.getConstructor(int.class).newInstance(
                            ((AbstractCobolRedefines<?>) source).getBytes().length);
                }
            } else {
                target = type.getConstructor().newInstance();
            }
            copies.put(source, target);
            for (Class<?> owner = type; owner != Object.class; owner = owner.getSuperclass()) {
                for (Field field : owner.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) continue;
                    if (field.isAnnotationPresent(CobolPointer.class)) {
                        field.setAccessible(true);
                        Object value = field.get(source);
                        field.set(target, copyPointerSlots(value, field.getType(), copies));
                        continue;
                    }
                    if (field.isSynthetic() || field.getType() == Object.class) {
                        throw new IllegalArgumentException("BY CONTENT requires explicit binding semantics for " + field);
                    }
                    field.setAccessible(true);
                    field.set(target, copyValue(field.get(source), copies));
                }
            }
            return target;
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Cannot copy BY CONTENT value of " + type.getName(), e);
        }
    }

    private static Object copyPointerSlots(Object value, Class<?> declaredType,
                                           IdentityHashMap<Object, Object> copies) {
        if (value == null || !declaredType.isArray()) return value;
        Object existing = copies.get(value);
        if (existing != null) return existing;
        int length = Array.getLength(value);
        Class<?> component = declaredType.getComponentType();
        Object target = Array.newInstance(component, length);
        copies.put(value, target);
        for (int i = 0; i < length; i++) {
            Array.set(target, i, copyPointerSlots(Array.get(value, i), component, copies));
        }
        return target;
    }

    private static boolean isImmutable(Object value) {
        return value instanceof String || value instanceof Integer || value instanceof Long
                || value instanceof Short || value instanceof Byte || value instanceof Float
                || value instanceof Double || value instanceof Boolean || value instanceof Character
                || value instanceof BigDecimal || value instanceof BigInteger || value instanceof Enum<?>
                || value instanceof Charset || value instanceof Class<?>;
    }
}
