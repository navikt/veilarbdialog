package no.nav.fo.veilarbdialog.kvp;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class KvpConfig {

    private final KvpService kvpService = mock(KvpService.class);

    @PostConstruct
    public void postConstruct() {
        when(kvpService.kontorsperreEnhetId(any())).thenReturn("KONTORSPERRE_ENHET_ID");
    }

    @Primary
    @Bean
    KvpService kvpService() {
        return kvpService;
    }

}
