package no.nav.fo.veilarbdialog.auth;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.abac.Pep;
import no.nav.common.abac.VeilarbPepFactory;
import no.nav.common.abac.audit.AuditLogFilterUtils;
import no.nav.common.abac.audit.SpringAuditRequestInfoSupplier;
import no.nav.common.utils.Credentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.common.abac.audit.AuditLogFilterUtils.not;

@Configuration
@Slf4j
public class AuthConfig {

    @Value("${application.abac.url}")
    private String abacUrl;

    @Bean
    Pep pep(Credentials systemUser) {
        return pep(abacUrl, systemUser);
    }

    Pep pep(String abacUrl, Credentials systemUser) {
        log.info("Using configured ABAC URL {}", abacUrl);
        return VeilarbPepFactory.get(
                abacUrl,
                systemUser.username,
                systemUser.password,
                new SpringAuditRequestInfoSupplier(),
                not(AuditLogFilterUtils.pathFilter(path -> path.endsWith("/api/dialog/sistOppdatert")))
        );
    }
}
