package no.nav.fo.veilarbdialog.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableScheduling
public class ApplicationConfig {

    @Bean
    public LockingTaskExecutor lockingTaskExecutor(DataSource ds) {
        return new DefaultLockingTaskExecutor(new JdbcTemplateLockProvider(ds));
    }

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }

    @Bean
    public XmlMapper xmlMapper() {

        XmlMapper mapper = new XmlMapper();
        mapper.registerModule(new JaxbAnnotationModule().setPriority(JaxbAnnotationModule.Priority.SECONDARY));
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;

    }

}
