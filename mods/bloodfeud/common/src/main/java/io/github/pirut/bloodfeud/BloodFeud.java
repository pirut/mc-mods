package io.github.pirut.bloodfeud;

import io.github.pirut.mcmods.lib.CommonLib;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Code shared by both loaders. Each loader's entry point calls {@link #init}. */
public final class BloodFeud {
    public static final String MOD_ID = "bloodfeud";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);
    /** Mobs that can rise to Captain. Data packs can add their own. */
    public static final TagKey<EntityType<?>> CAN_RISE = TagKey.create(Registries.ENTITY_TYPE, id("can_rise"));

    private static Platform platform;
    private static Config config = new Config();

    public static void init(Platform p) {
        platform = p;
        config = Config.load(p.configDir().resolve(MOD_ID + ".properties"));
        LOG.info("{} loaded on {} (common-lib {})", MOD_ID, p.loader(), CommonLib.version());
    }

    public static Platform platform() {
        return platform;
    }

    public static Config config() {
        return config;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private BloodFeud() {
    }
}
