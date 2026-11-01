package net.neoforged.neoforge.client;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class RenderTypeHelper {
   public static RenderType getEntityRenderType(ChunkSectionLayer chunkSectionLayer) {
      return chunkSectionLayer != ChunkSectionLayer.TRANSLUCENT ? Sheets.cutoutBlockSheet() : Sheets.translucentBlockItemSheet();
   }

   public static RenderType getMovingBlockRenderType(ChunkSectionLayer chunkSectionLayer) {
      return switch (chunkSectionLayer) {
         case SOLID -> RenderTypes.solidMovingBlock();
         case CUTOUT -> RenderTypes.cutoutMovingBlock();
         case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
         case TRIPWIRE -> RenderTypes.tripwireMovingBlock();
      };
   }

   public static Function<ItemStack, RenderType> detectItemModelRenderType(List<BakedQuad> quads, RenderTypeGroup renderTypes) {
      Iterator<BakedQuad> iterator = quads.iterator();
      if (!iterator.hasNext()) {
         return $ -> renderTypes.entityItem();
      } else {
         Identifier atlasId = iterator.next().sprite().atlasLocation();

         while (iterator.hasNext()) {
            Identifier currAtlasId = iterator.next().sprite().atlasLocation();
            if (!currAtlasId.equals(atlasId)) {
               throw new IllegalStateException("Multiple atlases used in model, expected " + atlasId + ", but also got " + currAtlasId);
            }
         }

         if (atlasId.equals(TextureAtlas.LOCATION_ITEMS)) {
            return $ -> renderTypes.entityItem();
         } else if (atlasId.equals(TextureAtlas.LOCATION_BLOCKS)) {
            return $ -> renderTypes.entityBlock();
         } else {
            throw new IllegalArgumentException("Atlas " + atlasId + " can't be used for item models");
         }
      }
   }

   private RenderTypeHelper() {
   }
}
