package net.minecraft.client.renderer.debug;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugPoiInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PoiDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final int MAX_RENDER_DIST_FOR_POI_INFO = 30;
   private static final float TEXT_SCALE = 0.32F;
   private static final int ORANGE = -23296;
   private final BrainDebugRenderer brainRenderer;

   public PoiDebugRenderer(BrainDebugRenderer p_449865_) {
      this.brainRenderer = p_449865_;
   }

   @Override
   public void emitGizmos(double p_455929_, double p_455458_, double p_456088_, DebugValueAccess p_455495_, Frustum p_455636_, float p_454695_) {
      BlockPos blockpos = BlockPos.containing(p_455929_, p_455458_, p_456088_);
      p_455495_.forEachBlock(DebugSubscriptions.POIS, (p_454341_, p_454342_) -> {
         if (blockpos.closerThan(p_454341_, 30.0)) {
            highlightPoi(p_454341_);
            this.renderPoiInfo(p_454342_, p_455495_);
         }
      });
      this.brainRenderer.getGhostPois(p_455495_).forEach((p_454337_, p_454338_) -> {
         if (p_455495_.getBlockValue(DebugSubscriptions.POIS, p_454337_) == null && blockpos.closerThan(p_454337_, 30.0)) {
            this.renderGhostPoi(p_454337_, (List<String>)p_454338_);
         }
      });
   }

   private static void highlightPoi(BlockPos p_449612_) {
      float f = 0.05F;
      Gizmos.cuboid(p_449612_, 0.05F, GizmoStyle.fill(ARGB.colorFromFloat(0.3F, 0.2F, 0.2F, 1.0F)));
   }

   private void renderGhostPoi(BlockPos p_449645_, List<String> p_449363_) {
      float f = 0.05F;
      Gizmos.cuboid(p_449645_, 0.05F, GizmoStyle.fill(ARGB.colorFromFloat(0.3F, 0.2F, 0.2F, 1.0F)));
      Gizmos.billboardTextOverBlock(p_449363_.toString(), p_449645_, 0, -256, 0.32F);
      Gizmos.billboardTextOverBlock("Ghost POI", p_449645_, 1, -65536, 0.32F);
   }

   private void renderPoiInfo(DebugPoiInfo p_449783_, DebugValueAccess p_449701_) {
      int i = 0;
      if (SharedConstants.DEBUG_BRAIN) {
         List<String> list = this.getTicketHolderNames(p_449783_, false, p_449701_);
         if (list.size() < 4) {
            renderTextOverPoi("Owners: " + list, p_449783_, i, -256);
         } else {
            renderTextOverPoi(list.size() + " ticket holders", p_449783_, i, -256);
         }

         i++;
         List<String> list1 = this.getTicketHolderNames(p_449783_, true, p_449701_);
         if (list1.size() < 4) {
            renderTextOverPoi("Candidates: " + list1, p_449783_, i, -23296);
         } else {
            renderTextOverPoi(list1.size() + " potential owners", p_449783_, i, -23296);
         }

         i++;
      }

      renderTextOverPoi("Free tickets: " + p_449783_.freeTicketCount(), p_449783_, i, -256);
      renderTextOverPoi(p_449783_.poiType().getRegisteredName(), p_449783_, ++i, -1);
   }

   private static void renderTextOverPoi(String p_449904_, DebugPoiInfo p_449586_, int p_449337_, int p_449958_) {
      Gizmos.billboardTextOverBlock(p_449904_, p_449586_.pos(), p_449337_, p_449958_, 0.32F);
   }

   private List<String> getTicketHolderNames(DebugPoiInfo p_449370_, boolean p_449543_, DebugValueAccess p_449887_) {
      List<String> list = new ArrayList<>();
      p_449887_.forEachEntity(DebugSubscriptions.BRAINS, (p_449314_, p_449925_) -> {
         boolean flag = p_449543_ ? p_449925_.hasPotentialPoi(p_449370_.pos()) : p_449925_.hasPoi(p_449370_.pos());
         if (flag) {
            list.add(DebugEntityNameGenerator.getEntityName(p_449314_.getUUID()));
         }
      });
      return list;
   }
}
