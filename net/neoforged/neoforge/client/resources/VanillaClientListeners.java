package net.neoforged.neoforge.client.resources;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.PeriodicNotificationManager;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.particle.ParticleResources;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.GpuWarnlistManager;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DryFoliageColorReloadListener;
import net.minecraft.client.resources.FoliageColorReloadListener;
import net.minecraft.client.resources.GrassColorReloadListener;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.client.resources.WaypointStyleManager;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.neoforge.common.util.VanillaClassToKey;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

public class VanillaClientListeners {
   private static final Map<Class<?>, Identifier> KNOWN_CLASSES = new LinkedHashMap<>();
   public static final Identifier LANGUAGE = key(LanguageManager.class);
   public static final Identifier TEXTURES = key(TextureManager.class);
   public static final Identifier SHADERS = key(ShaderManager.class);
   public static final Identifier SOUNDS = key(SoundManager.class);
   public static final Identifier SPLASHES = key(SplashManager.class);
   public static final Identifier ATLASES = key(AtlasManager.class);
   public static final Identifier FONTS = key(FontManager.class);
   public static final Identifier GRASS_COLOR = key(GrassColorReloadListener.class);
   public static final Identifier FOLIAGE_COLOR = key(FoliageColorReloadListener.class);
   public static final Identifier DRY_FOLIAGE_COLOR = key(DryFoliageColorReloadListener.class);
   public static final Identifier MODELS = key(ModelManager.class);
   public static final Identifier EQUIPMENT_ASSETS = key(EquipmentAssetManager.class);
   public static final Identifier BLOCK_RENDERER = key(BlockRenderDispatcher.class);
   public static final Identifier ENTITY_RENDERER = key(EntityRenderDispatcher.class);
   public static final Identifier BLOCK_ENTITY_RENDERER = key(BlockEntityRenderDispatcher.class);
   public static final Identifier PARTICLE_RESOURCES = key(ParticleResources.class);
   public static final Identifier WAYPOINT_STYLES = key(WaypointStyleManager.class);
   public static final Identifier LEVEL_RENDERER = key(LevelRenderer.class);
   public static final Identifier CLOUD_RENDERER = key(CloudRenderer.class);
   public static final Identifier GPU_WARNLIST = key(GpuWarnlistManager.class);
   public static final Identifier REGIONAL_COMPLIANCES = key(PeriodicNotificationManager.class);
   public static final Identifier FIRST = LANGUAGE;
   public static final Identifier LAST = REGIONAL_COMPLIANCES;

   private static Identifier key(Class<? extends PreparableReloadListener> cls) {
      if (KNOWN_CLASSES.containsKey(cls)) {
         throw new UnsupportedOperationException("Attempted to create two keys for the same class");
      } else {
         Identifier key = VanillaClassToKey.convert(cls);
         KNOWN_CLASSES.put(cls, key);
         return key;
      }
   }

   @Internal
   @Nullable
   public static Identifier getNameForClass(Class<? extends PreparableReloadListener> cls) {
      return KNOWN_CLASSES.get(cls);
   }
}
