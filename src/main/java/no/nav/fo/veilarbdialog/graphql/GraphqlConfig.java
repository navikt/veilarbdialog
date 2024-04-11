package no.nav.fo.veilarbdialog.graphql;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Configuration
class GraphqlConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer()  {
        return builder -> builder.scalar(DateScalar.DATESCALAR);
    }

}
