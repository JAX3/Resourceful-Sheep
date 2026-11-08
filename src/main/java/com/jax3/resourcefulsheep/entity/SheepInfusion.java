package com.jax3.resourcefulsheep.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * Infusion state stored on a sheep as a NeoForge data attachment (saved with the entity and synced to clients).
 *
 * @param resource       the infused resource, empty when the sheep is not infused
 * @param usesRemaining  shearings left before the infusion runs out; {@code -1} = permanent
 * @param cooldownUntil  game time before which the sheep cannot be infused again
 */
public record SheepInfusion(Optional<Identifier> resource, int usesRemaining, long cooldownUntil) {
    public static final SheepInfusion NONE = new SheepInfusion(Optional.empty(), 0, 0L);

    public static final MapCodec<SheepInfusion> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.optionalFieldOf("resource").forGetter(SheepInfusion::resource),
            Codec.INT.optionalFieldOf("uses_remaining", 0).forGetter(SheepInfusion::usesRemaining),
            Codec.LONG.optionalFieldOf("cooldown_until", 0L).forGetter(SheepInfusion::cooldownUntil)
    ).apply(i, SheepInfusion::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SheepInfusion> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC.apply(ByteBufCodecs::optional), SheepInfusion::resource,
            ByteBufCodecs.VAR_INT, SheepInfusion::usesRemaining,
            ByteBufCodecs.VAR_LONG, SheepInfusion::cooldownUntil,
            SheepInfusion::new);

    public boolean isInfused() {
        return resource.isPresent();
    }

    public SheepInfusion withResource(Identifier id, int uses) {
        return new SheepInfusion(Optional.of(id), uses, cooldownUntil);
    }

    /** Uses up one shearing. Returns the cleared state once no uses remain (permanent infusions are unchanged). */
    public SheepInfusion afterShearing(long gameTime, int cooldownTicks) {
        if (usesRemaining < 0) return this;
        int left = usesRemaining - 1;
        if (left <= 0) return new SheepInfusion(Optional.empty(), 0, cooldownTicks > 0 ? gameTime + cooldownTicks : 0L);
        return new SheepInfusion(resource, left, cooldownUntil);
    }

    public boolean onCooldown(long gameTime) {
        return gameTime < cooldownUntil;
    }
}
