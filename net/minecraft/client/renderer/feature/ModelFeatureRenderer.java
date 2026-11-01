package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.SharedConstants;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.ModelBakery;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ModelFeatureRenderer {
   private final PoseStack poseStack = new PoseStack();

   public void render(
      SubmitNodeCollection p_440688_, MultiBufferSource.BufferSource p_440021_, OutlineBufferSource p_439307_, MultiBufferSource.BufferSource p_440318_
   ) {
      ModelFeatureRenderer.Storage modelfeaturerenderer$storage = p_440688_.getModelSubmits();
      this.renderBatch(p_440021_, p_439307_, modelfeaturerenderer$storage.opaqueModelSubmits, p_440318_);
      modelfeaturerenderer$storage.translucentModelSubmits.sort(Comparator.comparingDouble(p_439003_ -> -p_439003_.position().lengthSquared()));
      this.renderTranslucents(p_440021_, p_439307_, modelfeaturerenderer$storage.translucentModelSubmits, p_440318_);
   }

   private void renderTranslucents(
      MultiBufferSource.BufferSource p_439004_,
      OutlineBufferSource p_439192_,
      List<SubmitNodeStorage.TranslucentModelSubmit<?>> p_439940_,
      MultiBufferSource.BufferSource p_440658_
   ) {
      for (SubmitNodeStorage.TranslucentModelSubmit<?> translucentmodelsubmit : p_439940_) {
         this.renderModel(
            translucentmodelsubmit.modelSubmit(),
            translucentmodelsubmit.renderType(),
            p_439004_.getBuffer(translucentmodelsubmit.renderType()),
            p_439192_,
            p_440658_
         );
      }
   }

   private void renderBatch(
      MultiBufferSource.BufferSource p_440096_,
      OutlineBufferSource p_439188_,
      Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> p_440629_,
      MultiBufferSource.BufferSource p_440563_
   ) {
      Iterable<Entry<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>>> iterable;
      if (SharedConstants.DEBUG_SHUFFLE_MODELS) {
         List<Entry<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>>> list = new ArrayList<>(p_440629_.entrySet());
         Collections.shuffle(list);
         iterable = list;
      } else {
         iterable = p_440629_.entrySet();
      }

      for (Entry<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> entry : iterable) {
         VertexConsumer vertexconsumer = p_440096_.getBuffer(entry.getKey());

         for (SubmitNodeStorage.ModelSubmit<?> modelsubmit : entry.getValue()) {
            this.renderModel(modelsubmit, entry.getKey(), vertexconsumer, p_439188_, p_440563_);
         }
      }
   }

   private <S> void renderModel(
      SubmitNodeStorage.ModelSubmit<S> p_440177_,
      RenderType p_458958_,
      VertexConsumer p_439575_,
      OutlineBufferSource p_439507_,
      MultiBufferSource.BufferSource p_439974_
   ) {
      this.poseStack.pushPose();
      this.poseStack.last().set(p_440177_.pose());
      Model<? super S> model = p_440177_.model();
      VertexConsumer vertexconsumer = p_440177_.sprite() == null ? p_439575_ : p_440177_.sprite().wrap(p_439575_);
      model.setupAnim(p_440177_.state());
      model.renderToBuffer(this.poseStack, vertexconsumer, p_440177_.lightCoords(), p_440177_.overlayCoords(), p_440177_.tintedColor());
      if (p_440177_.outlineColor() != 0 && (p_458958_.outline().isPresent() || p_458958_.isOutline())) {
         p_439507_.setColor(p_440177_.outlineColor());
         VertexConsumer vertexconsumer1 = p_439507_.getBuffer(p_458958_);
         model.renderToBuffer(
            this.poseStack,
            p_440177_.sprite() == null ? vertexconsumer1 : p_440177_.sprite().wrap(vertexconsumer1),
            p_440177_.lightCoords(),
            p_440177_.overlayCoords(),
            p_440177_.tintedColor()
         );
      }

      if (p_440177_.crumblingOverlay() != null && p_458958_.affectsCrumbling()) {
         VertexConsumer vertexconsumer2 = new SheetedDecalTextureGenerator(
            p_439974_.getBuffer(ModelBakery.DESTROY_TYPES.get(p_440177_.crumblingOverlay().progress())), p_440177_.crumblingOverlay().cameraPose(), 1.0F
         );
         model.renderToBuffer(
            this.poseStack,
            p_440177_.sprite() == null ? vertexconsumer2 : p_440177_.sprite().wrap(vertexconsumer2),
            p_440177_.lightCoords(),
            p_440177_.overlayCoords(),
            p_440177_.tintedColor()
         );
      }

      this.poseStack.popPose();
   }

   @OnlyIn(Dist.CLIENT)
   public record CrumblingOverlay(int progress, PoseStack.Pose cameraPose) {
   }

   @OnlyIn(Dist.CLIENT)
   public static class Storage {
      final Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> opaqueModelSubmits = new HashMap<>();
      final List<SubmitNodeStorage.TranslucentModelSubmit<?>> translucentModelSubmits = new ArrayList<>();
      private final Set<RenderType> usedModelSubmitBuckets = new ObjectOpenHashSet();

      public void add(RenderType p_459000_, SubmitNodeStorage.ModelSubmit<?> p_449574_) {
         if (p_459000_.pipeline().getBlendFunction().isEmpty()) {
            this.opaqueModelSubmits.computeIfAbsent(p_459000_, p_458950_ -> new ArrayList<>()).add(p_449574_);
         } else {
            Vector3f vector3f = p_449574_.pose().pose().transformPosition(new Vector3f());
            this.translucentModelSubmits.add(new SubmitNodeStorage.TranslucentModelSubmit<>(p_449574_, p_459000_, vector3f));
         }
      }

      public void clear() {
         this.translucentModelSubmits.clear();

         for (Entry<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> entry : this.opaqueModelSubmits.entrySet()) {
            List<SubmitNodeStorage.ModelSubmit<?>> list = entry.getValue();
            if (!list.isEmpty()) {
               this.usedModelSubmitBuckets.add(entry.getKey());
               list.clear();
            }
         }
      }

      public void endFrame() {
         this.opaqueModelSubmits.keySet().removeIf(p_459156_ -> !this.usedModelSubmitBuckets.contains(p_459156_));
         this.usedModelSubmitBuckets.clear();
      }
   }
}
