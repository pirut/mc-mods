package net.minecraft.client.resources;

import java.io.IOException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.DryFoliageColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DryFoliageColorReloadListener extends SimplePreparableReloadListener<int[]> {
   private static final Identifier LOCATION = Identifier.withDefaultNamespace("textures/colormap/dry_foliage.png");

   protected int[] prepare(ResourceManager p_406344_, ProfilerFiller p_406283_) {
      try {
         return LegacyStuffWrapper.getPixels(p_406344_, LOCATION);
      } catch (IOException var4) {
         throw new IllegalStateException("Failed to load dry foliage color texture", var4);
      }
   }

   protected void apply(int[] p_406287_, ResourceManager p_406260_, ProfilerFiller p_406269_) {
      DryFoliageColor.init(p_406287_);
   }
}
