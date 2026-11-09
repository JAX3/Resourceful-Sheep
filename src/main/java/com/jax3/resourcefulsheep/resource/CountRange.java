package com.jax3.resourcefulsheep.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * An inclusive item-count range. In JSON either a plain number ({@code 3}) or {@code {"min": 2, "max": 4}}.
 */
public record CountRange(int min, int max) {
    private static final Codec<CountRange> OBJECT_CODEC = RecordCodecBuilder.<CountRange>create(i -> i.group(
            Codec.intRange(1, 99).fieldOf("min").forGetter(CountRange::min),
            Codec.intRange(1, 99).fieldOf("max").forGetter(CountRange::max)
    ).apply(i, CountRange::new)).validate(r -> r.min <= r.max
            ? DataResult.success(r) : DataResult.error(() -> "min (" + r.min + ") must not exceed max (" + r.max + ")"));

    public static final Codec<CountRange> CODEC = Codec.withAlternative(OBJECT_CODEC,
            Codec.intRange(1, 99).xmap(n -> new CountRange(n, n), CountRange::min));

    public static CountRange exactly(int n) {
        return new CountRange(n, n);
    }

    public boolean isFixed() {
        return min == max;
    }

    /** "3" or "2-4". */
    public String label() {
        return isFixed() ? Integer.toString(min) : min + "-" + max;
    }
}
