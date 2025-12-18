package no.nav.fo.veilarbdialog.graphql;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component
class CustomExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    public GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        var error = GraphqlErrorBuilder.newError()
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation());
        if (ex instanceof ResponseStatusException responseException) {
            if (responseException.getStatusCode().value() == 403) error.errorType(ErrorType.FORBIDDEN).message("Ikke tilgang");
            else if (responseException.getStatusCode().value() == 401) error.errorType(ErrorType.FORBIDDEN).message("Ikke innlogget");
            else error.errorType(ErrorType.FORBIDDEN).message("Ikke tilgang");
            return error.build();
        } else {
            log.error("Uforventet feil på graphql endepunkt", ex);
            return error.errorType(ErrorType.INTERNAL_ERROR).message("Ukjent feil: " + ex.getMessage()).build();
        }
    }
}