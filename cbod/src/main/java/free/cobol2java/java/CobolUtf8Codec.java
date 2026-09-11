package free.cobol2java.java;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * UTF-8 byte projections over one lossless Unicode String state.
 * Valid text remains ordinary Unicode. An undecodable byte is represented by an
 * unpaired low surrogate U+DC00 + byte; paired surrogates always remain Unicode.
 * Escapes are private state, never visible read values or user write values.
 */
public final class CobolUtf8Codec {
    private CobolUtf8Codec() { }

    public static String decodeState(byte[] encoded) {
        Objects.requireNonNull(encoded, "encoded");
        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        var input = ByteBuffer.wrap(encoded);
        var output = CharBuffer.allocate(Math.max(1, encoded.length));
        var state = new StringBuilder(encoded.length);
        while (true) {
            var result = decoder.decode(input, output, true);
            output.flip();
            state.append(output);
            output.clear();
            if (result.isUnderflow()) return state.toString();
            if (!result.isError()) throw new IllegalStateException("UTF-8 output capacity invariant");
            for (int index = 0; index < result.length(); index++) {
                state.append((char) (0xdc00 + (input.get() & 255)));
            }
        }
    }

    public static byte[] encodeState(String state) {
        Objects.requireNonNull(state, "state");
        var bytes = new ByteArrayOutputStream(state.length());
        int start = 0;
        for (int index = 0; index < state.length(); index++) {
            char character = state.charAt(index);
            if (Character.isHighSurrogate(character)) {
                if (index + 1 >= state.length() || !Character.isLowSurrogate(state.charAt(index + 1))) {
                    throw new IllegalArgumentException("Unpaired high surrogate in UTF-8 state");
                }
                index++;
            } else if (character >= 0xdc00 && character <= 0xdcff) {
                bytes.writeBytes(state.substring(start, index).getBytes(StandardCharsets.UTF_8));
                bytes.write(character - 0xdc00);
                start = index + 1;
            } else if (Character.isLowSurrogate(character)) {
                throw new IllegalArgumentException("Unrecognized surrogate escape in UTF-8 state");
            }
        }
        bytes.writeBytes(state.substring(start).getBytes(StandardCharsets.UTF_8));
        return bytes.toByteArray();
    }

    public static String read(String state, int offset, int width) {
        Objects.requireNonNull(state, "state");
        if (ascii(state)) {
            Objects.checkFromIndexSize(offset, width, state.length());
            return state.substring(offset, offset + width);
        }
        byte[] bytes = encodeState(state);
        Objects.checkFromIndexSize(offset, width, bytes.length);
        return new String(bytes, offset, width, StandardCharsets.UTF_8);
    }

    public static String write(String state, String value, int offset, int width) {
        Objects.requireNonNull(state, "state");
        String text = value == null ? "" : value;
        if (ascii(state) && ascii(text)) {
            Objects.checkFromIndexSize(offset, width, state.length());
            String field = text.length() >= width ? text.substring(0, width)
                    : text + " ".repeat(width - text.length());
            return state.substring(0, offset) + field + state.substring(offset + width);
        }
        byte[] bytes = encodeState(state);
        Objects.checkFromIndexSize(offset, width, bytes.length);
        byte[] source = text.getBytes(StandardCharsets.UTF_8);
        Arrays.fill(bytes, offset, offset + width, (byte) ' ');
        System.arraycopy(source, 0, bytes, offset, Math.min(source.length, width));
        return decodeState(bytes);
    }

    /** Internal byte slice: unlike read, malformed bytes remain lossless state. */
    public static String sliceState(String state, int offset, int width) {
        Objects.requireNonNull(state, "state");
        if (ascii(state)) {
            Objects.checkFromIndexSize(offset, width, state.length());
            return state.substring(offset, offset + width);
        }
        byte[] bytes = encodeState(state);
        Objects.checkFromIndexSize(offset, width, bytes.length);
        return decodeState(Arrays.copyOfRange(bytes, offset, offset + width));
    }

    /** Internal exact-width copy; caller must perform public value padding separately. */
    public static String writeState(String state, String replacement, int offset, int width) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(replacement, "replacement");
        if (ascii(state) && ascii(replacement)) {
            Objects.checkFromIndexSize(offset, width, state.length());
            if (replacement.length() != width) throw new IllegalArgumentException("State width mismatch");
            return state.substring(0, offset) + replacement + state.substring(offset + width);
        }
        byte[] bytes = encodeState(state);
        Objects.checkFromIndexSize(offset, width, bytes.length);
        byte[] source = encodeState(replacement);
        if (source.length != width) throw new IllegalArgumentException("State width mismatch");
        System.arraycopy(source, 0, bytes, offset, width);
        return decodeState(bytes);
    }

    private static boolean ascii(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) > 127) return false;
        }
        return true;
    }
}
