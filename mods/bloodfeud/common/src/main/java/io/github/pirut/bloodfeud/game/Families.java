package io.github.pirut.bloodfeud.game;

import io.github.pirut.bloodfeud.BloodFeud;
import io.github.pirut.bloodfeud.feud.Family;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.util.EnumMap;
import java.util.Map;

/** Which family a mob belongs to, from the entity tags bloodfeud:rotten, :bones, :illagers and :nether. */
public final class Families {
    private static final Map<Family, TagKey<EntityType<?>>> TAGS = new EnumMap<>(Family.class);

    static {
        for (Family f : Family.values()) TAGS.put(f, TagKey.create(Registries.ENTITY_TYPE, BloodFeud.id(f.key())));
    }

    private Families() {
    }

    public static Family of(EntityType<?> type) {
        for (Map.Entry<Family, TagKey<EntityType<?>>> e : TAGS.entrySet()) if (type.is(e.getValue())) return e.getKey();
        return null;
    }
}
