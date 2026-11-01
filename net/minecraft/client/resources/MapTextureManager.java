package net.minecraft.client.resources;

import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MapTextureManager implements AutoCloseable {
   private final Int2ObjectMap<MapTextureManager.MapInstance> maps = new Int2ObjectOpenHashMap();
   final TextureManager textureManager;

   public MapTextureManager(TextureManager p_364440_) {
      this.textureManager = p_364440_;
   }

   public void update(MapId p_360802_, MapItemSavedData p_362966_) {
      this.getOrCreateMapInstance(p_360802_, p_362966_).forceUpload();
   }

   public Identifier prepareMapTexture(MapId p_364272_, MapItemSavedData p_364454_) {
      MapTextureManager.MapInstance maptexturemanager$mapinstance = this.getOrCreateMapInstance(p_364272_, p_364454_);
      maptexturemanager$mapinstance.updateTextureIfNeeded();
      return maptexturemanager$mapinstance.location;
   }

   public void resetData() {
      ObjectIterator var1 = this.maps.values().iterator();

      while (var1.hasNext()) {
         MapTextureManager.MapInstance maptexturemanager$mapinstance = (MapTextureManager.MapInstance)var1.next();
         maptexturemanager$mapinstance.close();
      }

      this.maps.clear();
   }

   private MapTextureManager.MapInstance getOrCreateMapInstance(MapId p_360665_, MapItemSavedData p_363475_) {
      return (MapTextureManager.MapInstance)this.maps.compute(p_360665_.id(), (p_362953_, p_362729_) -> {
         if (p_362729_ == null) {
            return new MapTextureManager.MapInstance(p_362953_, p_363475_);
         } else {
            p_362729_.replaceMapData(p_363475_);
            return p_362729_;
         }
      });
   }

   @Override
   public void close() {
      this.resetData();
   }

   @OnlyIn(Dist.CLIENT)
   class MapInstance implements AutoCloseable {
      private MapItemSavedData data;
      private final DynamicTexture texture;
      private boolean requiresUpload = true;
      final Identifier location;

      MapInstance(int p_362268_, MapItemSavedData p_362707_) {
         this.data = p_362707_;
         this.texture = new DynamicTexture(() -> "Map " + p_362268_, 128, 128, true);
         this.location = Identifier.withDefaultNamespace("map/" + p_362268_);
         MapTextureManager.this.textureManager.register(this.location, this.texture);
      }

      void replaceMapData(MapItemSavedData p_361304_) {
         boolean flag = this.data != p_361304_;
         this.data = p_361304_;
         this.requiresUpload |= flag;
      }

      public void forceUpload() {
         this.requiresUpload = true;
      }

      void updateTextureIfNeeded() {
         if (this.requiresUpload) {
            NativeImage nativeimage = this.texture.getPixels();
            if (nativeimage != null) {
               for (int i = 0; i < 128; i++) {
                  for (int j = 0; j < 128; j++) {
                     int k = j + i * 128;
                     nativeimage.setPixel(j, i, MapColor.getColorFromPackedId(this.data.colors[k]));
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
