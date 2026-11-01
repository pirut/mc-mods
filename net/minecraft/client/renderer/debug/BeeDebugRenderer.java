package net.minecraft.client.renderer.debug;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugBeeInfo;
import net.minecraft.util.debug.DebugGoalInfo;
import net.minecraft.util.debug.DebugHiveInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BeeDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final boolean SHOW_GOAL_FOR_ALL_BEES = true;
   private static final boolean SHOW_NAME_FOR_ALL_BEES = true;
   private static final boolean SHOW_HIVE_FOR_ALL_BEES = true;
   private static final boolean SHOW_FLOWER_POS_FOR_ALL_BEES = true;
   private static final boolean SHOW_TRAVEL_TICKS_FOR_ALL_BEES = true;
   private static final boolean SHOW_GOAL_FOR_SELECTED_BEE = true;
   private static final boolean SHOW_NAME_FOR_SELECTED_BEE = true;
   private static final boolean SHOW_HIVE_FOR_SELECTED_BEE = true;
   private static final boolean SHOW_FLOWER_POS_FOR_SELECTED_BEE = true;
   private static final boolean SHOW_TRAVEL_TICKS_FOR_SELECTED_BEE = true;
   private static final boolean SHOW_HIVE_MEMBERS = true;
   private static final boolean SHOW_BLACKLISTS = true;
   private static final int MAX_RENDER_DIST_FOR_HIVE_OVERLAY = 30;
   private static final int MAX_RENDER_DIST_FOR_BEE_OVERLAY = 30;
   private static final int MAX_TARGETING_DIST = 8;
   private static final float TEXT_SCALE = 0.32F;
   private static final int ORANGE = -23296;
   private static final int GRAY = -3355444;
   private static final int PINK = -98404;
   private final Minecraft minecraft;
   @Nullable
   private UUID lastLookedAtUuid;

   public BeeDebugRenderer(Minecraft p_113053_) {
      this.minecraft = p_113053_;
   }

   @Override
   public void emitGizmos(double p_456220_, double p_455610_, double p_455534_, DebugValueAccess p_455739_, Frustum p_454831_, float p_455624_) {
      this.doRender(p_455739_);
      if (!this.minecraft.player.isSpectator()) {
         this.updateLastLookedAtUuid();
      }
   }

   private void doRender(DebugValueAccess p_449775_) {
      BlockPos blockpos = this.getCamera().blockPosition();
      p_449775_.forEachEntity(DebugSubscriptions.BEES, (p_454269_, p_454270_) -> {
         if (this.minecraft.player.closerThan(p_454269_, 30.0)) {
            DebugGoalInfo debuggoalinfo = p_449775_.getEntityValue(DebugSubscriptions.GOAL_SELECTORS, p_454269_);
            this.renderBeeInfo(p_454269_, p_454270_, debuggoalinfo);
         }
      });
      this.renderFlowerInfos(p_449775_);
      Map<BlockPos, Set<UUID>> map = this.createHiveBlacklistMap(p_449775_);
      p_449775_.forEachBlock(DebugSubscriptions.BEE_HIVES, (p_454279_, p_454280_) -> {
         if (blockpos.closerThan(p_454279_, 30.0)) {
            highlightHive(p_454279_);
            Set<UUID> set = map.getOrDefault(p_454279_, Set.of());
            this.renderHiveInfo(p_454279_, p_454280_, set, p_449775_);
         }
      });
      this.getGhostHives(p_449775_).forEach((p_454272_, p_454273_) -> {
         if (blockpos.closerThan(p_454272_, 30.0)) {
            this.renderGhostHive(p_454272_, (List<String>)p_454273_);
         }
      });
   }

   private Map<BlockPos, Set<UUID>> createHiveBlacklistMap(DebugValueAccess p_449155_) {
      Map<BlockPos, Set<UUID>> map = new HashMap<>();
      p_449155_.forEachEntity(DebugSubscriptions.BEES, (p_448716_, p_448717_) -> {
         for (BlockPos blockpos : p_448717_.blacklistedHives()) {
            map.computeIfAbsent(blockpos, p_293649_ -> new HashSet<>()).add(p_448716_.getUUID());
         }
      });
      return map;
   }

   private void renderFlowerInfos(DebugValueAccess p_449201_) {
      Map<BlockPos, Set<UUID>> map = new HashMap<>();
      p_449201_.forEachEntity(DebugSubscriptions.BEES, (p_448743_, p_448744_) -> {
         if (p_448744_.flowerPos().isPresent()) {
            map.computeIfAbsent(p_448744_.flowerPos().get(), p_448745_ -> new HashSet<>()).add(p_448743_.getUUID());
         }
      });
      map.forEach((p_454274_, p_454275_) -> {
         Set<String> set = p_454275_.stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet());
         int i = 1;
         Gizmos.billboardTextOverBlock(set.toString(), p_454274_, i++, -256, 0.32F);
         Gizmos.billboardTextOverBlock("Flower", p_454274_, i++, -1, 0.32F);
         Gizmos.cuboid(p_454274_, 0.05F, GizmoStyle.fill(ARGB.colorFromFloat(0.3F, 0.8F, 0.8F, 0.0F)));
      });
   }

   private static String getBeeUuidsAsString(Collection<UUID> p_113116_) {
      if (p_113116_.isEmpty()) {
         return "-";
      } else {
         return p_113116_.size() > 3
            ? p_113116_.size() + " bees"
            : p_113116_.stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet()).toString();
      }
   }

   private static void highlightHive(BlockPos p_270687_) {
      float f = 0.05F;
      Gizmos.cuboid(p_270687_, 0.05F, GizmoStyle.fill(ARGB.colorFromFloat(0.3F, 0.2F, 0.2F, 1.0F)));
   }

   private void renderGhostHive(BlockPos p_270550_, List<String> p_270221_) {
      float f = 0.05F;
      Gizmos.cuboid(p_270550_, 0.05F, GizmoStyle.fill(ARGB.colorFromFloat(0.3F, 0.2F, 0.2F, 1.0F)));
      Gizmos.billboardTextOverBlock(p_270221_.toString(), p_270550_, 0, -256, 0.32F);
      Gizmos.billboardTextOverBlock("Ghost Hive", p_270550_, 1, -65536, 0.32F);
   }

   private void renderHiveInfo(BlockPos p_449019_, DebugHiveInfo p_449520_, Collection<UUID> p_270946_, DebugValueAccess p_449273_) {
      int i = 0;
      if (!p_270946_.isEmpty()) {
         renderTextOverHive("Blacklisted by " + getBeeUuidsAsString(p_270946_), p_449019_, i++, -65536);
      }

      renderTextOverHive("Out: " + getBeeUuidsAsString(this.getHiveMembers(p_449019_, p_449273_)), p_449019_, i++, -3355444);
      if (p_449520_.occupantCount() == 0) {
         renderTextOverHive("In: -", p_449019_, i++, -256);
      } else if (p_449520_.occupantCount() == 1) {
         renderTextOverHive("In: 1 bee", p_449019_, i++, -256);
      } else {
         renderTextOverHive("In: " + p_449520_.occupantCount() + " bees", p_449019_, i++, -256);
      }

      renderTextOverHive("Honey: " + p_449520_.honeyLevel(), p_449019_, i++, -23296);
      renderTextOverHive(p_449520_.type().getName().getString() + (p_449520_.sedated() ? " (sedated)" : ""), p_449019_, i++, -1);
   }

   private void renderBeeInfo(Entity p_449500_, DebugBeeInfo p_449581_, @Nullable DebugGoalInfo p_449315_) {
      boolean flag = this.isBeeSelected(p_449500_);
      int i = 0;
      Gizmos.billboardTextOverMob(p_449500_, i++, p_449581_.toString(), -1, 0.48F);
      if (p_449581_.hivePos().isEmpty()) {
         Gizmos.billboardTextOverMob(p_449500_, i++, "No hive", -98404, 0.32F);
      } else {
         Gizmos.billboardTextOverMob(p_449500_, i++, "Hive: " + this.getPosDescription(p_449500_, p_449581_.hivePos().get()), -256, 0.32F);
      }

      if (p_449581_.flowerPos().isEmpty()) {
         Gizmos.billboardTextOverMob(p_449500_, i++, "No flower", -98404, 0.32F);
      } else {
         Gizmos.billboardTextOverMob(p_449500_, i++, "Flower: " + this.getPosDescription(p_449500_, p_449581_.flowerPos().get()), -256, 0.32F);
      }

      if (p_449315_ != null) {
         for (DebugGoalInfo.DebugGoal debuggoalinfo$debuggoal : p_449315_.goals()) {
            if (debuggoalinfo$debuggoal.isRunning()) {
               Gizmos.billboardTextOverMob(p_449500_, i++, debuggoalinfo$debuggoal.name(), -16711936, 0.32F);
            }
         }
      }

      if (p_449581_.travelTicks() > 0) {
         int j = p_449581_.travelTicks() < 2400 ? -3355444 : -23296;
         Gizmos.billboardTextOverMob(p_449500_, i++, "Travelling: " + p_449581_.travelTicks() + " ticks", j, 0.32F);
      }
   }

   private static void renderTextOverHive(String p_270119_, BlockPos p_449763_, int p_270930_, int p_270094_) {
      Gizmos.billboardTextOverBlock(p_270119_, p_449763_, p_270930_, p_270094_, 0.32F);
   }

   private Camera getCamera() {
      return this.minecraft.gameRenderer.getMainCamera();
   }

   private String getPosDescription(Entity p_449942_, BlockPos p_113070_) {
      double d0 = p_113070_.distToCenterSqr(p_449942_.position());
      double d1 = Math.round(d0 * 10.0) / 10.0;
      return p_113070_.toShortString() + " (dist " + d1 + ")";
   }

   private boolean isBeeSelected(Entity p_449481_) {
      return Objects.equals(this.lastLookedAtUuid, p_449481_.getUUID());
   }

   private Collection<UUID> getHiveMembers(BlockPos p_113130_, DebugValueAccess p_449875_) {
      Set<UUID> set = new HashSet<>();
      p_449875_.forEachEntity(DebugSubscriptions.BEES, (p_448725_, p_448726_) -> {
         if (p_448726_.hasHive(p_113130_)) {
            set.add(p_448725_.getUUID());
         }
      });
      return set;
   }

   private Map<BlockPos, List<String>> getGhostHives(DebugValueAccess p_449456_) {
      Map<BlockPos, List<String>> map = new HashMap<>();
      p_449456_.forEachEntity(DebugSubscriptions.BEES, (p_448736_, p_448737_) -> {
         if (p_448737_.hivePos().isPresent() && p_449456_.getBlockValue(DebugSubscriptions.BEE_HIVES, p_448737_.hivePos().get()) == null) {
            map.computeIfAbsent(p_448737_.hivePos().get(), p_113140_ -> Lists.newArrayList()).add(DebugEntityNameGenerator.getEntityName(p_448736_));
         }
      });
      return map;
   }

   private void updateLastLookedAtUuid() {
      DebugRenderer.getTargetedEntity(this.minecraft.getCameraEntity(), 8).ifPresent(p_113059_ -> this.lastLookedAtUuid = p_113059_.getUUID());
   }
}
