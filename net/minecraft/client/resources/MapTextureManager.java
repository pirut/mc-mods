package net.minecraft.client.resources;

import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

@Environment(EnvType.CLIENT)
public class MapTextureManager implements AutoCloseable {
   private final Int2ObjectMap<MapTextureManager.MapInstance> maps = new Int2ObjectOpenHashMap();
   final TextureManager textureManager;

   public MapTextureManager(TextureManager textureManager) {
      this.textureManager = textureManager;
   }

   public void update(MapId id, MapItemSavedData data) {
      this.getOrCreateMapInstance(id, data).forceUpload();
   }

   public Identifier prepareMapTexture(MapId id, MapItemSavedData data) {
      MapTextureManager.MapInstance mapInstance = this.getOrCreateMapInstance(id, data);
      mapInstance.updateTextureIfNeeded();
      return mapInstance.location;
   }

   public void resetData() {
      ObjectIterator var1 = this.maps.values().iterator();

      while (var1.hasNext()) {
         MapTextureManager.MapInstance mapInstance = (MapTextureManager.MapInstance)var1.next();
         mapInstance.close();
      }

      this.maps.clear();
   }

   private MapTextureManager.MapInstance getOrCreateMapInstance(MapId id, MapItemSavedData data) {
      return (MapTextureManager.MapInstance)this.maps.compute(id.id(), (integer, mapInstance) -> {
         if (mapInstance == null) {
            return new MapTextureManager.MapInstance(integer, data);
         } else {
            mapInstance.replaceMapData(data);
            return mapInstance;
         }
      });
   }

   @Override
   public void close() {
      this.resetData();
   }

   @Environment(EnvType.CLIENT)
   class MapInstance implements AutoCloseable {
      private MapItemSavedData data;
      private final DynamicTexture texture;
      private boolean requiresUpload = true;
      final Identifier location;

      MapInstance(final int id, final MapItemSavedData data) {
         this.data = data;
         this.texture = new DynamicTexture(() -> "Map " + id, 128, 128, true);
         this.location = Identifier.withDefaultNamespace("map/" + id);
         MapTextureManager.this.textureManager.register(this.location, this.texture);
      }

      void replaceMapData(MapItemSavedData data) {
         boolean bl = this.data != data;
         this.data = data;
         this.requiresUpload |= bl;
      }

      public void forceUpload() {
         this.requiresUpload = true;
      }

      void updateTextureIfNeeded() {
         if (this.requiresUpload) {
            NativeImage nativeImage = this.texture.getPixels();
            if (nativeImage != null) {
               for (int i = 0; i < 128; i++) {
                  for (int j = 0; j < 128; j++) {
                     int k = j + i * 128;
                     nativeImage.setPixel(j, i, MapColor.getColorFromPackedId(this.data.colors[k]));
                  }
               }
            }

            this.texture.upload();
            this.requiresUpload = false;
         }
      }

      @Override
      public void close() {
         this.texture.close();
      }
   }
}
