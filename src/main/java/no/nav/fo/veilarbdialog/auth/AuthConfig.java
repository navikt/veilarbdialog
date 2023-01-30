package no.nav.fo.veilarbdialog.auth;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.abac.Pep;
import no.nav.common.abac.VeilarbPepFactory;
import no.nav.common.abac.audit.AuditLogFilterUtils;
import no.nav.common.abac.audit.SpringAuditRequestInfoSupplier;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EksternBrukerId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.utils.Credentials;
import no.nav.poao.dab.spring_auth.AuthService;
import no.nav.poao.dab.spring_auth.IAuthService;
import no.nav.poao.dab.spring_auth.IPersonService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static no.nav.common.abac.audit.AuditLogFilterUtils.not;

@Configuration
@Slf4j
@Profile("!local")
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

    @Bean
    IAuthService authService(AuthContextHolder authcontextHolder, Pep pep, AktorOppslagClient aktorOppslagClient) {
        var personService = new IPersonService() {
            @NotNull
            @Override
            public Fnr getFnrForAktorId(@NotNull EksternBrukerId eksternBrukerId) {
                if (eksternBrukerId instanceof Fnr fnr) return fnr;
                if (eksternBrukerId instanceof AktorId aktorId) return aktorOppslagClient.hentFnr(aktorId);
                throw new IllegalStateException("Kan bare hente fnr for AktorId eller Fnr");
            }

            @NotNull
            @Override
            public AktorId getAktorIdForPersonBruker(@NotNull EksternBrukerId eksternBrukerId) {
                if (eksternBrukerId instanceof AktorId aktorId) return aktorId;
                if (eksternBrukerId instanceof Fnr fnr) return aktorOppslagClient.hentAktorId(fnr);
                throw new IllegalStateException("Kan bare hente aktorId for AktorId eller Fnr");
            }
        };
        return new AuthService(authcontextHolder, pep, personService);
    }
}
