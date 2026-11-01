package net.neoforged.neoforge.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.function.Function;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

public record RenderTypeGroup(ChunkSectionLayer block, RenderType entityBlock, RenderType entityItem) {
   public static RenderTypeGroup EMPTY = new RenderTypeGroup(null, null, null);

   public RenderTypeGroup(ChunkSectionLayer block, RenderType entityBlock, RenderType entityItem) {
      if (block == null == (entityBlock == null) && block == null == (entityItem == null)) {
         if (entityBlock == null || entityBlock.format() == DefaultVertexFormat.NEW_ENTITY && entityItem.format() == DefaultVertexFormat.NEW_ENTITY) {
            this.block = block;
            this.entityBlock = entityBlock;
            this.entityItem = entityItem;
         } else {
            throw new IllegalArgumentException("The entity render types must use DefaultVertexFormat.NEW_ENTITY.");
         }
      } else {
         throw new IllegalArgumentException("The render types in a group must either be all null, or all non-null.");
      }
   }

   public RenderTypeGroup(ChunkSectionLayer block, Function<Identifier, RenderType> entity) {
      this(block, entity.apply(TextureAtlas.LOCATION_BLOCKS), entity.apply(TextureAtlas.LOCATION_ITEMS));
   }

   public boolean isEmpty() {
      return this.block == null;
   }
}
