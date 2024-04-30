package no.nav.fo.veilarbdialog.util;

import kotlin.reflect.KClass;
import kotlin.reflect.jvm.internal.KClassImpl;
import lombok.AllArgsConstructor;
import no.nav.poao.dab.spring_a2_annotations.auth.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class VeilarbdialogOwnerProvider implements OwnerProvider {

    private final DialogOwnerProvider dialogOwnerProvider;

    @NotNull
    @Override
    public OwnerResult getOwner(@NotNull String resourceId, @NotNull KClass<? extends ResourceType> resourceTypeClass) {
        if (DialogResource.class.isAssignableFrom(((KClassImpl) resourceTypeClass).getJClass()) ) {
            var owner = dialogOwnerProvider.getOwner(resourceId);
            if (owner.isEmpty()) return ResourceNotFound.INSTANCE;
            return new OwnerResultSuccess(owner.get(), null);
        }

        return ResourceNotFound.INSTANCE;
    }
}
