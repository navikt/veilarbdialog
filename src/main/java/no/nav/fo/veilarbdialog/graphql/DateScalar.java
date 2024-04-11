package no.nav.fo.veilarbdialog.graphql;


import graphql.language.StringValue;
import graphql.schema.*;
import org.jetbrains.annotations.NotNull;

import java.time.*;
import java.util.Date;

class DateScalar {
    public static GraphQLScalarType DATESCALAR = GraphQLScalarType.newScalar()
        .name("Date")
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
        if (dataFetcherResult instanceof Date date) {
            return iso8601Fromdate(date, ZoneOffset.systemDefault());
        } else {
            throw new CoercingParseValueException("Failed to parse input in serializeDate");
        }
    }

    private static Date dateFromISO8601(String date) {
        Instant instant =  ZonedDateTime.parse(date).toInstant();
        return Date.from(instant);
    }
    private static String iso8601Fromdate(Date date, ZoneId zoneId) {
        return OffsetDateTime.ofInstant(date.toInstant(), zoneId).toString();
    }
}
