package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class SolidFaceRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;

   public SolidFaceRenderer(Minecraft p_113668_) {
      this.minecraft = p_113668_;
   }

   @Override
   public void render(PoseStack p_113670_, MultiBufferSource p_113671_, double p_113672_, double p_113673_, double p_113674_) {
      Matrix4f $$5 = p_113670_.last().pose();
      BlockGetter $$6 = this.minecraft.player.level();
      BlockPos $$7 = BlockPos.containing(p_113672_, p_113673_, p_113674_);

      for (BlockPos $$8 : BlockPos.betweenClosed($$7.offset(-6, -6, -6), $$7.offset(6, 6, 6))) {
         BlockState $$9 = $$6.getBlockState($$8);
         if (!$$9.is(Blocks.AIR)) {
            VoxelShape $$10 = $$9.getShape($$6, $$8);

            for (AABB $$11 : $$10.toAabbs()) {
               AABB $$12 = $$11.move($$8).inflate(0.002);
               float $$13 = (float)($$12.minX - p_113672_);
               float $$14 = (float)($$12.minY - p_113673_);
               float $$15 = (float)($$12.minZ - p_113674_);
               float $$16 = (float)($$12.maxX - p_113672_);
               float $$17 = (float)($$12.maxY - p_113673_);
               float $$18 = (float)($$12.maxZ - p_113674_);
               int $$19 = -2130771968;
               if ($$9.isFaceSturdy($$6, $$8, Direction.WEST)) {
                  VertexConsumer $$20 = p_113671_.getBuffer(RenderType.debugFilledBox());
                  $$20.addVertex($$5, $$13, $$14, $$15).setColor(-2130771968);
                  $$20.addVertex($$5, $$13, $$14, $$18).setColor(-2130771968);
                  $$20.addVertex($$5, $$13, $$17, $$15).setColor(-2130771968);
                  $$20.addVertex($$5, $$13, $$17, $$18).setColor(-2130771968);
               }

               if ($$9.isFaceSturdy($$6, $$8, Direction.SOUTH)) {
                  VertexConsumer $$21 = p_113671_.getBuffer(RenderType.debugFilledBox());
                  $$21.addVertex($$5, $$13, $$17, $$18).setColor(-2130771968);
                  $$21.addVertex($$5, $$13, $$14, $$18).setColor(-2130771968);
                  $$21.addVertex($$5, $$16, $$17, $$18).setColor(-2130771968);
                  $$21.addVertex($$5, $$16, $$14, $$18).setColor(-2130771968);
               }

               if ($$9.isFaceSturdy($$6, $$8, Direction.EAST)) {
                  VertexConsumer $$22 = p_113671_.getBuffer(RenderType.debugFilledBox());
                  $$22.addVertex($$5, $$16, $$14, $$18).setColor(-2130771968);
                  $$22.addVertex($$5, $$16, $$14, $$15).setColor(-2130771968);
                  $$22.addVertex($$5, $$16, $$17, $$18).setColor(-2130771968);
                  $$22.addVertex($$5, $$16, $$17, $$15).setColor(-2130771968);
               }

               if ($$9.isFaceSturdy($$6, $$8, Direction.NORTH)) {
                  VertexConsumer $$23 = p_113671_.getBuffer(RenderType.debugFilledBox());
                  $$23.addVertex($$5, $$16, $$17, $$15).setColor(-2130771968);
                  $$23.addVertex($$5, $$16, $$14, $$15).setColor(-2130771968);
                  $$23.addVertex($$5, $$13, $$17, $$15).setColor(-2130771968);
                  $$23.addVertex($$5, $$13, $$14, $$15).setColor(-2130771968);
               }

               if ($$9.isFaceSturdy($$6, $$8, Direction.DOWN)) {
                  VertexConsumer $$24 = p_113671_.getBuffer(RenderType.debugFilledBox());
                  $$24.addVertex($$5, $$13, $$14, $$15).setColor(-2130771968);
                  $$24.addVertex($$5, $$16, $$14, $$15).setColor(-2130771968);
                  $$24.addVertex($$5, $$13, $$14, $$18).setColor(-2130771968);
                  $$24.addVertex($$5, $$16, $$14, $$18).setColor(-2130771968);
               }

               if ($$9.isFaceSturdy($$6, $$8, Direction.UP)) {
                  VertexConsumer $$25 = p_113671_.getBuffer(RenderType.debugFilledBox());
                  $$25.addVertex($$5, $$13, $$17, $$15).setColor(-2130771968);
                  $$25.addVertex($$5, $$13, $$17, $$18).setColor(-2130771968);
                  $$25.addVertex($$5, $$16, $$17, $$15).setColor(-2130771968);
                  $$25.addVertex($$5, $$16, $$17, $$18).setColor(-2130771968);
               }
            }
         }
      }
   }
}
