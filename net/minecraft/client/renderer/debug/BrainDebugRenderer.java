package net.minecraft.client.renderer.debug;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.network.protocol.common.custom.BrainDebugPayload;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class BrainDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final boolean SHOW_NAME_FOR_ALL = true;
   private static final boolean SHOW_PROFESSION_FOR_ALL = false;
   private static final boolean SHOW_BEHAVIORS_FOR_ALL = false;
   private static final boolean SHOW_ACTIVITIES_FOR_ALL = false;
   private static final boolean SHOW_INVENTORY_FOR_ALL = false;
   private static final boolean SHOW_GOSSIPS_FOR_ALL = false;
   private static final boolean SHOW_PATH_FOR_ALL = false;
   private static final boolean SHOW_HEALTH_FOR_ALL = false;
   private static final boolean SHOW_WANTS_GOLEM_FOR_ALL = true;
   private static final boolean SHOW_ANGER_LEVEL_FOR_ALL = false;
   private static final boolean SHOW_NAME_FOR_SELECTED = true;
   private static final boolean SHOW_PROFESSION_FOR_SELECTED = true;
   private static final boolean SHOW_BEHAVIORS_FOR_SELECTED = true;
   private static final boolean SHOW_ACTIVITIES_FOR_SELECTED = true;
   private static final boolean SHOW_MEMORIES_FOR_SELECTED = true;
   private static final boolean SHOW_INVENTORY_FOR_SELECTED = true;
   private static final boolean SHOW_GOSSIPS_FOR_SELECTED = true;
   private static final boolean SHOW_PATH_FOR_SELECTED = true;
   private static final boolean SHOW_HEALTH_FOR_SELECTED = true;
   private static final boolean SHOW_WANTS_GOLEM_FOR_SELECTED = true;
   private static final boolean SHOW_ANGER_LEVEL_FOR_SELECTED = true;
   private static final boolean SHOW_POI_INFO = true;
   private static final int MAX_RENDER_DIST_FOR_BRAIN_INFO = 30;
   private static final int MAX_RENDER_DIST_FOR_POI_INFO = 30;
   private static final int MAX_TARGETING_DIST = 8;
   private static final float TEXT_SCALE = 0.02F;
   private static final int WHITE = -1;
   private static final int YELLOW = -256;
   private static final int CYAN = -16711681;
   private static final int GREEN = -16711936;
   private static final int GRAY = -3355444;
   private static final int PINK = -98404;
   private static final int RED = -65536;
   private static final int ORANGE = -23296;
   private final Minecraft minecraft;
   private final Map<BlockPos, BrainDebugRenderer.PoiInfo> pois = Maps.newHashMap();
   private final Map<UUID, BrainDebugPayload.BrainDump> brainDumpsPerEntity = Maps.newHashMap();
   @Nullable
   private UUID lastLookedAtUuid;

   public BrainDebugRenderer(Minecraft p_113200_) {
      this.minecraft = p_113200_;
   }

   @Override
   public void clear() {
      this.pois.clear();
      this.brainDumpsPerEntity.clear();
      this.lastLookedAtUuid = null;
   }

   public void addPoi(BrainDebugRenderer.PoiInfo p_113227_) {
      this.pois.put(p_113227_.pos, p_113227_);
   }

   public void removePoi(BlockPos p_113229_) {
      this.pois.remove(p_113229_);
   }

   public void setFreeTicketCount(BlockPos p_113231_, int p_113232_) {
      BrainDebugRenderer.PoiInfo $$2 = this.pois.get(p_113231_);
      if ($$2 == null) {
         LOGGER.warn("Strange, setFreeTicketCount was called for an unknown POI: {}", p_113231_);
      } else {
         $$2.freeTicketCount = p_113232_;
      }
   }

   public void addOrUpdateBrainDump(BrainDebugPayload.BrainDump p_294334_) {
      this.brainDumpsPerEntity.put(p_294334_.uuid(), p_294334_);
   }

   public void removeBrainDump(int p_173811_) {
      this.brainDumpsPerEntity.values().removeIf(p_293654_ -> p_293654_.id() == p_173811_);
   }

   @Override
   public void render(PoseStack p_113214_, MultiBufferSource p_113215_, double p_113216_, double p_113217_, double p_113218_) {
      this.clearRemovedEntities();
      this.doRender(p_113214_, p_113215_, p_113216_, p_113217_, p_113218_);
      if (!this.minecraft.player.isSpectator()) {
         this.updateLastLookedAtUuid();
      }
   }

   private void clearRemovedEntities() {
      this.brainDumpsPerEntity.entrySet().removeIf(p_293652_ -> {
         Entity $$1 = this.minecraft.level.getEntity(p_293652_.getValue().id());
         return $$1 == null || $$1.isRemoved();
      });
   }

   private void doRender(PoseStack p_270747_, MultiBufferSource p_270289_, double p_270303_, double p_270416_, double p_270542_) {
      BlockPos $$5 = BlockPos.containing(p_270303_, p_270416_, p_270542_);
      this.brainDumpsPerEntity.values().forEach(p_293660_ -> {
         if (this.isPlayerCloseEnoughToMob(p_293660_)) {
            this.renderBrainInfo(p_270747_, p_270289_, p_293660_, p_270303_, p_270416_, p_270542_);
         }
      });

      for (BlockPos $$6 : this.pois.keySet()) {
         if ($$5.closerThan($$6, 30.0)) {
            highlightPoi(p_270747_, p_270289_, $$6);
         }
      }

      this.pois.values().forEach(p_269718_ -> {
         if ($$5.closerThan(p_269718_.pos, 30.0)) {
            this.renderPoiInfo(p_270747_, p_270289_, p_269718_);
         }
      });
      this.getGhostPois().forEach((p_269707_, p_269708_) -> {
         if ($$5.closerThan(p_269707_, 30.0)) {
            this.renderGhostPoi(p_270747_, p_270289_, p_269707_, (List<String>)p_269708_);
         }
      });
   }

   private static void highlightPoi(PoseStack p_270066_, MultiBufferSource p_270965_, BlockPos p_270159_) {
      float $$3 = 0.05F;
      DebugRenderer.renderFilledBox(p_270066_, p_270965_, p_270159_, 0.05F, 0.2F, 0.2F, 1.0F, 0.3F);
   }

   private void renderGhostPoi(PoseStack p_270206_, MultiBufferSource p_270976_, BlockPos p_270670_, List<String> p_270882_) {
      float $$4 = 0.05F;
      DebugRenderer.renderFilledBox(p_270206_, p_270976_, p_270670_, 0.05F, 0.2F, 0.2F, 1.0F, 0.3F);
      renderTextOverPos(p_270206_, p_270976_, p_270882_ + "", p_270670_, 0, -256);
      renderTextOverPos(p_270206_, p_270976_, "Ghost POI", p_270670_, 1, -65536);
   }

   private void renderPoiInfo(PoseStack p_270999_, MultiBufferSource p_270627_, BrainDebugRenderer.PoiInfo p_270986_) {
      int $$3 = 0;
      Set<String> $$4 = this.getTicketHolderNames(p_270986_);
      if ($$4.size() < 4) {
         renderTextOverPoi(p_270999_, p_270627_, "Owners: " + $$4, p_270986_, $$3, -256);
      } else {
         renderTextOverPoi(p_270999_, p_270627_, $$4.size() + " ticket holders", p_270986_, $$3, -256);
      }

      $$3++;
      Set<String> $$5 = this.getPotentialTicketHolderNames(p_270986_);
      if ($$5.size() < 4) {
         renderTextOverPoi(p_270999_, p_270627_, "Candidates: " + $$5, p_270986_, $$3, -23296);
      } else {
         renderTextOverPoi(p_270999_, p_270627_, $$5.size() + " potential owners", p_270986_, $$3, -23296);
      }

      renderTextOverPoi(p_270999_, p_270627_, "Free tickets: " + p_270986_.freeTicketCount, p_270986_, ++$$3, -256);
      renderTextOverPoi(p_270999_, p_270627_, p_270986_.type, p_270986_, ++$$3, -1);
   }

   private void renderPath(
      PoseStack p_270435_, MultiBufferSource p_270439_, BrainDebugPayload.BrainDump p_294227_, double p_270109_, double p_270342_, double p_270834_
   ) {
      if (p_294227_.path() != null) {
         PathfindingRenderer.renderPath(p_270435_, p_270439_, p_294227_.path(), 0.5F, false, false, p_270109_, p_270342_, p_270834_);
      }
   }

   private void renderBrainInfo(
      PoseStack p_270145_, MultiBufferSource p_270489_, BrainDebugPayload.BrainDump p_294452_, double p_270922_, double p_270468_, double p_270838_
   ) {
      boolean $$6 = this.isMobSelected(p_294452_);
      int $$7 = 0;
      renderTextOverMob(p_270145_, p_270489_, p_294452_.pos(), $$7, p_294452_.name(), -1, 0.03F);
      $$7++;
      if ($$6) {
         renderTextOverMob(p_270145_, p_270489_, p_294452_.pos(), $$7, p_294452_.profession() + " " + p_294452_.xp() + " xp", -1, 0.02F);
         $$7++;
      }

      if ($$6) {
         int $$8 = p_294452_.health() < p_294452_.maxHealth() ? -23296 : -1;
         renderTextOverMob(
            p_270145_,
            p_270489_,
            p_294452_.pos(),
            $$7,
            "health: " + String.format(Locale.ROOT, "%.1f", p_294452_.health()) + " / " + String.format(Locale.ROOT, "%.1f", p_294452_.maxHealth()),
            $$8,
            0.02F
         );
         $$7++;
      }

      if ($$6 && !p_294452_.inventory().equals("")) {
         renderTextOverMob(p_270145_, p_270489_, p_294452_.pos(), $$7, p_294452_.inventory(), -98404, 0.02F);
         $$7++;
      }

      if ($$6) {
         for (String $$9 : p_294452_.behaviors()) {
            renderTextOverMob(p_270145_, p_270489_, p_294452_.pos(), $$7, $$9, -16711681, 0.02F);
            $$7++;
         }
      }

      if ($$6) {
         for (String $$10 : p_294452_.activities()) {
            renderTextOverMob(p_270145_, p_270489_, p_294452_.pos(), $$7, $$10, -16711936, 0.02F);
            $$7++;
         }
      }

      if (p_294452_.wantsGolem()) {
         renderTextOverMob(p_270145_, p_270489_, p_294452_.pos(), $$7, "Wants Golem", -23296, 0.02F);
         $$7++;
      }

      if ($$6 && p_294452_.angerLevel() != -1) {
         renderTextOverMob(p_270145_, p_270489_, p_294452_.pos(), $$7, "Anger Level: " + p_294452_.angerLevel(), -98404, 0.02F);
         $$7++;
      }

      if ($$6) {
         for (String $$11 : p_294452_.gossips()) {
            if ($$11.startsWith(p_294452_.name())) {
               renderTextOverMob(p_270145_, p_270489_, p_294452_.pos(), $$7, $$11, -1, 0.02F);
            } else {
               renderTextOverMob(p_270145_, p_270489_, p_294452_.pos(), $$7, $$11, -23296, 0.02F);
            }

            $$7++;
         }
      }

      if ($$6) {
         for (String $$12 : Lists.reverse(p_294452_.memories())) {
            renderTextOverMob(p_270145_, p_270489_, p_294452_.pos(), $$7, $$12, -3355444, 0.02F);
            $$7++;
         }
      }

      if ($$6) {
         this.renderPath(p_270145_, p_270489_, p_294452_, p_270922_, p_270468_, p_270838_);
      }
   }

   private static void renderTextOverPoi(
      PoseStack p_270498_, MultiBufferSource p_270609_, String p_270070_, BrainDebugRenderer.PoiInfo p_270677_, int p_270143_, int p_271011_
   ) {
      renderTextOverPos(p_270498_, p_270609_, p_270070_, p_270677_.pos, p_270143_, p_271011_);
   }

   private static void renderTextOverPos(PoseStack p_270640_, MultiBufferSource p_270809_, String p_270632_, BlockPos p_270082_, int p_270078_, int p_270440_) {
      double $$6 = 1.3;
      double $$7 = 0.2;
      double $$8 = p_270082_.getX() + 0.5;
      double $$9 = p_270082_.getY() + 1.3 + p_270078_ * 0.2;
      double $$10 = p_270082_.getZ() + 0.5;
      DebugRenderer.renderFloatingText(p_270640_, p_270809_, p_270632_, $$8, $$9, $$10, p_270440_, 0.02F, true, 0.0F, true);
   }

   private static void renderTextOverMob(
      PoseStack p_270664_, MultiBufferSource p_270816_, Position p_270715_, int p_270126_, String p_270487_, int p_270218_, float p_270737_
   ) {
      double $$7 = 2.4;
      double $$8 = 0.25;
      BlockPos $$9 = BlockPos.containing(p_270715_);
      double $$10 = $$9.getX() + 0.5;
      double $$11 = p_270715_.y() + 2.4 + p_270126_ * 0.25;
      double $$12 = $$9.getZ() + 0.5;
      float $$13 = 0.5F;
      DebugRenderer.renderFloatingText(p_270664_, p_270816_, p_270487_, $$10, $$11, $$12, p_270218_, p_270737_, false, 0.5F, true);
   }

   private Set<String> getTicketHolderNames(BrainDebugRenderer.PoiInfo p_113283_) {
      return this.getTicketHolders(p_113283_.pos).stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet());
   }

   private Set<String> getPotentialTicketHolderNames(BrainDebugRenderer.PoiInfo p_113288_) {
      return this.getPotentialTicketHolders(p_113288_.pos).stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet());
   }

   private boolean isMobSelected(BrainDebugPayload.BrainDump p_296487_) {
      return Objects.equals(this.lastLookedAtUuid, p_296487_.uuid());
   }

   private boolean isPlayerCloseEnoughToMob(BrainDebugPayload.BrainDump p_296011_) {
      Player $$1 = this.minecraft.player;
      BlockPos $$2 = BlockPos.containing($$1.getX(), p_296011_.pos().y(), $$1.getZ());
      BlockPos $$3 = BlockPos.containing(p_296011_.pos());
      return $$2.closerThan($$3, 30.0);
   }

   private Collection<UUID> getTicketHolders(BlockPos p_113285_) {
      return this.brainDumpsPerEntity
         .values()
         .stream()
         .filter(p_293662_ -> p_293662_.hasPoi(p_113285_))
         .map(BrainDebugPayload.BrainDump::uuid)
         .collect(Collectors.toSet());
   }

   private Collection<UUID> getPotentialTicketHolders(BlockPos p_113290_) {
      return this.brainDumpsPerEntity
         .values()
         .stream()
         .filter(p_293664_ -> p_293664_.hasPotentialPoi(p_113290_))
         .map(BrainDebugPayload.BrainDump::uuid)
         .collect(Collectors.toSet());
   }

   private Map<BlockPos, List<String>> getGhostPois() {
      Map<BlockPos, List<String>> $$0 = Maps.newHashMap();

      for (BrainDebugPayload.BrainDump $$1 : this.brainDumpsPerEntity.values()) {
         for (BlockPos $$2 : Iterables.concat($$1.pois(), $$1.potentialPois())) {
            if (!this.pois.containsKey($$2)) {
               $$0.computeIfAbsent($$2, p_113292_ -> Lists.newArrayList()).add($$1.name());
            }
         }
      }

      return $$0;
   }

   private void updateLastLookedAtUuid() {
      DebugRenderer.getTargetedEntity(this.minecraft.getCameraEntity(), 8).ifPresent(p_113212_ -> this.lastLookedAtUuid = p_113212_.getUUID());
   }

   @OnlyIn(Dist.CLIENT)
   public static class PoiInfo {
      public final BlockPos pos;
      public final String type;
      public int freeTicketCount;

      public PoiInfo(BlockPos p_113337_, String p_113338_, int p_113339_) {
         this.pos = p_113337_;
         this.type = p_113338_;
         this.freeTicketCount = p_113339_;
      }
   }
}
