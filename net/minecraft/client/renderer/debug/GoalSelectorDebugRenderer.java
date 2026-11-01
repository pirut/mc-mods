package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.GoalDebugPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GoalSelectorDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final int MAX_RENDER_DIST = 160;
   private final Minecraft minecraft;
   private final Int2ObjectMap<GoalSelectorDebugRenderer.EntityGoalInfo> goalSelectors = new Int2ObjectOpenHashMap();

   @Override
   public void clear() {
      this.goalSelectors.clear();
   }

   public void addGoalSelector(int p_113549_, BlockPos p_295031_, List<GoalDebugPayload.DebugGoal> p_113550_) {
      this.goalSelectors.put(p_113549_, new GoalSelectorDebugRenderer.EntityGoalInfo(p_295031_, p_113550_));
   }

   public void removeGoalSelector(int p_173889_) {
      this.goalSelectors.remove(p_173889_);
   }

   public GoalSelectorDebugRenderer(Minecraft p_113546_) {
      this.minecraft = p_113546_;
   }

   @Override
   public void render(PoseStack p_113552_, MultiBufferSource p_113553_, double p_113554_, double p_113555_, double p_113556_) {
      Camera $$5 = this.minecraft.gameRenderer.getMainCamera();
      BlockPos $$6 = BlockPos.containing($$5.getPosition().x, 0.0, $$5.getPosition().z);
      ObjectIterator var11 = this.goalSelectors.values().iterator();

      while (var11.hasNext()) {
         GoalSelectorDebugRenderer.EntityGoalInfo $$7 = (GoalSelectorDebugRenderer.EntityGoalInfo)var11.next();
         BlockPos $$8 = $$7.entityPos;
         if ($$6.closerThan($$8, 160.0)) {
            for (int $$9 = 0; $$9 < $$7.goals.size(); $$9++) {
               GoalDebugPayload.DebugGoal $$10 = $$7.goals.get($$9);
               double $$11 = $$8.getX() + 0.5;
               double $$12 = $$8.getY() + 2.0 + $$9 * 0.25;
               double $$13 = $$8.getZ() + 0.5;
               int $$14 = $$10.isRunning() ? -16711936 : -3355444;
               DebugRenderer.renderFloatingText(p_113552_, p_113553_, $$10.name(), $$11, $$12, $$13, $$14);
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   record EntityGoalInfo(BlockPos entityPos, List<GoalDebugPayload.DebugGoal> goals) {
   }
}
