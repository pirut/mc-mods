package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class NameTagFeatureRenderer {
   public void render(SubmitNodeCollection p_439963_, MultiBufferSource.BufferSource p_435849_, Font p_433982_) {
      NameTagFeatureRenderer.Storage nametagfeaturerenderer$storage = p_439963_.getNameTagSubmits();
      nametagfeaturerenderer$storage.nameTagSubmitsSeethrough.sort(Comparator.comparing(SubmitNodeStorage.NameTagSubmit::distanceToCameraSq).reversed());

      for (SubmitNodeStorage.NameTagSubmit submitnodestorage$nametagsubmit : nametagfeaturerenderer$storage.nameTagSubmitsSeethrough) {
         p_433982_.drawInBatch(
            submitnodestorage$nametagsubmit.text(),
            submitnodestorage$nametagsubmit.x(),
            submitnodestorage$nametagsubmit.y(),
            submitnodestorage$nametagsubmit.color(),
            false,
            submitnodestorage$nametagsubmit.pose(),
            p_435849_,
            Font.DisplayMode.SEE_THROUGH,
            submitnodestorage$nametagsubmit.backgroundColor(),
            submitnodestorage$nametagsubmit.lightCoords()
         );
      }

      for (SubmitNodeStorage.NameTagSubmit submitnodestorage$nametagsubmit1 : nametagfeaturerenderer$storage.nameTagSubmitsNormal) {
         p_433982_.drawInBatch(
            submitnodestorage$nametagsubmit1.text(),
            submitnodestorage$nametagsubmit1.x(),
            submitnodestorage$nametagsubmit1.y(),
            submitnodestorage$nametagsubmit1.color(),
            false,
            submitnodestorage$nametagsubmit1.pose(),
            p_435849_,
            Font.DisplayMode.NORMAL,
            submitnodestorage$nametagsubmit1.backgroundColor(),
            submitnodestorage$nametagsubmit1.lightCoords()
         );
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Storage {
      final List<SubmitNodeStorage.NameTagSubmit> nameTagSubmitsSeethrough = new ArrayList<>();
      final List<SubmitNodeStorage.NameTagSubmit> nameTagSubmitsNormal = new ArrayList<>();

      public void add(
         PoseStack p_449100_,
         @Nullable Vec3 p_449479_,
         int p_449074_,
         Component p_449010_,
         boolean p_449529_,
         int p_451639_,
         double p_449896_,
         CameraRenderState p_451536_
      ) {
         if (p_449479_ != null) {
            Minecraft minecraft = Minecraft.getInstance();
            p_449100_.pushPose();
            p_449100_.translate(p_449479_.x, p_449479_.y + 0.5, p_449479_.z);
            p_449100_.mulPose(p_451536_.orientation);
            p_449100_.scale(0.025F, -0.025F, 0.025F);
            Matrix4f matrix4f = new Matrix4f(p_449100_.last().pose());
            float f = -minecraft.font.width(p_449010_) / 2.0F;
            int i = (int)(minecraft.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
            if (p_449529_) {
               this.nameTagSubmitsNormal
                  .add(
                     new SubmitNodeStorage.NameTagSubmit(
                        matrix4f, f, p_449074_, p_449010_, LightTexture.lightCoordsWithEmission(p_451639_, 2), -1, 0, p_449896_
                     )
                  );
               this.nameTagSubmitsSeethrough.add(new SubmitNodeStorage.NameTagSubmit(matrix4f, f, p_449074_, p_449010_, p_451639_, -2130706433, i, p_449896_));
            } else {
               this.nameTagSubmitsNormal.add(new SubmitNodeStorage.NameTagSubmit(matrix4f, f, p_449074_, p_449010_, p_451639_, -2130706433, i, p_449896_));
            }

            p_449100_.popPose();
         }
      }

      public void clear() {
         this.nameTagSubmitsNormal.clear();
         this.nameTagSubmitsSeethrough.clear();
      }
   }
}
