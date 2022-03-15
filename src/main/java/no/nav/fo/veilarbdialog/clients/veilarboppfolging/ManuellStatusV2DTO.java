package no.nav.fo.veilarbdialog.clients.veilarboppfolging;

import lombok.Value;

@Value
public class ManuellStatusV2DTO {
    boolean erUnderManuellOppfolging;
    KrrStatus krrStatus;

    @Value
    public static class KrrStatus {
        boolean kanVarsles;
        boolean erReservert;
    }
}
