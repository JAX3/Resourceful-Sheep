package com.jax3.resourcefulsheep.resource;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Immutable snapshot of all loaded {@link SheepResource}s for one logical side.
 *
 * <p>There is one instance for the logical server (filled by the reload listener) and one for the client (filled by
 * the sync packet). Use {@link #get(Level)} or {@link #get(boolean)} to pick the right one; in singleplayer both live
 * in the same JVM, so never share state between them.</p>
 */
public final class SheepResourceRegistry {
    private static volatile SheepResourceRegistry server = new SheepResourceRegistry(Map.of());
    private static volatile SheepResourceRegistry client = new SheepResourceRegistry(Map.of());
    private static final List<Consumer<SheepResourceRegistry>> CLIENT_LISTENERS = new java.util.concurrent.CopyOnWriteArrayList<>();

    private final Map<Identifier, SheepResource> byId;
    private final List<SheepResource> ordered;

    public SheepResourceRegistry(Map<Identifier, SheepResource> resources) {
        // Sort by id so feeding lookups are deterministic when several definitions accept the same item.
        Map<Identifier, SheepResource> sorted = new LinkedHashMap<>();
        resources.keySet().stream().sorted().forEach(id -> sorted.put(id, resources.get(id)));
        this.byId = Collections.unmodifiableMap(sorted);
        this.ordered = List.copyOf(sorted.values());
    }

    public static SheepResourceRegistry get(Level level) {
        return get(level.isClientSide());
    }

    public static SheepResourceRegistry get(boolean clientSide) {
        return clientSide ? client : server;
    }

    public static SheepResourceRegistry server() {
        return server;
    }

    /** The client copy. On a dedicated server this is always empty. */
    public static SheepResourceRegistry client() {
        return client;
    }

    static void setServer(SheepResourceRegistry registry) {
        server = registry;
    }

    public static void setClient(SheepResourceRegistry registry) {
        client = registry;
        CLIENT_LISTENERS.forEach(l -> l.accept(registry));
    }

    /** Notified whenever the client receives a new set of definitions (e.g. to refresh creative tabs). */
    public static void addClientListener(Consumer<SheepResourceRegistry> listener) {
        CLIENT_LISTENERS.add(listener);
    }

    public Optional<SheepResource> byId(@Nullable Identifier id) {
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }

    /** Finds the resource whose feeding ingredient accepts the given stack. */
    public Optional<SheepResource> byFeedingItem(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        for (SheepResource resource : ordered) {
            if (resource.feedingItem().test(stack)) return Optional.of(resource);
        }
        return Optional.empty();
    }

    public Collection<SheepResource> all() {
        return ordered;
    }

    public Map<Identifier, SheepResource> asMap() {
        return byId;
    }

    public boolean isEmpty() {
        return ordered.isEmpty();
    }

    public int size() {
        return ordered.size();
    }
}
