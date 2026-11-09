package com.jax3.resourcefulsheep.resource;

import com.jax3.resourcefulsheep.config.ModConfigs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * A single data-driven resource a sheep can be infused with.
 *
 * <p>Loaded from {@code data/<namespace>/resourcefulsheep/sheep_resources/<path>.json}; the resource id is
 * {@code <namespace>:<path>}. The same object drives sheep feeding, shearing drops, the Mechanical Separator and
 * JEI, so all of those always agree. See {@code README.md} for the full JSON reference.</p>
 *
 * @param id            resource id (from the file location, not part of the JSON)
 * @param feedingItem   what must be fed to the sheep; an item, a tag ({@code "#c:storage_blocks/tin"}) or a list
 * @param feedingAmount items consumed per feeding
 * @param displayName   optional resource name ("Iron"); defaults to the translation key {@code resource.<ns>.<path>}
 * @param woolName      optional full wool name ("Iron Wool"); defaults to "{displayName} Wool"
 * @param woolColor     main RGB colour of the wool (sheep and item tint)
 * @param accentColor   RGB colour of the glinting strands/flecks
 * @param category      what kind of material this is; purely informational except for defaults documented in README
 * @param woolDropCount infused wool dropped per shearing
 * @param infusionUses  number of shearings one feeding lasts (before the global multiplier)
 * @param separator     how the Mechanical Separator processes this resource's wool
 * @param growthItem    optional item/block shown growing out of the fleece; defaults to the first feeding item
 */
public record SheepResource(
        Identifier id,
        Ingredient feedingItem,
        int feedingAmount,
        Optional<Component> displayName,
        Optional<Component> woolName,
        int woolColor,
        int accentColor,
        Category category,
        int woolDropCount,
        int infusionUses,
        Separator separator,
        Optional<ItemStackTemplate> growthItem) {

    /** Accepts {@code "#RRGGBB"}, {@code "RRGGBB"}, an integer, or an {@code [r, g, b]} array. */
    public static final Codec<Integer> COLOR_CODEC = Codec.withAlternative(
            Codec.STRING.comapFlatMap(SheepResource::parseHex, c -> String.format(Locale.ROOT, "#%06X", c & 0xFFFFFF)),
            Codec.withAlternative(Codec.INT, Codec.intRange(0, 255).listOf(3, 3).xmap(
                    l -> l.get(0) << 16 | l.get(1) << 8 | l.get(2),
                    c -> List.of(c >> 16 & 0xFF, c >> 8 & 0xFF, c & 0xFF))))
            .xmap(c -> c & 0xFFFFFF, c -> c & 0xFFFFFF);

    /** Codec for the JSON body; the id is supplied separately by {@link #withId}. */
    public static final Codec<Definition> DEFINITION_CODEC = RecordCodecBuilder.create(i -> i.group(
            Ingredient.CODEC.fieldOf("feeding_item").forGetter(Definition::feedingItem),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("feeding_amount", 1).forGetter(Definition::feedingAmount),
            ComponentSerialization.CODEC.optionalFieldOf("display_name").forGetter(Definition::displayName),
            ComponentSerialization.CODEC.optionalFieldOf("wool_name").forGetter(Definition::woolName),
            COLOR_CODEC.optionalFieldOf("wool_color", 0xE0E0E0).forGetter(Definition::woolColor),
            COLOR_CODEC.optionalFieldOf("accent_color", 0xFFFFFF).forGetter(Definition::accentColor),
            Category.CODEC.optionalFieldOf("category", Category.MATERIAL).forGetter(Definition::category),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("wool_drop_count", 1).forGetter(Definition::woolDropCount),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("infusion_uses", 1).forGetter(Definition::infusionUses),
            Separator.CODEC.optionalFieldOf("separator", Separator.DEFAULT).forGetter(Definition::separator),
            ItemStackTemplate.CODEC.optionalFieldOf("growth_item").forGetter(Definition::growthItem)
    ).apply(i, Definition::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SheepResource> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, SheepResource::id,
            ByteBufCodecs.fromCodecWithRegistries(DEFINITION_CODEC), SheepResource::definition,
            SheepResource::withId);

    public static SheepResource withId(Identifier id, Definition d) {
        return new SheepResource(id, d.feedingItem, d.feedingAmount, d.displayName, d.woolName, d.woolColor,
                d.accentColor, d.category, d.woolDropCount, d.infusionUses, d.separator, d.growthItem);
    }

    public Definition definition() {
        return new Definition(feedingItem, feedingAmount, displayName, woolName, woolColor, accentColor, category,
                woolDropCount, infusionUses, separator, growthItem);
    }

    // ------------------------------------------------------------------ derived values (with config applied)

    public String translationKey() {
        return "resource." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    /** "Iron" */
    public Component name() {
        return displayName.orElseGet(() -> Component.translatable(translationKey()));
    }

    /** "Iron Wool" */
    public Component woolDisplayName() {
        return woolName.orElseGet(() -> Component.translatable("item.resourcefulsheep.resource_wool.named", name()));
    }

    /** Items consumed per feeding, after config overrides. */
    public int effectiveFeedingAmount() {
        int override = ModConfigs.getInt(ModConfigs.SERVER.feedingAmountOverride);
        return override >= 0 ? Math.max(1, override) : feedingAmount;
    }

    /** Wool dropped per shearing, after config overrides (excluding the random bonus). */
    public int effectiveWoolDropCount() {
        int override = ModConfigs.getInt(ModConfigs.SERVER.woolDropCountOverride);
        return override >= 0 ? override : woolDropCount;
    }

    /** Shearings per feeding after the config multiplier; {@code -1} when infusions are permanent. */
    public int effectiveInfusionUses() {
        if (!ModConfigs.getBool(ModConfigs.SERVER.infusionConsumedByShearing)) return -1;
        double mult = ModConfigs.getDouble(ModConfigs.SERVER.infusionUsesMultiplier);
        return Math.max(1, (int) Math.round(infusionUses * mult));
    }

    /** Example item to show for the feeding input (first matching item). */
    /** What grows out of an infused sheep's fleece: {@code growth_item}, or else the first feeding item. */
    public ItemStack growthStack() {
        return growthItem.map(ItemStackTemplate::create)
                .orElseGet(() -> feedingItem.items().findFirst().map(h -> new ItemStack(h)).orElse(ItemStack.EMPTY));
    }

    public ItemStack exampleFeedingStack() {
        return feedingItem.items().findFirst().map(h -> new ItemStack(h, effectiveFeedingAmount())).orElse(ItemStack.EMPTY);
    }

    public List<Holder<Item>> feedingItems() {
        return feedingItem.items().toList();
    }

    /** Resolves this resource's separator block into a concrete {@link Separation}. */
    public Separation separation() {
        ItemStackTemplate primary = separator.primaryOutput().orElseGet(() -> feedingItem.items().findFirst()
                .map(ItemStackTemplate::new).orElse(new ItemStackTemplate(Items.STRING)));
        CountRange primaryCount = separator.primaryCount().orElse(CountRange.exactly(primary.count()));
        int secondaryCount = separator.secondaryCount().orElseGet(() -> ModConfigs.getInt(ModConfigs.SERVER.defaultStringOutput));
        return new Separation(primary.withCount(Math.max(1, primaryCount.min())), primaryCount.max(),
                secondaryCount > 0 ? Optional.of(separator.secondaryOutput().withCount(secondaryCount)) : Optional.empty(),
                separator.bonusOutput(), separator.energyCost(), separator.processingTicks());
    }

    private static DataResult<Integer> parseHex(String s) {
        String hex = s.startsWith("#") ? s.substring(1) : s;
        if (hex.startsWith("0x") || hex.startsWith("0X")) hex = hex.substring(2);
        try {
            if (hex.length() != 6) return DataResult.error(() -> "Colour must be 6 hex digits: " + s);
            return DataResult.success(Integer.parseInt(hex, 16));
        } catch (NumberFormatException e) {
            return DataResult.error(() -> "Invalid hex colour: " + s);
        }
    }

    // ------------------------------------------------------------------ nested types

    public record Definition(
            Ingredient feedingItem, int feedingAmount, Optional<Component> displayName, Optional<Component> woolName,
            int woolColor, int accentColor, Category category, int woolDropCount, int infusionUses, Separator separator,
            Optional<ItemStackTemplate> growthItem) {}

    /** What kind of material a resource is. Ores should normally separate into their raw material. */
    public enum Category implements StringRepresentable {
        ORE, GEM, DUST, MATERIAL, ORGANIC;

        public static final Codec<Category> CODEC = StringRepresentable.fromEnum(Category::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Component displayName() {
            return Component.translatable("category.resourcefulsheep." + getSerializedName());
        }
    }

    /**
     * Separator settings for a resource.
     *
     * @param primaryOutput   extracted material; defaults to the first feeding item
     * @param primaryCount    overrides the count inside {@code primaryOutput}; a number or {@code {"min": 2, "max": 4}}
     * @param secondaryOutput byproduct, string by default
     * @param secondaryCount  byproduct count; defaults to the {@code defaultStringOutput} config value
     * @param bonusOutput     optional chance-based extra output
     * @param energyCost      total FE per operation; defaults to {@code defaultEnergyCost}
     * @param processingTicks duration; defaults to {@code defaultProcessingTicks}
     */
    public record Separator(
            Optional<ItemStackTemplate> primaryOutput,
            Optional<CountRange> primaryCount,
            ItemStackTemplate secondaryOutput,
            Optional<Integer> secondaryCount,
            Optional<ChanceOutput> bonusOutput,
            Optional<Integer> energyCost,
            Optional<Integer> processingTicks) {

        public static final Separator DEFAULT = new Separator(Optional.empty(), Optional.empty(),
                new ItemStackTemplate(Items.STRING), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

        public static final Codec<Separator> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStackTemplate.CODEC.optionalFieldOf("primary_output").forGetter(Separator::primaryOutput),
                CountRange.CODEC.optionalFieldOf("primary_count").forGetter(Separator::primaryCount),
                ItemStackTemplate.CODEC.optionalFieldOf("secondary_output", new ItemStackTemplate(Items.STRING)).forGetter(Separator::secondaryOutput),
                ExtraCodecs.intRange(0, 99).optionalFieldOf("secondary_count").forGetter(Separator::secondaryCount),
                ChanceOutput.CODEC.optionalFieldOf("bonus_output").forGetter(Separator::bonusOutput),
                ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("energy_cost").forGetter(Separator::energyCost),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("processing_ticks").forGetter(Separator::processingTicks)
        ).apply(i, Separator::new));
    }
}
