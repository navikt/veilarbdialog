package no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class KvitteringMetrikk {
    private final MeterRegistry meterRegistry;
    private static final String BRUKERNOTIFIKASJON_KVITTERING_MOTTATT = "brukernotifikasjon_kvittering_mottatt";
    private static final String BRUKERNOTIFIKASJON_MANGLER_KVITTERING = "brukernotifikasjon_mangler_kvittering";
    private static final String STATUS = "status";
    private static final List<String> statuser = List.of(
            EksternVarslingKvitteringConsumer.FEILET,
            EksternVarslingKvitteringConsumer.FERDIGSTILT,
            EksternVarslingKvitteringConsumer.INFO,
            EksternVarslingKvitteringConsumer.OVERSENDT
    );

    private final AtomicInteger forsinkedeBestillinger = new AtomicInteger();

    public KvitteringMetrikk(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        statuser.forEach(s -> meterRegistry.counter(BRUKERNOTIFIKASJON_KVITTERING_MOTTATT, KvitteringMetrikk.STATUS, s));
        Gauge.builder(BRUKERNOTIFIKASJON_MANGLER_KVITTERING, forsinkedeBestillinger, AtomicInteger::doubleValue).register(meterRegistry);
    }

    public void incrementBrukernotifikasjonKvitteringMottatt(String status) {
        Counter.builder(BRUKERNOTIFIKASJON_KVITTERING_MOTTATT)
                .tag(KvitteringMetrikk.STATUS, status)
                .register(meterRegistry)
                .increment();
    }

    public void countForsinkedeVarslerSisteDognet(int antall) {
        forsinkedeBestillinger.setPlain(antall);
    }
}
