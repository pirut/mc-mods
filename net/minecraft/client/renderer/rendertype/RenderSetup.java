package net.minecraft.client.renderer.rendertype;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class RenderSetup {
   final RenderPipeline pipeline;
   final Map<String, RenderSetup.TextureBinding> textures;
   final TextureTransform textureTransform;
   final OutputTarget outputTarget;
   final RenderSetup.OutlineProperty outlineProperty;
   final boolean useLightmap;
   final boolean useOverlay;
   final boolean affectsCrumbling;
   final boolean sortOnUpload;
   final int bufferSize;
   final LayeringTransform layeringTransform;

   RenderSetup(
      RenderPipeline p_459062_,
      Map<String, RenderSetup.TextureBinding> p_459163_,
      boolean p_459076_,
      boolean p_458981_,
      LayeringTransform p_458941_,
      OutputTarget p_459215_,
      TextureTransform p_459057_,
      RenderSetup.OutlineProperty p_459015_,
      boolean p_459149_,
      boolean p_459142_,
      int p_459004_
   ) {
      this.pipeline = p_459062_;
      this.textures = p_459163_;
      this.outputTarget = p_459215_;
      this.textureTransform = p_459057_;
      this.useLightmap = p_459076_;
      this.useOverlay = p_458981_;
      this.outlineProperty = p_459015_;
      this.layeringTransform = p_458941_;
      this.affectsCrumbling = p_459149_;
      this.sortOnUpload = p_459142_;
      this.bufferSize = p_459004_;
   }

   @Override
   public String toString() {
      return "RenderSetup[layeringTransform="
         + this.layeringTransform
         + ", textureTransform="
         + this.textureTransform
         + ", textures="
         + this.textures
         + ", outlineProperty="
         + this.outlineProperty
         + ", useLightmap="
         + this.useLightmap
         + ", useOverlay="
         + this.useOverlay
         + "]";
   }

   public static RenderSetup.RenderSetupBuilder builder(RenderPipeline p_459213_) {
      return new RenderSetup.RenderSetupBuilder(p_459213_);
   }

   public Map<String, RenderSetup.TextureAndSampler> getTextures() {
      if (this.textures.isEmpty() && !this.useOverlay && !this.useLightmap) {
         return Collections.emptyMap();
      } else {
         Map<String, RenderSetup.TextureAndSampler> map = new HashMap<>();
         if (this.useOverlay) {
            map.put(
               "Sampler1",
               new RenderSetup.TextureAndSampler(
                  Minecraft.getInstance().gameRenderer.overlayTexture().getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
               )
            );
         }

         if (this.useLightmap) {
            map.put(
               "Sampler2",
               new RenderSetup.TextureAndSampler(
                  Minecraft.getInstance().gameRenderer.lightTexture().getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
               )
            );
         }

         TextureManager texturemanager = Minecraft.getInstance().getTextureManager();

         for (Entry<String, RenderSetup.TextureBinding> entry : this.textures.entrySet()) {
            AbstractTexture abstracttexture = texturemanager.getTexture(entry.getValue().location);
            GpuSampler gpusampler = entry.getValue().sampler().get();
            map.put(
               entry.getKey(),
               new RenderSetup.TextureAndSampler(abstracttexture.getTextureView(), gpusampler != null ? gpusampler : abstracttexture.getSampler())
            );
         }

         return map;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static enum OutlineProperty {
      NONE("none"),
      IS_OUTLINE("is_outline"),
      AFFECTS_OUTLINE("affects_outline");

      private final String name;

      private OutlineProperty(String p_458908_) {
         this.name = p_458908_;
      }

      @Override
      public String toString() {
         return this.name;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class RenderSetupBuilder {
      private final RenderPipeline pipeline;
      private boolean useLightmap = false;
      private boolean useOverlay = false;
      private LayeringTransform layeringTransform = LayeringTransform.NO_LAYERING;
      private OutputTarget outputTarget = OutputTarget.MAIN_TARGET;
      private TextureTransform textureTransform = TextureTransform.DEFAULT_TEXTURING;
      private boolean affectsCrumbling = false;
      private boolean sortOnUpload = false;
      private int bufferSize = 1536;
      private RenderSetup.OutlineProperty outlineProperty = RenderSetup.OutlineProperty.NONE;
      private final Map<String, RenderSetup.TextureBinding> textures = new HashMap<>();

      RenderSetupBuilder(RenderPipeline p_458942_) {
         this.pipeline = p_458942_;
      }

      public RenderSetup.RenderSetupBuilder withTexture(String p_458918_, Identifier p_469927_) {
         this.textures.put(p_458918_, new RenderSetup.TextureBinding(p_469927_, () -> null));
         return this;
      }

      public RenderSetup.RenderSetupBuilder withTexture(String p_469693_, Identifier p_466960_, @Nullable Supplier<GpuSampler> p_468969_) {
         this.textures.put(p_469693_, new RenderSetup.TextureBinding(p_466960_, Suppliers.memoize(() -> p_468969_ == null ? null : p_468969_.get())));
         return this;
      }

      public RenderSetup.RenderSetupBuilder useLightmap() {
         this.useLightmap = true;
         return this;
      }

      public RenderSetup.RenderSetupBuilder useOverlay() {
         this.useOverlay = true;
         return this;
      }

      public RenderSetup.RenderSetupBuilder affectsCrumbling() {
         this.affectsCrumbling = true;
         return this;
      }

      public RenderSetup.RenderSetupBuilder sortOnUpload() {
         this.sortOnUpload = true;
         return this;
      }

      public RenderSetup.RenderSetupBuilder bufferSize(int p_459211_) {
         this.bufferSize = p_459211_;
         return this;
      }

      public RenderSetup.RenderSetupBuilder setLayeringTransform(LayeringTransform p_459087_) {
         this.layeringTransform = p_459087_;
         return this;
      }

      public RenderSetup.RenderSetupBuilder setOutputTarget(OutputTarget p_458954_) {
         this.outputTarget = p_458954_;
         return this;
      }

      public RenderSetup.RenderSetupBuilder setTextureTransform(TextureTransform p_458963_) {
         this.textureTransform = p_458963_;
         return this;
      }

      public RenderSetup.RenderSetupBuilder setOutline(RenderSetup.OutlineProperty p_459114_) {
         this.outlineProperty = p_459114_;
         return this;
      }

      public RenderSetup createRenderSetup() {
         return new RenderSetup(
            this.pipeline,
            this.textures,
            this.useLightmap,
            this.useOverlay,
            this.layeringTransform,
            this.outputTarget,
            this.textureTransform,
            this.outlineProperty,
            this.affectsCrumbling,
            this.sortOnUpload,
            this.bufferSize
         );
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record TextureAndSampler(GpuTextureView textureView, GpuSampler sampler) {
   }

   @OnlyIn(Dist.CLIENT)
   record TextureBinding(Identifier location, Supplier<GpuSampler> sampler) {
   }
}
