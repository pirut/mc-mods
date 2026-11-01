package net.minecraft.client.renderer;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

// $VF: synthetic class
@OnlyIn(Dist.CLIENT)
class ItemBlockRenderTypes$2 {
   static {
      try {
         $SwitchMap$net$minecraft$client$renderer$chunk$ChunkSectionLayer[ChunkSectionLayer.SOLID.ordinal()] = 1;
      } catch (NoSuchFieldError var4) {
      }

      try {
         $SwitchMap$net$minecraft$client$renderer$chunk$ChunkSectionLayer[ChunkSectionLayer.CUTOUT.ordinal()] = 2;
      } catch (NoSuchFieldError var3) {
      }

      try {
         $SwitchMap$net$minecraft$client$renderer$chunk$ChunkSectionLayer[ChunkSectionLayer.TRANSLUCENT.ordinal()] = 3;
      } catch (NoSuchFieldError var2) {
      }

      try {
         $SwitchMap$net$minecraft$client$renderer$chunk$ChunkSectionLayer[ChunkSectionLayer.TRIPWIRE.ordinal()] = 4;
      } catch (NoSuchFieldError var1) {
      }
   }
}
