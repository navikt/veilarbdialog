package no.nav.fo.veilarbdialog.domain;

import lombok.Data;

@Data
public class DialogStatusOppdaterer {
    private final long dialogId;
    private BooleanUpdateEnum historisk = BooleanUpdateEnum.KEEP;
    private DateUpdateEnum venterPaSvarFraBruker = DateUpdateEnum.KEEP;
    private DateUpdateEnum venterPaNavSiden = DateUpdateEnum.KEEP;
    private DateUpdateEnum eldsteUlesteForVeileder = DateUpdateEnum.KEEP;
    private DateUpdateEnum eldsteUlesteForBruker = DateUpdateEnum.KEEP;
    private DateUpdateEnum lestAvVeilederTid = DateUpdateEnum.KEEP;
    private DateUpdateEnum lestAvBrukerTid = DateUpdateEnum.KEEP;

    public DialogStatusOppdaterer(long dialogId) {
        this.dialogId = dialogId;
    }

    public void lestAvVeileder() {
        eldsteUlesteForVeileder = DateUpdateEnum.NULL;
        lestAvVeilederTid = DateUpdateEnum.NOW;
    }

    public void lestAvBruker() {
        eldsteUlesteForBruker = DateUpdateEnum.NULL;
        lestAvBrukerTid = DateUpdateEnum.NOW;
    }
}
