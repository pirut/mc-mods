package net.minecraft.client.renderer.debug;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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

@Environment(EnvType.CLIENT)
public class PoiDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final int MAX_RENDER_DIST_FOR_POI_INFO = 30;
   private static final float TEXT_SCALE = 0.32F;
   private static final int ORANGE = -23296;
   private final BrainDebugRenderer brainRenderer;

   public PoiDebugRenderer(BrainDebugRenderer brainRenderer) {
      this.brainRenderer = brainRenderer;
   }

   @Override
   public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValueAccess, Frustum frustum, float partialTick) {
      BlockPos blockPos = BlockPos.containing(camX, camY, camZ);
      debugValueAccess.forEachBlock(DebugSubscriptions.POIS, (blockPos2, debugPoiInfo) -> {
         if (blockPos.closerThan(blockPos2, 30.0)) {
            highlightPoi(blockPos2);
            this.renderPoiInfo(debugPoiInfo, debugValueAccess);
         }
      });
      this.brainRenderer.getGhostPois(debugValueAccess).forEach((blockPos2, list) -> {
         if (debugValueAccess.getBlockValue(DebugSubscriptions.POIS, blockPos2) == null) {
            if (blockPos.closerThan(blockPos2, 30.0)) {
               this.renderGhostPoi(blockPos2, (List<String>)list);
            }
         }
      });
   }

   private static void highlightPoi(BlockPos pos) {
      float f = 0.05F;
      Gizmos.cuboid(pos, 0.05F, GizmoStyle.fill(ARGB.colorFromFloat(0.3F, 0.2F, 0.2F, 1.0F)));
   }

   private void renderGhostPoi(BlockPos pos, List<String> entities) {
      float f = 0.05F;
      Gizmos.cuboid(pos, 0.05F, GizmoStyle.fill(ARGB.colorFromFloat(0.3F, 0.2F, 0.2F, 1.0F)));
      Gizmos.billboardTextOverBlock(entities.toString(), pos, 0, -256, 0.32F);
      Gizmos.billboardTextOverBlock("Ghost POI", pos, 1, -65536, 0.32F);
   }

   private void renderPoiInfo(DebugPoiInfo poiInfo, DebugValueAccess debugValueAccess) {
      int i = 0;
      if (SharedConstants.DEBUG_BRAIN) {
         List<String> list = this.getTicketHolderNames(poiInfo, false, debugValueAccess);
         if (list.size() < 4) {
            renderTextOverPoi("Owners: " + list, poiInfo, i, -256);
         } else {
            renderTextOverPoi(list.size() + " ticket holders", poiInfo, i, -256);
         }

         i++;
         List<String> list2 = this.getTicketHolderNames(poiInfo, true, debugValueAccess);
         if (list2.size() < 4) {
            renderTextOverPoi("Candidates: " + list2, poiInfo, i, -23296);
         } else {
            renderTextOverPoi(list2.size() + " potential owners", poiInfo, i, -23296);
         }

         i++;
      }

      renderTextOverPoi("Free tickets: " + poiInfo.freeTicketCount(), poiInfo, i, -256);
      renderTextOverPoi(poiInfo.poiType().getRegisteredName(), poiInfo, ++i, -1);
   }

   private static void renderTextOverPoi(String text, DebugPoiInfo poiInfo, int line, int color) {
      Gizmos.billboardTextOverBlock(text, poiInfo.pos(), line, color, 0.32F);
   }

   private List<String> getTicketHolderNames(DebugPoiInfo poiInfo, boolean potential, DebugValueAccess debugValueAccess) {
      List<String> list = new ArrayList<>();
      debugValueAccess.forEachEntity(DebugSubscriptions.BRAINS, (entity, debugBrainDump) -> {
         boolean bl2 = potential ? debugBrainDump.hasPotentialPoi(poiInfo.pos()) : debugBrainDump.hasPoi(poiInfo.pos());
         if (bl2) {
            list.add(DebugEntityNameGenerator.getEntityName(entity.getUUID()));
         }
      });
      return list;
   }
}
