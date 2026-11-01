package net.minecraft.client.renderer.debug;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GameEventListenerRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private static final int LISTENER_RENDER_DIST = 32;
   private static final float BOX_HEIGHT = 1.0F;
   private final List<GameEventListenerRenderer.TrackedGameEvent> trackedGameEvents = Lists.newArrayList();
   private final List<GameEventListenerRenderer.TrackedListener> trackedListeners = Lists.newArrayList();

   public GameEventListenerRenderer(Minecraft p_173822_) {
      this.minecraft = p_173822_;
   }

   @Override
   public void render(PoseStack p_173846_, MultiBufferSource p_173847_, double p_173848_, double p_173849_, double p_173850_) {
      Level $$5 = this.minecraft.level;
      if ($$5 == null) {
         this.trackedGameEvents.clear();
         this.trackedListeners.clear();
      } else {
         Vec3 $$6 = new Vec3(p_173848_, 0.0, p_173850_);
         this.trackedGameEvents.removeIf(GameEventListenerRenderer.TrackedGameEvent::isExpired);
         this.trackedListeners.removeIf(p_234512_ -> p_234512_.isExpired($$5, $$6));
         VertexConsumer $$7 = p_173847_.getBuffer(RenderType.lines());

         for (GameEventListenerRenderer.TrackedListener $$8 : this.trackedListeners) {
            $$8.getPosition($$5)
               .ifPresent(
                  p_269731_ -> {
                     double $$7x = p_269731_.x() - $$8.getListenerRadius();
                     double $$8x = p_269731_.y() - $$8.getListenerRadius();
                     double $$9 = p_269731_.z() - $$8.getListenerRadius();
                     double $$10 = p_269731_.x() + $$8.getListenerRadius();
                     double $$11 = p_269731_.y() + $$8.getListenerRadius();
                     double $$12x = p_269731_.z() + $$8.getListenerRadius();
                     LevelRenderer.renderVoxelShape(
                        p_173846_,
                        $$7,
                        Shapes.create(new AABB($$7x, $$8x, $$9, $$10, $$11, $$12x)),
                        -p_173848_,
                        -p_173849_,
                        -p_173850_,
                        1.0F,
                        1.0F,
                        0.0F,
                        0.35F,
                        true
                     );
                  }
               );
         }

         VertexConsumer $$9 = p_173847_.getBuffer(RenderType.debugFilledBox());

         for (GameEventListenerRenderer.TrackedListener $$10 : this.trackedListeners) {
            $$10.getPosition($$5)
               .ifPresent(
                  p_269724_ -> LevelRenderer.addChainedFilledBoxVertices(
                     p_173846_,
                     $$9,
                     p_269724_.x() - 0.25 - p_173848_,
                     p_269724_.y() - p_173849_,
                     p_269724_.z() - 0.25 - p_173850_,
                     p_269724_.x() + 0.25 - p_173848_,
                     p_269724_.y() - p_173849_ + 1.0,
                     p_269724_.z() + 0.25 - p_173850_,
                     1.0F,
                     1.0F,
                     0.0F,
                     0.35F
                  )
               );
         }

         for (GameEventListenerRenderer.TrackedListener $$11 : this.trackedListeners) {
            $$11.getPosition($$5)
               .ifPresent(
                  p_274713_ -> {
                     DebugRenderer.renderFloatingText(p_173846_, p_173847_, "Listener Origin", p_274713_.x(), p_274713_.y() + 1.8F, p_274713_.z(), -1, 0.025F);
                     DebugRenderer.renderFloatingText(
                        p_173846_, p_173847_, BlockPos.containing(p_274713_).toString(), p_274713_.x(), p_274713_.y() + 1.5, p_274713_.z(), -6959665, 0.025F
                     );
                  }
               );
         }

         for (GameEventListenerRenderer.TrackedGameEvent $$12 : this.trackedGameEvents) {
            Vec3 $$13 = $$12.position;
            double $$14 = 0.2F;
            double $$15 = $$13.x - 0.2F;
            double $$16 = $$13.y - 0.2F;
            double $$17 = $$13.z - 0.2F;
            double $$18 = $$13.x + 0.2F;
            double $$19 = $$13.y + 0.2F + 0.5;
            double $$20 = $$13.z + 0.2F;
            renderFilledBox(p_173846_, p_173847_, new AABB($$15, $$16, $$17, $$18, $$19, $$20), 1.0F, 1.0F, 1.0F, 0.2F);
            DebugRenderer.renderFloatingText(p_173846_, p_173847_, $$12.gameEvent.location().toString(), $$13.x, $$13.y + 0.85F, $$13.z, -7564911, 0.0075F);
         }
      }
   }

   private static void renderFilledBox(
      PoseStack p_270351_, MultiBufferSource p_270763_, AABB p_270205_, float p_270707_, float p_270538_, float p_270314_, float p_270966_
   ) {
      Camera $$7 = Minecraft.getInstance().gameRenderer.getMainCamera();
      if ($$7.isInitialized()) {
         Vec3 $$8 = $$7.getPosition().reverse();
         DebugRenderer.renderFilledBox(p_270351_, p_270763_, p_270205_.move($$8), p_270707_, p_270538_, p_270314_, p_270966_);
      }
   }

   public void trackGameEvent(ResourceKey<GameEvent> p_294637_, Vec3 p_234515_) {
      this.trackedGameEvents.add(new GameEventListenerRenderer.TrackedGameEvent(Util.getMillis(), p_294637_, p_234515_));
   }

   public void trackListener(PositionSource p_173831_, int p_173832_) {
      this.trackedListeners.add(new GameEventListenerRenderer.TrackedListener(p_173831_, p_173832_));
   }

   @OnlyIn(Dist.CLIENT)
   record TrackedGameEvent(long timeStamp, ResourceKey<GameEvent> gameEvent, Vec3 position) {
      public boolean isExpired() {
         return Util.getMillis() - this.timeStamp > 3000L;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class TrackedListener implements GameEventListener {
      public final PositionSource listenerSource;
      public final int listenerRange;

      public TrackedListener(PositionSource p_173872_, int p_173873_) {
         this.listenerSource = p_173872_;
         this.listenerRange = p_173873_;
      }

      public boolean isExpired(Level p_234543_, Vec3 p_234544_) {
         return this.listenerSource.getPosition(p_234543_).filter(p_234547_ -> p_234547_.distanceToSqr(p_234544_) <= 1024.0).isPresent();
      }

      public Optional<Vec3> getPosition(Level p_173876_) {
         return this.listenerSource.getPosition(p_173876_);
      }

      @Override
      public PositionSource getListenerSource() {
         return this.listenerSource;
      }

      @Override
      public int getListenerRadius() {
         return this.listenerRange;
      }

      @Override
      public boolean handleGameEvent(ServerLevel p_234540_, Holder<GameEvent> p_316503_, GameEvent.Context p_250285_, Vec3 p_250758_) {
         return false;
      }
   }
}
