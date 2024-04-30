package no.nav.fo.veilarbdialog.graphql;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.time.ZonedDateTime;

@Configuration
class GraphqlConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer()  {
        return builder -> builder
                .scalar(DateScalar.DATESCALAR)
                .scalar(ZonedDateTimeScalar.ZONED_DATE_TIME_SCALAR);
    }

}
