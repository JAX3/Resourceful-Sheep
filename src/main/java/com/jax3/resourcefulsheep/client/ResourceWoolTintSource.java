package com.jax3.resourcefulsheep.client;

import com.jax3.resourcefulsheep.items.ResourceWoolItem;
import com.jax3.resourcefulsheep.resource.SheepResource;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Item tint for {@code resourcefulsheep:resource_wool} models: colours a greyscale wool layer with the resource's
 * wool or accent colour. Used by the fallback model so resources added by data packs always get a sensible texture.
 *
 * <pre>{@code "tints": [ { "type": "resourcefulsheep:resource_wool", "channel": "wool" } ]}</pre>
 */
public record ResourceWoolTintSource(Channel channel) implements ItemTintSource {
    public static final MapCodec<ResourceWoolTintSource> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Channel.CODEC.optionalFieldOf("channel", Channel.WOOL).forGetter(ResourceWoolTintSource::channel)
    ).apply(i, ResourceWoolTintSource::new));

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        return ResourceWoolItem.getResource(stack, true)
                .map(r -> ARGB.opaque(channel == Channel.WOOL ? r.woolColor() : r.accentColor()))
                .orElse(channel == Channel.WOOL ? 0xFFE6E6E6 : 0xFFFFFFFF);
    }

    @Override
    public MapCodec<ResourceWoolTintSource> type() {
        return MAP_CODEC;
    }

    public enum Channel implements StringRepresentable {
        WOOL, ACCENT;

        public static final com.mojang.serialization.Codec<Channel> CODEC = StringRepresentable.fromEnum(Channel::values);

        @Override
        public String getSerializedName() {
            return this == WOOL ? "wool" : "accent";
        }
    }

    static int colorOf(SheepResource r, Channel c) {
        return c == Channel.WOOL ? r.woolColor() : r.accentColor();
    }
}
