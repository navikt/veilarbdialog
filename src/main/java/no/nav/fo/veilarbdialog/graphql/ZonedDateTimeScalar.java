package no.nav.fo.veilarbdialog.graphql;

import graphql.language.StringValue;
import graphql.schema.*;
import org.jetbrains.annotations.NotNull;

import java.time.*;

public class ZonedDateTimeScalar {
    public static GraphQLScalarType ZONED_DATE_TIME_SCALAR = GraphQLScalarType.newScalar()
            .name("ZonedDateTime")
            .description("A custom scalar that handles zonedDateTime")
            .coercing(new Coercing<Object, String>() {
                @Override
                public String serialize(@NotNull Object dataFetcherResult) throws CoercingSerializeException {
                    return serializeDate(dataFetcherResult);
                }
                @Override
                public @NotNull Object parseValue(@NotNull Object input) throws CoercingParseValueException {
                    return parseDateFromVariable(input);
                }
                @Override
                public @NotNull Object parseLiteral(@NotNull Object input) throws CoercingParseLiteralException {
                    return parseDateFromAstLiteral(input);
                }
            }).build();

    private static Object parseDateFromAstLiteral(Object input) {
        if (input instanceof StringValue stringValue) {
            return dateFromISO8601(stringValue.getValue());
        } else {
            throw new CoercingParseValueException("Failed to parse input in parseZonedDateFromAstLiteral");
        }
    }
    private static Object parseDateFromVariable(Object input) {
        if (input instanceof String stringValue) {
            return dateFromISO8601(stringValue);
        } else {
            throw new CoercingParseValueException("Failed to parse input in parseZonedDateFromVariable");
        }
    }
    private static String serializeDate(Object dataFetcherResult) {
        if (dataFetcherResult instanceof ZonedDateTime date) {
            return  iso8601Fromdate(date, ZoneOffset.systemDefault());
        } else {
            throw new CoercingParseValueException("Failed to parse input in serializeDate");
        }
    }

    private static ZonedDateTime dateFromISO8601(String date) {
        return  ZonedDateTime.parse(date);
    }
    private static String iso8601Fromdate(ZonedDateTime date, ZoneId zoneId) {
        return date.toOffsetDateTime().toString();
    }
}


