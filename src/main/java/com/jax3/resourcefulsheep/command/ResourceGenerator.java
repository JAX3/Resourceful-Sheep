package com.jax3.resourcefulsheep.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jax3.resourcefulsheep.resource.SheepResource;
import com.jax3.resourcefulsheep.resource.SheepResourceRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.MapColor;

/**
 * Builds sheep resource definitions automatically for every material in the game.
 *
 * <p>Materials are discovered from the {@code c:storage_blocks/<material>} item tags that vanilla, NeoForge and
 * practically every content mod populate. For each material not already covered by an existing definition it picks:</p>
 * <ul>
 *     <li><b>feeding item:</b> the storage block tag itself, so every mod's variant of the block works;</li>
 *     <li><b>output:</b> the first item of {@code c:raw_materials/}, {@code c:ingots/}, {@code c:gems/} or
 *     {@code c:dusts/<material>}, else an item named like the material ({@code slime_ball}, {@code resin_clump}...),
 *     else the block itself;</li>
 *     <li><b>uses:</b> 9 when a storage block was broken down into one of those units (a block holds 9), else 1;</li>
 *     <li><b>colours:</b> from the block's map colour;</li>
 *     <li><b>category:</b> ore / gem / dust / material from which tag supplied the output.</li>
 * </ul>
 * The result is plain JSON in a world data pack, so it can be reviewed and hand-edited afterwards.
 */
public final class ResourceGenerator {
    public static final String NAMESPACE = "resourcefulsheep_generated";
    public static final String PACK_NAME = "resourcefulsheep_generated";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String STORAGE_PREFIX = "storage_blocks/";
    private static final String[] OUTPUT_TAGS = {"raw_materials/", "ingots/", "gems/", "dusts/"};
    private static final String[] OUTPUT_SUFFIXES = {"", "_ingot", "_gem", "_dust", "_ball", "_clump", "_shard", "_crystal"};

    private ResourceGenerator() {}

    /** One generated definition: its file name and JSON body. */
    public record Generated(String name, String feeding, Item output, int uses, SheepResource.Category category, JsonObject json) {}

    /** Works out what would be generated, without writing anything. */
    public static List<Generated> plan(HolderLookup.Provider registries, SheepResourceRegistry existing, boolean includeExisting) {
        HolderLookup.RegistryLookup<Item> items = registries.lookupOrThrow(Registries.ITEM);
        Set<String> existingNames = existing.all().stream()
                .filter(r -> !r.id().getNamespace().equals(NAMESPACE))
                .map(r -> r.id().getPath())
                .collect(Collectors.toSet());
        List<Generated> out = new ArrayList<>();
        List<HolderSet.Named<Item>> storageTags = items.listTags()
                .filter(t -> t.key().location().getNamespace().equals("c") && t.key().location().getPath().startsWith(STORAGE_PREFIX))
                .sorted(Comparator.comparing(t -> t.key().location().getPath()))
                .toList();
        for (HolderSet.Named<Item> tag : storageTags) {
            String material = tag.key().location().getPath().substring(STORAGE_PREFIX.length());
            if (material.isEmpty() || material.contains("/") || material.startsWith("raw_")) continue; // raw ore blocks
            List<Holder<Item>> blocks = tag.stream().sorted(VANILLA_FIRST).toList();
            if (blocks.isEmpty()) continue;
            if (!includeExisting) {
                if (existingNames.contains(material)) continue;
                boolean covered = blocks.stream().anyMatch(b -> existing.all().stream()
                        .filter(r -> !r.id().getNamespace().equals(NAMESPACE))
                        .anyMatch(r -> r.feedingItem().test(new ItemStack(b))));
                if (covered) continue;
            }
            out.add(build(items, material, "#" + tag.key().location(), blocks.getFirst().value()));
        }
        return out;
    }

    /** Every material that has a {@code c:storage_blocks/<material>} tag (raw ore blocks excluded), sorted. */
    public static List<String> storageMaterials(HolderLookup.Provider registries) {
        return registries.lookupOrThrow(Registries.ITEM).listTags()
                .map(t -> t.key().location())
                .filter(id -> id.getNamespace().equals("c") && id.getPath().startsWith(STORAGE_PREFIX))
                .map(id -> id.getPath().substring(STORAGE_PREFIX.length()))
                .filter(m -> !m.isEmpty() && !m.contains("/") && !m.startsWith("raw_"))
                .sorted()
                .toList();
    }

    /** Builds a definition for one storage-block material, e.g. {@code "tin"}. Empty if the tag is missing or empty. */
    public static Optional<Generated> forMaterial(HolderLookup.Provider registries, String material) {
        HolderLookup.RegistryLookup<Item> items = registries.lookupOrThrow(Registries.ITEM);
        Identifier tag = Identifier.fromNamespaceAndPath("c", STORAGE_PREFIX + material);
        return items.get(TagKey.create(Registries.ITEM, tag))
                .flatMap(set -> set.stream().sorted(VANILLA_FIRST).findFirst())
                .map(block -> build(items, material, "#" + tag, block.value()));
    }

    /**
     * Builds a definition fed with one specific item (any item, not only storage blocks). The material name is the
     * item's path without {@code _block}/{@code block_}; outputs and uses follow the same rules as the tag scan. If the
     * item is a storage block of a known material, the whole storage-block tag is used so other mods' variants work.
     */
    public static Generated forItem(HolderLookup.Provider registries, Item item) {
        HolderLookup.RegistryLookup<Item> items = registries.lookupOrThrow(Registries.ITEM);
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        String material = path.replaceFirst("^block_of_", "").replaceFirst("^block_", "").replaceFirst("_block$", "");
        Identifier tag = Identifier.fromNamespaceAndPath("c", STORAGE_PREFIX + material);
        boolean inStorageTag = items.get(TagKey.create(Registries.ITEM, tag))
                .map(set -> set.stream().anyMatch(h -> h.value() == item)).orElse(false);
        String feeding = inStorageTag ? "#" + tag : BuiltInRegistries.ITEM.getKey(item).toString();
        return build(items, material, feeding, item);
    }

    /** A resource that is not generated and already accepts one of {@code generated}'s feeding items, if any. */
    public static Optional<SheepResource> coveredBy(HolderLookup.Provider registries, SheepResourceRegistry existing, Generated generated) {
        var ops = registries.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        var parsed = SheepResource.DEFINITION_CODEC.parse(ops, generated.json()).result();
        if (parsed.isEmpty()) return Optional.empty();
        SheepResource candidate = SheepResource.withId(Identifier.fromNamespaceAndPath(NAMESPACE, generated.name()), parsed.get());
        String name = candidate.name().getString().toLowerCase(Locale.ROOT);
        Optional<SheepResource> sameName = existing.all().stream().filter(r -> !isGenerated(r)
                && (r.id().getPath().equals(generated.name()) || r.name().getString().toLowerCase(Locale.ROOT).equals(name))).findFirst();
        if (sameName.isPresent()) return sameName;
        for (Holder<Item> h : candidate.feedingItems()) {
            Optional<SheepResource> hit = existing.all().stream()
                    .filter(r -> !isGenerated(r) && r.feedingItem().test(new ItemStack(h))).findFirst();
            if (hit.isPresent()) return hit;
        }
        return Optional.empty();
    }

    private static final Comparator<Holder<Item>> VANILLA_FIRST = Comparator
            .comparing((Holder<Item> h) -> !BuiltInRegistries.ITEM.getKey(h.value()).getNamespace().equals("minecraft"))
            .thenComparing(h -> BuiltInRegistries.ITEM.getKey(h.value()).toString());

    private static Generated build(HolderLookup.RegistryLookup<Item> items, String material, String feeding, Item block) {
        Item output = null;
        SheepResource.Category category = SheepResource.Category.MATERIAL;
        for (String prefix : OUTPUT_TAGS) {
            Optional<Item> found = firstOfTag(items, Identifier.fromNamespaceAndPath("c", prefix + material));
            if (found.isPresent()) {
                output = found.get();
                category = switch (prefix) {
                    case "raw_materials/" -> SheepResource.Category.ORE;
                    case "gems/" -> SheepResource.Category.GEM;
                    case "dusts/" -> SheepResource.Category.DUST;
                    default -> SheepResource.Category.MATERIAL;
                };
                break;
            }
        }
        if (output == null) output = byName(material, BuiltInRegistries.ITEM.getKey(block).getNamespace(), block).orElse(null);
        boolean brokenDown = output != null && output != block;
        if (output == null) output = block;
        // Only a real storage block (c:storage_blocks tag) is known to hold 9 units; anything else defaults to 1.
        int uses = brokenDown && feeding.startsWith("#c:" + STORAGE_PREFIX) ? 9 : 1;
        if (category == SheepResource.Category.MATERIAL && material.matches(".*(wheat|kelp|bone|slime|resin|wood|log|honey).*")) {
            category = SheepResource.Category.ORGANIC;
        }

        // Blocks use their map colour; plain items get a stable colour derived from their id (edit the JSON to taste).
        int color = block instanceof BlockItem bi ? bi.getBlock().defaultMapColor().col
                : java.awt.Color.HSBtoRGB((BuiltInRegistries.ITEM.getKey(block).hashCode() & 0xFFFF) / 65535F, 0.45F, 0.85F) & 0xFFFFFF;
        if (color == 0) color = 0xC8C8C8;
        int accent = lighten(color, 0.55F);

        JsonObject json = new JsonObject();
        json.addProperty("_generated", "by /resourcefulsheep generate; safe to edit");
        json.addProperty("feeding_item", feeding);
        json.addProperty("feeding_amount", 1);
        json.addProperty("display_name", title(material));
        json.addProperty("wool_color", hex(color));
        json.addProperty("accent_color", hex(accent));
        json.addProperty("category", category.getSerializedName());
        json.addProperty("wool_drop_count", 1);
        json.addProperty("infusion_uses", uses);
        JsonObject sep = new JsonObject();
        sep.addProperty("primary_output", BuiltInRegistries.ITEM.getKey(output).toString());
        JsonObject count = new JsonObject();
        count.addProperty("min", 2);
        count.addProperty("max", 4);
        sep.add("primary_count", count);
        sep.addProperty("secondary_output", "minecraft:string");
        sep.addProperty("secondary_count", 2);
        json.add("separator", sep);
        json.addProperty("growth_item", BuiltInRegistries.ITEM.getKey(block).toString());
        return new Generated(material, feeding, output, uses, category, json);
    }

    private static Optional<Item> firstOfTag(HolderLookup.RegistryLookup<Item> items, Identifier tagId) {
        return items.get(TagKey.create(Registries.ITEM, tagId))
                .flatMap(set -> set.stream().sorted(VANILLA_FIRST).findFirst())
                .map(Holder::value);
    }

    private static Optional<Item> byName(String material, String namespace, Item block) {
        for (String ns : new String[]{namespace, "minecraft"}) {
            for (String suffix : OUTPUT_SUFFIXES) {
                Identifier id = Identifier.tryBuild(ns, material + suffix);
                if (id == null) continue;
                Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
                if (item.isPresent() && item.get() != block && !(item.get() instanceof BlockItem)) return item;
            }
        }
        return Optional.empty();
    }

    /** A generated resource that duplicates another one, and why. */
    public record Duplicate(SheepResource resource, SheepResource keptInstead, String reason) {}

    /**
     * Finds generated resources ({@value #NAMESPACE} namespace) that duplicate another resource: their feeding items
     * overlap, or they have the same name. Built-in and hand-written definitions always win and are never reported;
     * between two generated ones, the first by id is kept.
     */
    public static List<Duplicate> findDuplicates(SheepResourceRegistry registry) {
        List<SheepResource> kept = new ArrayList<>(registry.all().stream().filter(r -> !isGenerated(r)).toList());
        List<Duplicate> duplicates = new ArrayList<>();
        List<SheepResource> generated = registry.all().stream().filter(ResourceGenerator::isGenerated)
                .sorted(Comparator.comparing(r -> r.id().toString())).toList();
        for (SheepResource g : generated) {
            Duplicate dup = null;
            String name = g.name().getString().toLowerCase(Locale.ROOT);
            for (SheepResource k : kept) {
                if (k.id().getPath().equals(g.id().getPath()) || k.name().getString().toLowerCase(Locale.ROOT).equals(name)) {
                    dup = new Duplicate(g, k, "same name");
                    break;
                }
                boolean overlap = g.feedingItems().stream().anyMatch(h -> k.feedingItem().test(new ItemStack(h)));
                if (overlap) {
                    dup = new Duplicate(g, k, "same feeding item");
                    break;
                }
            }
            if (dup != null) duplicates.add(dup);
            else kept.add(g);
        }
        return duplicates;
    }

    public static boolean isGenerated(SheepResource resource) {
        return resource.id().getNamespace().equals(NAMESPACE);
    }

    /** Deletes the given generated definitions from the world's generated data pack. Returns how many files were removed. */
    public static int delete(Path datapacksDir, List<SheepResource> resources) throws IOException {
        Path dir = datapacksDir.resolve(PACK_NAME).resolve("data").resolve(NAMESPACE).resolve("resourcefulsheep").resolve("sheep_resources");
        int removed = 0;
        for (SheepResource r : resources) {
            if (!isGenerated(r)) continue; // never touch built-in or hand-written definitions
            if (Files.deleteIfExists(dir.resolve(r.id().getPath() + ".json"))) removed++;
        }
        return removed;
    }

    /** Writes the definitions as a world data pack (replacing any previous generated pack). Returns the pack folder. */
    public static Path write(Path datapacksDir, List<Generated> generated) throws IOException {
        Path pack = datapacksDir.resolve(PACK_NAME);
        Path dir = pack.resolve("data").resolve(NAMESPACE).resolve("resourcefulsheep").resolve("sheep_resources");
        if (Files.exists(dir)) {
            try (var files = Files.list(dir)) {
                for (Path p : files.filter(f -> f.toString().endsWith(".json")).toList()) Files.delete(p);
            }
        }
        Files.createDirectories(dir);
        for (Generated g : generated) {
            Files.writeString(dir.resolve(g.name() + ".json"), GSON.toJson(g.json()), StandardCharsets.UTF_8);
        }
        writePackMeta(pack);
        return pack;
    }

    /** Adds (or replaces) a single generated definition without touching the others. Returns the pack folder. */
    public static Path writeOne(Path datapacksDir, Generated generated) throws IOException {
        Path pack = datapacksDir.resolve(PACK_NAME);
        Path dir = pack.resolve("data").resolve(NAMESPACE).resolve("resourcefulsheep").resolve("sheep_resources");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(generated.name() + ".json"), GSON.toJson(generated.json()), StandardCharsets.UTF_8);
        writePackMeta(pack);
        return pack;
    }

    private static void writePackMeta(Path pack) throws IOException {
        JsonObject meta = new JsonObject();
        JsonObject packObj = new JsonObject();
        packObj.addProperty("description", "Resourceful Sheep: auto-generated resources");
        int format = SharedConstants.getCurrentVersion().packVersion(net.minecraft.server.packs.PackType.SERVER_DATA).major();
        JsonArray fmt = new JsonArray();
        fmt.add(format);
        fmt.add(0);
        packObj.add("min_format", fmt);
        packObj.add("max_format", fmt.deepCopy());
        meta.add("pack", packObj);
        Files.writeString(pack.resolve("pack.mcmeta"), GSON.toJson(meta), StandardCharsets.UTF_8);
    }

    static String title(String material) {
        StringBuilder sb = new StringBuilder();
        for (String part : material.split("_")) {
            if (part.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private static String hex(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    private static int lighten(int rgb, float t) {
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF;
        r += (int) ((255 - r) * t);
        g += (int) ((255 - g) * t);
        b += (int) ((255 - b) * t);
        return r << 16 | g << 8 | b;
    }
}
