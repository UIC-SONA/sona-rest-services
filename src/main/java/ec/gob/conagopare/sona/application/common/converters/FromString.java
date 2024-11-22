package ec.gob.conagopare.sona.application.common.converters;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public record FromString<T>(Class<T> type, Function<String, T> parser) {

    public static final FromString<String> STRING = new FromString<>(String.class, Function.identity());
    public static final FromString<Integer> INTEGER = new FromString<>(Integer.class, Integer::parseInt);
    public static final FromString<Double> DOUBLE = new FromString<>(Double.class, Double::parseDouble);
    public static final FromString<java.util.UUID> UUID = new FromString<>(UUID.class, java.util.UUID::fromString);
    public static final FromString<Boolean> BOOLEAN = new FromString<>(Boolean.class, Boolean::parseBoolean);
    public static final FromString<Float> FLOAT = new FromString<>(Float.class, Float::parseFloat);
    public static final FromString<Short> SHORT = new FromString<>(Short.class, Short::parseShort);
    public static final FromString<Byte> BYTE = new FromString<>(Byte.class, Byte::parseByte);
    public static final FromString<Character> CHARACTER = new FromString<>(Character.class, s -> s.charAt(0));
    public static final FromString<BigDecimal> BIG_DECIMAL = new FromString<>(BigDecimal.class, BigDecimal::new);
    public static final FromString<BigInteger> BIG_INTEGER = new FromString<>(BigInteger.class, BigInteger::new);
    public static final FromString<Long> LONG = new FromString<>(Long.class, Long::parseLong);

    public static final FromString<Integer> PRIMITIVE_INTEGER = new FromString<>(int.class, Integer::parseInt);
    public static final FromString<Double> PRIMITIVE_DOUBLE = new FromString<>(double.class, Double::parseDouble);
    public static final FromString<Long> PRIMITIVE_LONG = new FromString<>(long.class, Long::parseLong);
    public static final FromString<Boolean> PRIMITIVE_BOOLEAN = new FromString<>(boolean.class, Boolean::parseBoolean);
    public static final FromString<Float> PRIMITIVE_FLOAT = new FromString<>(float.class, Float::parseFloat);
    public static final FromString<Short> PRIMITIVE_SHORT = new FromString<>(short.class, Short::parseShort);
    public static final FromString<Byte> PRIMITIVE_BYTE = new FromString<>(byte.class, Byte::parseByte);
    public static final FromString<Character> PRIMITIVE_CHARACTER = new FromString<>(char.class, s -> s.charAt(0));

    public static final List<FromString<?>> PRIMITIVES = List.of(
            PRIMITIVE_INTEGER,
            PRIMITIVE_DOUBLE,
            PRIMITIVE_LONG,
            PRIMITIVE_BOOLEAN,
            PRIMITIVE_FLOAT,
            PRIMITIVE_SHORT,
            PRIMITIVE_BYTE,
            PRIMITIVE_CHARACTER
    );

    public static final List<FromString<?>> NON_PRIMITIVES = List.of(
            STRING,
            INTEGER,
            DOUBLE,
            LONG,
            BOOLEAN,
            FLOAT,
            SHORT,
            BYTE,
            CHARACTER,
            BIG_DECIMAL,
            BIG_INTEGER,
            UUID
    );


    public static final List<FromString<?>> ALL = Stream.concat(PRIMITIVES.stream(), NON_PRIMITIVES.stream()).toList();

    private static Predicate<FromString<?>> match(Class<?> type) {
        return parser -> parser.type().equals(type);
    }


    public static boolean isSupported(Class<?> type) {
        return ALL.stream().anyMatch(match(type));
    }

    @SuppressWarnings("unchecked")
    public static <T> FromString<T> from(Class<T> type) {
        return (FromString<T>) ALL.stream()
                .filter(match(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported type: " + type));
    }

    public T parse(String value) {
        return parser.apply(value);
    }
}
