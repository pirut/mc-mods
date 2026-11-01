package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class HeightMapRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private static final int CHUNK_DIST = 2;
   private static final float BOX_HEIGHT = 0.09375F;

   public HeightMapRenderer(Minecraft p_113572_) {
      this.minecraft = p_113572_;
   }

   @Override
   public void render(PoseStack p_113576_, MultiBufferSource p_113577_, double p_113578_, double p_113579_, double p_113580_) {
      LevelAccessor $$5 = this.minecraft.level;
      VertexConsumer $$6 = p_113577_.getBuffer(RenderType.debugFilledBox());
      BlockPos $$7 = BlockPos.containing(p_113578_, 0.0, p_113580_);

      for (int $$8 = -2; $$8 <= 2; $$8++) {
         for (int $$9 = -2; $$9 <= 2; $$9++) {
            ChunkAccess $$10 = $$5.getChunk($$7.offset($$8 * 16, 0, $$9 * 16));

            for (Entry<Heightmap.Types, Heightmap> $$11 : $$10.getHeightmaps()) {
               Heightmap.Types $$12 = $$11.getKey();
               ChunkPos $$13 = $$10.getPos();
               Vector3f $$14 = this.getColor($$12);

               for (int $$15 = 0; $$15 < 16; $$15++) {
                  for (int $$16 = 0; $$16 < 16; $$16++) {
                     int $$17 = SectionPos.sectionToBlockCoord($$13.x, $$15);
                     int $$18 = SectionPos.sectionToBlockCoord($$13.z, $$16);
                     float $$19 = (float)($$5.getHeight($$12, $$17, $$18) + $$12.ordinal() * 0.09375F - p_113579_);
                     LevelRenderer.addChainedFilledBoxVertices(
                        p_113576_,
                        $$6,
                        $$17 + 0.25F - p_113578_,
                        (double)$$19,
                        $$18 + 0.25F - p_113580_,
                        $$17 + 0.75F - p_113578_,
                        (double)($$19 + 0.09375F),
                        $$18 + 0.75F - p_113580_,
                        $$14.x(),
                        $$14.y(),
                        $$14.z(),
                        1.0F
                     );
                  }
               }
            }
         }
      }
   }

   private Vector3f getColor(Heightmap.Types p_113574_) {
      return switch (p_113574_) {
         case WORLD_SURFACE_WG -> new Vector3f(1.0F, 1.0F, 0.0F);
         case OCEAN_FLOOR_WG -> new Vector3f(1.0F, 0.0F, 1.0F);
         case WORLD_SURFACE -> new Vector3f(0.0F, 0.7F, 0.0F);
         case OCEAN_FLOOR -> new Vector3f(0.0F, 0.0F, 0.5F);
         case MOTION_BLOCKING -> new Vector3f(0.0F, 0.3F, 0.3F);
         case MOTION_BLOCKING_NO_LEAVES -> new Vector3f(0.0F, 0.5F, 0.5F);
      };
   }
}
