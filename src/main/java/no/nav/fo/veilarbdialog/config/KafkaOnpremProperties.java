package no.nav.fo.veilarbdialog.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "application.kafka")
public class KafkaOnpremProperties {
    String brokersUrl;
    String endringPaaDialogTopic;
    String oppfolgingAvsluttetTopic;
    String kvpAvsluttetTopic;
}

