package no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.fo.veilarbdialog.minsidevarsler.dto.VarselHendelseEventType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class KvitteringMetrikk {
    private final MeterRegistry meterRegistry;
    private static final String BRUKERNOTIFIKASJON_MANGLER_KVITTERING = "brukernotifikasjon_mangler_kvittering";
    private static final String VARSEL_HENDELSE = "varsel_hendelse";
    private static final String HENDELSE_TYPE = "hendelse_type";
    private static final List<String> hendelseTyper = List.of(
        VarselHendelseEventType.feilet_ekstern.name(),
        VarselHendelseEventType.bestilt_ekstern.name(),
        VarselHendelseEventType.sendt_ekstern.name(),
        VarselHendelseEventType.venter_ekstern.name(),
        VarselHendelseEventType.opprettet.name(),
        VarselHendelseEventType.inaktivert.name(),
        VarselHendelseEventType.slettet.name()
    );

    private final AtomicInteger forsinkedeBestillinger = new AtomicInteger();

    public KvitteringMetrikk(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        hendelseTyper.forEach(hendelseNavn -> meterRegistry.counter(VARSEL_HENDELSE, HENDELSE_TYPE, hendelseNavn));
        Gauge.builder(BRUKERNOTIFIKASJON_MANGLER_KVITTERING, forsinkedeBestillinger, AtomicInteger::doubleValue).register(meterRegistry);
    }

    public void incrementBrukernotifikasjonKvitteringMottatt(VarselHendelseEventType hendelseEventType) {
        Counter.builder(VARSEL_HENDELSE)
                .tag(HENDELSE_TYPE, hendelseEventType.name())
                .register(meterRegistry)
                .increment();
    }

    public void countForsinkedeVarslerSisteDognet(int antall) {
        forsinkedeBestillinger.setPlain(antall);
    }
}
