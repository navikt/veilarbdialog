package no.nav.fo.veilarbdialog.clients.dialogvarsler;


import lombok.AllArgsConstructor;

@AllArgsConstructor
class DialogVarselDto {
    String subscriptionKey;
    String eventType = "NY_DIALOGMELDING_FRA_BRUKER_TIL_NAV";
}
