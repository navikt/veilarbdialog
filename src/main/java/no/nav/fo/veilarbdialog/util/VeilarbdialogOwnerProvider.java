package no.nav.fo.veilarbdialog.util;

import kotlin.reflect.KClass;
import no.nav.poao.dab.spring_a2_annotations.auth.OwnerProvider;
import no.nav.poao.dab.spring_a2_annotations.auth.OwnerResult;
import no.nav.poao.dab.spring_a2_annotations.auth.ResourceType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class VeilarbdialogOwnerProvider implements OwnerProvider {
    @NotNull
    @Override
    public OwnerResult getOwner(@NotNull String s, @NotNull KClass<? extends ResourceType> kClass) {
        return null;
    }
}
