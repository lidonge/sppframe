package free.cobol2java.java;

import java.lang.reflect.Field;

/**
 * SQL host-variable adapters used by generated embedded SQL code.
 */
public final class SqlHostVar {
    private SqlHostVar() {
    }

    public static void assignVarchar(Object value, Object hostGroup) {
        VarcharFields fields = VarcharFields.of(hostGroup);
        if (fields == null) {
            throw new IllegalArgumentException("Not a COBOL SQL VARCHAR host group: "
                    + (hostGroup == null ? "null" : hostGroup.getClass().getName()));
        }
        String text = value == null ? "" : Util.copyString(value);
        int maxLength = fields.textLength();
        String fixedText = padRight(text.length() > maxLength ? text.substring(0, maxLength) : text, maxLength);
        try {
            fields.lengthField().set(hostGroup, numericValue(fields.lengthField().getType(), Math.min(text.length(), maxLength)));
            fields.textField().set(hostGroup, fixedText);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Failed to assign COBOL SQL VARCHAR host group", ex);
        }
    }

    public static String toVarchar(Object hostGroup) {
        if (hostGroup == null) {
            return null;
        }
        VarcharFields fields = VarcharFields.of(hostGroup);
        if (fields == null) {
            throw new IllegalArgumentException("Not a COBOL SQL VARCHAR host group: "
                    + hostGroup.getClass().getName());
        }
        try {
            Object lengthValue = fields.lengthField().get(hostGroup);
            Object textValue = fields.textField().get(hostGroup);
            String text = textValue == null ? "" : Util.copyString(textValue);
            int length = lengthValue instanceof Number number ? number.intValue() : text.length();
            length = Math.max(0, Math.min(length, Math.min(fields.textLength(), text.length())));
            return text.substring(0, length);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Failed to read COBOL SQL VARCHAR host group", ex);
        }
    }

    private static String padRight(String value, int length) {
        String text = value == null ? "" : value;
        if (text.length() >= length) {
            return text;
        }
        StringBuilder builder = new StringBuilder(length);
        builder.append(text);
        while (builder.length() < length) {
            builder.append(' ');
        }
        return builder.toString();
    }

    private static Object numericValue(Class<?> type, int value) {
        if (Integer.class.equals(type) || Integer.TYPE.equals(type)) {
            return value;
        }
        if (Short.class.equals(type) || Short.TYPE.equals(type)) {
            return (short) value;
        }
        if (Long.class.equals(type) || Long.TYPE.equals(type)) {
            return (long) value;
        }
        if (Byte.class.equals(type) || Byte.TYPE.equals(type)) {
            return (byte) value;
        }
        throw new IllegalArgumentException("Unsupported COBOL SQL VARCHAR length field type: " + type.getName());
    }

    private record VarcharFields(Field lengthField, Field textField, int textLength) {
        private static VarcharFields of(Object hostGroup) {
            if (hostGroup == null) {
                return null;
            }
            Field lengthField = null;
            Field textField = null;
            int textLength = -1;
            for (Field field : hostGroup.getClass().getFields()) {
                FieldInfo info = field.getAnnotation(FieldInfo.class);
                if (info == null) {
                    continue;
                }
                String cobolType = info.cobolType();
                if (lengthField == null
                        && Number.class.isAssignableFrom(field.getType())
                        && info.levelNumber() == 49
                        && "S9(4)".equalsIgnoreCase(cobolType)
                        && "COMP".equalsIgnoreCase(info.usageType())) {
                    lengthField = field;
                    continue;
                }
                if (textField == null
                        && String.class.equals(field.getType())
                        && info.levelNumber() == 49
                        && cobolType != null
                        && cobolType.matches("(?i)X\\(\\d+\\)")) {
                    textField = field;
                    textLength = Math.max(info.precision(), 0);
                }
            }
            if (lengthField == null || textField == null || textLength < 0) {
                return null;
            }
            return new VarcharFields(lengthField, textField, textLength);
        }
    }
}
