package no.nav.fo.veilarbdialog.util;

import org.junit.Test;

import static no.nav.fo.veilarbdialog.util.StringUtils.notNullOrEmpty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class StringUtilsTest {

    @Test
    public void notNullOrEmpty_() {
        assertThat(notNullOrEmpty(null), is(false));
        assertThat(notNullOrEmpty(""), is(false));
        assertThat(notNullOrEmpty(" "), is(false));
        assertThat(notNullOrEmpty(" a "), is(true));
    }

}