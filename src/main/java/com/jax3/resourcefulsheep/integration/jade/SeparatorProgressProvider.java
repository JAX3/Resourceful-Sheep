package com.jax3.resourcefulsheep.integration.jade;

import com.jax3.resourcefulsheep.ResourcefulSheep;
import com.jax3.resourcefulsheep.blockentity.MechanicalSeparatorBlockEntity;
import java.util.List;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ProgressView;
import snownee.jade.api.view.ViewGroup;

/** Jade's progress arrow for the separator's current operation, animated smoothly between server updates. */
public enum SeparatorProgressProvider implements IServerExtensionProvider<ProgressView.Data>,
        IClientExtensionProvider<ProgressView.Data, ProgressView> {
    INSTANCE;

    public static final Identifier UID = ResourcefulSheep.id("separator_progress");

    @Override
    public @Nullable List<ViewGroup<ProgressView.Data>> getGroups(Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor block) || !(block.getBlockEntity() instanceof MechanicalSeparatorBlockEntity be)) return null;
        int max = be.getMaxProgress();
        if (max <= 0) return null;
        float progress = (float) be.getProgress() / max;
        float speed = be.getStatus() == com.jax3.resourcefulsheep.blockentity.MachineStatus.PROCESSING ? 1.0F / max : 0.0F;
        return List.of(new ViewGroup<>(List.of(new ProgressView.Data(progress, speed, 1.0F))));
    }

    @Override
    public List<ClientViewGroup<ProgressView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<ProgressView.Data>> groups) {
        return ClientViewGroup.map(groups, ProgressView::read, null);
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
