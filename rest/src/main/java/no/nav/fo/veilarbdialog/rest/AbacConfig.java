package no.nav.fo.veilarbdialog.rest;

import no.nav.sbl.dialogarena.common.abac.pep.context.AbacContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({AbacContext.class})
public class AbacConfig {}