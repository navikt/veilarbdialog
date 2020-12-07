package no.nav.fo.veilarbdialog.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class TestKontorsperreEnhetData implements KontorsperreEnhetData {

    public static TestKontorsperreEnhetData ALLOWED = new TestKontorsperreEnhetData(null);

    private final String kontorsperreEnhetId;
}
