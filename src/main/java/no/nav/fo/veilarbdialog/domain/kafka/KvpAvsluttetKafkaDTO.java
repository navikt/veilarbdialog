package no.nav.fo.veilarbdialog.domain.kafka;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Builder
@Accessors(chain = true)
public class KvpAvsluttetKafkaDTO {
    private String aktorId;
    private String avsluttetAv;
    private ZonedDateTime avsluttetDato;
    private String avsluttetBegrunnelse;
}
