package net.minecraft.client.renderer.debug;

import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NeighborsUpdateRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private final Map<Long, Map<BlockPos, Integer>> lastUpdate = Maps.newTreeMap(Ordering.natural().reverse());

   NeighborsUpdateRenderer(Minecraft p_113595_) {
      this.minecraft = p_113595_;
   }

   public void addUpdate(long p_113597_, BlockPos p_113598_) {
      Map<BlockPos, Integer> $$2 = this.lastUpdate.computeIfAbsent(p_113597_, p_113606_ -> Maps.newHashMap());
      int $$3 = $$2.getOrDefault(p_113598_, 0);
      $$2.put(p_113598_, $$3 + 1);
   }

   @Override
   public void render(PoseStack p_113600_, MultiBufferSource p_113601_, double p_113602_, double p_113603_, double p_113604_) {
      long $$5 = this.minecraft.level.getGameTime();
      int $$6 = 200;
      double $$7 = 0.0025;
      Set<BlockPos> $$8 = Sets.newHashSet();
      Map<BlockPos, Integer> $$9 = Maps.newHashMap();
      VertexConsumer $$10 = p_113601_.getBuffer(RenderType.lines());
      Iterator<Entry<Long, Map<BlockPos, Integer>>> $$11 = this.lastUpdate.entrySet().iterator();

      while ($$11.hasNext()) {
         Entry<Long, Map<BlockPos, Integer>> $$12 = $$11.next();
         Long $$13 = $$12.getKey();
         Map<BlockPos, Integer> $$14 = $$12.getValue();
         long $$15 = $$5 - $$13;
         if ($$15 > 200L) {
            $$11.remove();
         } else {
            for (Entry<BlockPos, Integer> $$16 : $$14.entrySet()) {
               BlockPos $$17 = $$16.getKey();
               Integer $$18 = $$16.getValue();
               if ($$8.add($$17)) {
                  AABB $$19 = new AABB(BlockPos.ZERO)
                     .inflate(0.002)
                     .deflate(0.0025 * $$15)
                     .move($$17.getX(), $$17.getY(), $$17.getZ())
                     .move(-p_113602_, -p_113603_, -p_113604_);
                  LevelRenderer.renderLineBox(p_113600_, $$10, $$19.minX, $$19.minY, $$19.minZ, $$19.maxX, $$19.maxY, $$19.maxZ, 1.0F, 1.0F, 1.0F, 1.0F);
                  $$9.put($$17, $$18);
               }
            }
         }
      }

      for (Entry<BlockPos, Integer> $$20 : $$9.entrySet()) {
         BlockPos $$21 = $$20.getKey();
         Integer $$22 = $$20.getValue();
         DebugRenderer.renderFloatingText(p_113600_, p_113601_, String.valueOf($$22), $$21.getX(), $$21.getY(), $$21.getZ(), -1);
      }
   }
}
