package no.nav.fo.veilarbdialog.feed;

import no.nav.fo.veilarbdialog.kvp.KvpService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.annotation.PostConstruct;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class KvpConfig {

    private final KvpService kvpService = mock(KvpService.class);

    @PostConstruct
    public void postConstruct() {
        when(kvpService.kontorsperreEnhetId(anyString())).thenReturn("KONTORSPERRE_ENHET_ID");
    }

    @Primary
    @Bean
    KvpService kvpService() {
        return kvpService;
    }

}
