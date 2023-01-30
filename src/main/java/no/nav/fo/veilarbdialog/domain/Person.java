package no.nav.fo.veilarbdialog.domain;

import no.nav.common.types.identer.EksternBrukerId;
import no.nav.common.types.identer.Fnr;

import java.util.Objects;

public abstract class Person {
    private final String id;

    private Person(String id) {
        this.id = id;
    }

    public String get() {
        return id;
    }

    public static Fnr fnr(String fnr) {
        return new Fnr(fnr);
    }

    public static AktorId aktorId(String aktorId) {
        return new AktorId(aktorId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return id.equals(person.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static class Fnr extends Person {
        private Fnr(String id) {
            super(id);
        }
    }

    public static class AktorId extends Person {

        private AktorId(String id) {
            super(id);
        }
    }

    public EksternBrukerId eksternBrukerId() {
        if (this instanceof Fnr) return no.nav.common.types.identer.Fnr.of(this.get());
        if (this instanceof AktorId) return no.nav.common.types.identer.AktorId.of(this.get());
        throw new IllegalStateException("EksternBrukerId må være Fnr eller AktorId");
    }

}
