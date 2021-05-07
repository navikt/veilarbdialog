package no.nav.fo.veilarbdialog.util;

import java.util.Arrays;

public class EnumUtils {

    private EnumUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String getName(Enum<?> anEnum) {
        return anEnum != null ? anEnum.name() : null;
    }

    public static <T extends Enum<?>> T valueOf(Class<T> enumClass, String name) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> e.name().equals(name))
                .findAny()
                .orElse(null);
    }

}
