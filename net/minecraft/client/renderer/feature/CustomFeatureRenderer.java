package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CustomFeatureRenderer {
   public void render(SubmitNodeCollection p_439835_, MultiBufferSource.BufferSource p_433110_) {
      CustomFeatureRenderer.Storage customfeaturerenderer$storage = p_439835_.getCustomGeometrySubmits();

      for (Entry<RenderType, List<SubmitNodeStorage.CustomGeometrySubmit>> entry : customfeaturerenderer$storage.customGeometrySubmits.entrySet()) {
         VertexConsumer vertexconsumer = p_433110_.getBuffer(entry.getKey());

         for (SubmitNodeStorage.CustomGeometrySubmit submitnodestorage$customgeometrysubmit : entry.getValue()) {
            submitnodestorage$customgeometrysubmit.customGeometryRenderer().render(submitnodestorage$customgeometrysubmit.pose(), vertexconsumer);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Storage {
      final Map<RenderType, List<SubmitNodeStorage.CustomGeometrySubmit>> customGeometrySubmits = new HashMap<>();
      private final Set<RenderType> customGeometrySubmitsUsage = new ObjectOpenHashSet();

      public void add(PoseStack p_449489_, RenderType p_459005_, SubmitNodeCollector.CustomGeometryRenderer p_449261_) {
         List<SubmitNodeStorage.CustomGeometrySubmit> list = this.customGeometrySubmits.computeIfAbsent(p_459005_, p_458959_ -> new ArrayList<>());
         list.add(new SubmitNodeStorage.CustomGeometrySubmit(p_449489_.last().copy(), p_449261_));
      }

      public void clear() {
         for (Entry<RenderType, List<SubmitNodeStorage.CustomGeometrySubmit>> entry : this.customGeometrySubmits.entrySet()) {
            if (!entry.getValue().isEmpty()) {
               this.customGeometrySubmitsUsage.add(entry.getKey());
               entry.getValue().clear();
            }
         }
      }

      public void endFrame() {
         this.customGeometrySubmits.keySet().removeIf(p_459161_ -> !this.customGeometrySubmitsUsage.contains(p_459161_));
         this.customGeometrySubmitsUsage.clear();
      }
   }
}
