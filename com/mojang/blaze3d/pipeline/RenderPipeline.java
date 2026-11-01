package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.LogicOp;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.stencil.StencilTest;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class RenderPipeline {
   private final Identifier location;
   private final Identifier vertexShader;
   private final Identifier fragmentShader;
   private final ShaderDefines shaderDefines;
   private final List<String> samplers;
   private final List<RenderPipeline.UniformDescription> uniforms;
   private final DepthTestFunction depthTestFunction;
   private final PolygonMode polygonMode;
   private final boolean cull;
   private final LogicOp colorLogic;
   private final Optional<BlendFunction> blendFunction;
   private final boolean writeColor;
   private final boolean writeAlpha;
   private final boolean writeDepth;
   private final VertexFormat vertexFormat;
   private final VertexFormat.Mode vertexFormatMode;
   private final float depthBiasScaleFactor;
   private final float depthBiasConstant;
   private final int sortKey;
   private static int sortKeySeed;
   private final Optional<StencilTest> stencilTest;

   @Deprecated
   protected RenderPipeline(
      Identifier p_467434_,
      Identifier p_467154_,
      Identifier p_467193_,
      ShaderDefines p_405426_,
      List<String> p_405334_,
      List<RenderPipeline.UniformDescription> p_404693_,
      Optional<BlendFunction> p_404667_,
      DepthTestFunction p_410224_,
      PolygonMode p_409822_,
      boolean p_404907_,
      boolean p_405688_,
      boolean p_405411_,
      boolean p_405477_,
      LogicOp p_410051_,
      VertexFormat p_405109_,
      VertexFormat.Mode p_404708_,
      float p_410755_,
      float p_409999_,
      int p_415862_
   ) {
      this(
         p_467434_,
         p_467154_,
         p_467193_,
         p_405426_,
         p_405334_,
         p_404693_,
         p_404667_,
         p_410224_,
         p_409822_,
         p_404907_,
         p_405688_,
         p_405411_,
         p_405477_,
         p_410051_,
         p_405109_,
         p_404708_,
         p_410755_,
         p_409999_,
         p_415862_,
         Optional.empty()
      );
   }

   protected RenderPipeline(
      Identifier p_405403_,
      Identifier p_405389_,
      Identifier p_404945_,
      ShaderDefines p_405426_,
      List<String> p_405334_,
      List<RenderPipeline.UniformDescription> p_404693_,
      Optional<BlendFunction> p_404667_,
      DepthTestFunction p_410224_,
      PolygonMode p_409822_,
      boolean p_404907_,
      boolean p_405688_,
      boolean p_405411_,
      boolean p_405477_,
      LogicOp p_410051_,
      VertexFormat p_405109_,
      VertexFormat.Mode p_404708_,
      float p_410755_,
      float p_409999_,
      int p_415862_,
      Optional<StencilTest> stencilTest
   ) {
      this.location = p_405403_;
      this.vertexShader = p_405389_;
      this.fragmentShader = p_404945_;
      this.shaderDefines = p_405426_;
      this.samplers = p_405334_;
      this.uniforms = p_404693_;
      this.depthTestFunction = p_410224_;
      this.polygonMode = p_409822_;
      this.cull = p_404907_;
      this.blendFunction = p_404667_;
      this.writeColor = p_405688_;
      this.writeAlpha = p_405411_;
      this.writeDepth = p_405477_;
      this.colorLogic = p_410051_;
      this.vertexFormat = p_405109_;
      this.vertexFormatMode = p_404708_;
      this.depthBiasScaleFactor = p_410755_;
      this.depthBiasConstant = p_409999_;
      this.sortKey = p_415862_;
      this.stencilTest = stencilTest;
   }

   public int getSortKey() {
      return SharedConstants.DEBUG_SHUFFLE_UI_RENDERING_ORDER ? super.hashCode() * (sortKeySeed + 1) : this.sortKey;
   }

   public static void updateSortKeySeed() {
      sortKeySeed = Math.round(100000.0F * (float)Math.random());
   }

   @Override
   public String toString() {
      return this.location.toString();
   }

   public DepthTestFunction getDepthTestFunction() {
      return this.depthTestFunction;
   }

   public PolygonMode getPolygonMode() {
      return this.polygonMode;
   }

   public boolean isCull() {
      return this.cull;
   }

   public LogicOp getColorLogic() {
      return this.colorLogic;
   }

   public Optional<BlendFunction> getBlendFunction() {
      return this.blendFunction;
   }

   public boolean isWriteColor() {
      return this.writeColor;
   }

   public boolean isWriteAlpha() {
      return this.writeAlpha;
   }

   public boolean isWriteDepth() {
      return this.writeDepth;
   }

   public float getDepthBiasScaleFactor() {
      return this.depthBiasScaleFactor;
   }

   public float getDepthBiasConstant() {
      return this.depthBiasConstant;
   }

   public Identifier getLocation() {
      return this.location;
   }

   public VertexFormat getVertexFormat() {
      return this.vertexFormat;
   }

   public VertexFormat.Mode getVertexFormatMode() {
      return this.vertexFormatMode;
   }

   public Identifier getVertexShader() {
      return this.vertexShader;
   }

   public Identifier getFragmentShader() {
      return this.fragmentShader;
   }

   public ShaderDefines getShaderDefines() {
      return this.shaderDefines;
   }

   public List<String> getSamplers() {
      return this.samplers;
   }

   public List<RenderPipeline.UniformDescription> getUniforms() {
      return this.uniforms;
   }

   public boolean wantsDepthTexture() {
      return this.depthTestFunction != DepthTestFunction.NO_DEPTH_TEST
         || this.depthBiasConstant != 0.0F
         || this.depthBiasScaleFactor != 0.0F
         || this.writeDepth;
   }

   public Optional<StencilTest> getStencilTest() {
      return this.stencilTest;
   }

   public RenderPipeline.Builder toBuilder() {
      RenderPipeline.Builder builder = new RenderPipeline.Builder();
      builder.location = Optional.of(this.location);
      builder.fragmentShader = Optional.of(this.fragmentShader);
      builder.vertexShader = Optional.of(this.vertexShader);
      if (!this.shaderDefines.isEmpty()) {
         ShaderDefines.Builder defBuilder = ShaderDefines.builder();

         for (Entry<String, String> entry : this.shaderDefines.values().entrySet()) {
            defBuilder.define(entry.getKey(), entry.getValue());
         }

         for (String flag : this.shaderDefines.flags()) {
            defBuilder.define(flag);
         }

         builder.definesBuilder = Optional.of(defBuilder);
      }

      if (!this.samplers.isEmpty()) {
         builder.samplers = Optional.of(new ArrayList<>(this.samplers));
      }

      if (!this.uniforms.isEmpty()) {
         builder.uniforms = Optional.of(new ArrayList<>(this.uniforms));
      }

      builder.depthTestFunction = Optional.of(this.depthTestFunction);
      builder.polygonMode = Optional.of(this.polygonMode);
      builder.cull = Optional.of(this.cull);
      builder.writeColor = Optional.of(this.writeColor);
      builder.writeAlpha = Optional.of(this.writeAlpha);
      builder.writeDepth = Optional.of(this.writeDepth);
      builder.colorLogic = Optional.of(this.colorLogic);
      builder.blendFunction = this.blendFunction;
      builder.vertexFormat = Optional.of(this.vertexFormat);
      builder.vertexFormatMode = Optional.of(this.vertexFormatMode);
      builder.depthBiasScaleFactor = this.depthBiasScaleFactor;
      builder.depthBiasConstant = this.depthBiasConstant;
      builder.stencilTest = this.stencilTest;
      return builder;
   }

   public static RenderPipeline.Builder builder(RenderPipeline.Snippet... p_405781_) {
      RenderPipeline.Builder renderpipeline$builder = new RenderPipeline.Builder();

      for (RenderPipeline.Snippet renderpipeline$snippet : p_405781_) {
         renderpipeline$builder.withSnippet(renderpipeline$snippet);
      }

      return renderpipeline$builder;
   }

   @OnlyIn(Dist.CLIENT)
   @DontObfuscate
   public static class Builder {
      private static int nextPipelineSortKey;
      private Optional<Identifier> location = Optional.empty();
      private Optional<Identifier> fragmentShader = Optional.empty();
      private Optional<Identifier> vertexShader = Optional.empty();
      private Optional<ShaderDefines.Builder> definesBuilder = Optional.empty();
      private Optional<List<String>> samplers = Optional.empty();
      private Optional<List<RenderPipeline.UniformDescription>> uniforms = Optional.empty();
      private Optional<DepthTestFunction> depthTestFunction = Optional.empty();
      private Optional<PolygonMode> polygonMode = Optional.empty();
      private Optional<Boolean> cull = Optional.empty();
      private Optional<Boolean> writeColor = Optional.empty();
      private Optional<Boolean> writeAlpha = Optional.empty();
      private Optional<Boolean> writeDepth = Optional.empty();
      private Optional<LogicOp> colorLogic = Optional.empty();
      private Optional<BlendFunction> blendFunction = Optional.empty();
      private Optional<VertexFormat> vertexFormat = Optional.empty();
      private Optional<VertexFormat.Mode> vertexFormatMode = Optional.empty();
      private float depthBiasScaleFactor;
      private float depthBiasConstant;
      private Optional<StencilTest> stencilTest = Optional.empty();

      Builder() {
      }

      public RenderPipeline.Builder withLocation(String p_405348_) {
         this.location = Optional.of(Identifier.withDefaultNamespace(p_405348_));
         return this;
      }

      public RenderPipeline.Builder withLocation(Identifier p_469362_) {
         this.location = Optional.of(p_469362_);
         return this;
      }

      public RenderPipeline.Builder withFragmentShader(String p_405346_) {
         this.fragmentShader = Optional.of(Identifier.withDefaultNamespace(p_405346_));
         return this;
      }

      public RenderPipeline.Builder withFragmentShader(Identifier p_468243_) {
         this.fragmentShader = Optional.of(p_468243_);
         return this;
      }

      public RenderPipeline.Builder withVertexShader(String p_405325_) {
         this.vertexShader = Optional.of(Identifier.withDefaultNamespace(p_405325_));
         return this;
      }

      public RenderPipeline.Builder withVertexShader(Identifier p_467021_) {
         this.vertexShader = Optional.of(p_467021_);
         return this;
      }

      public RenderPipeline.Builder withShaderDefine(String p_405063_) {
         if (this.definesBuilder.isEmpty()) {
            this.definesBuilder = Optional.of(ShaderDefines.builder());
         }

         this.definesBuilder.get().define(p_405063_);
         return this;
      }

      public RenderPipeline.Builder withShaderDefine(String p_405578_, int p_404886_) {
         if (this.definesBuilder.isEmpty()) {
            this.definesBuilder = Optional.of(ShaderDefines.builder());
         }

         this.definesBuilder.get().define(p_405578_, p_404886_);
         return this;
      }

      public RenderPipeline.Builder withShaderDefine(String p_405342_, float p_404699_) {
         if (this.definesBuilder.isEmpty()) {
            this.definesBuilder = Optional.of(ShaderDefines.builder());
         }

         this.definesBuilder.get().define(p_405342_, p_404699_);
         return this;
      }

      public RenderPipeline.Builder withSampler(String p_405103_) {
         if (this.samplers.isEmpty()) {
            this.samplers = Optional.of(new ArrayList<>());
         }

         this.samplers.get().add(p_405103_);
         return this;
      }

      public RenderPipeline.Builder withUniform(String p_405111_, UniformType p_410370_) {
         if (this.uniforms.isEmpty()) {
            this.uniforms = Optional.of(new ArrayList<>());
         }

         if (p_410370_ == UniformType.TEXEL_BUFFER) {
            throw new IllegalArgumentException("Cannot use texel buffer without specifying texture format");
         } else {
            this.uniforms.get().add(new RenderPipeline.UniformDescription(p_405111_, p_410370_));
            return this;
         }
      }

      public RenderPipeline.Builder withUniform(String p_418407_, UniformType p_418443_, TextureFormat p_418186_) {
         if (this.uniforms.isEmpty()) {
            this.uniforms = Optional.of(new ArrayList<>());
         }

         if (p_418443_ != UniformType.TEXEL_BUFFER) {
            throw new IllegalArgumentException("Only texel buffer can specify texture format");
         } else {
            this.uniforms.get().add(new RenderPipeline.UniformDescription(p_418407_, p_418186_));
            return this;
         }
      }

      public RenderPipeline.Builder withDepthTestFunction(DepthTestFunction p_410838_) {
         this.depthTestFunction = Optional.of(p_410838_);
         return this;
      }

      public RenderPipeline.Builder withPolygonMode(PolygonMode p_410375_) {
         this.polygonMode = Optional.of(p_410375_);
         return this;
      }

      public RenderPipeline.Builder withCull(boolean p_404683_) {
         this.cull = Optional.of(p_404683_);
         return this;
      }

      public RenderPipeline.Builder withBlend(BlendFunction p_404892_) {
         this.blendFunction = Optional.of(p_404892_);
         return this;
      }

      public RenderPipeline.Builder withoutBlend() {
         this.blendFunction = Optional.empty();
         return this;
      }

      public RenderPipeline.Builder withColorWrite(boolean p_405003_) {
         this.writeColor = Optional.of(p_405003_);
         this.writeAlpha = Optional.of(p_405003_);
         return this;
      }

      public RenderPipeline.Builder withColorWrite(boolean p_405874_, boolean p_405301_) {
         this.writeColor = Optional.of(p_405874_);
         this.writeAlpha = Optional.of(p_405301_);
         return this;
      }

      public RenderPipeline.Builder withDepthWrite(boolean p_405817_) {
         this.writeDepth = Optional.of(p_405817_);
         return this;
      }

      @Deprecated
      public RenderPipeline.Builder withColorLogic(LogicOp p_410366_) {
         this.colorLogic = Optional.of(p_410366_);
         return this;
      }

      public RenderPipeline.Builder withVertexFormat(VertexFormat p_405022_, VertexFormat.Mode p_405878_) {
         this.vertexFormat = Optional.of(p_405022_);
         this.vertexFormatMode = Optional.of(p_405878_);
         return this;
      }

      public RenderPipeline.Builder withDepthBias(float p_410793_, float p_410207_) {
         this.depthBiasScaleFactor = p_410793_;
         this.depthBiasConstant = p_410207_;
         return this;
      }

      public RenderPipeline.Builder withStencilTest(StencilTest stencilTest) {
         this.stencilTest = Optional.of(stencilTest);
         return this;
      }

      public RenderPipeline.Builder withoutStencilTest() {
         this.stencilTest = Optional.empty();
         return this;
      }

      void withSnippet(RenderPipeline.Snippet p_405255_) {
         if (p_405255_.vertexShader.isPresent()) {
            this.vertexShader = p_405255_.vertexShader;
         }

         if (p_405255_.fragmentShader.isPresent()) {
            this.fragmentShader = p_405255_.fragmentShader;
         }

         if (p_405255_.shaderDefines.isPresent()) {
            if (this.definesBuilder.isEmpty()) {
               this.definesBuilder = Optional.of(ShaderDefines.builder());
            }

            ShaderDefines shaderdefines = p_405255_.shaderDefines.get();

            for (Entry<String, String> entry : shaderdefines.values().entrySet()) {
               this.definesBuilder.get().define(entry.getKey(), entry.getValue());
            }

            for (String s : shaderdefines.flags()) {
               this.definesBuilder.get().define(s);
            }
         }

         p_405255_.samplers.ifPresent(p_405714_ -> {
            if (this.samplers.isPresent()) {
               this.samplers.get().addAll(p_405714_);
            } else {
               this.samplers = Optional.of(new ArrayList<>(p_405714_));
            }
         });
         p_405255_.uniforms.ifPresent(p_405104_ -> {
            if (this.uniforms.isPresent()) {
               this.uniforms.get().addAll(p_405104_);
            } else {
               this.uniforms = Optional.of(new ArrayList<>(p_405104_));
            }
         });
         if (p_405255_.depthTestFunction.isPresent()) {
            this.depthTestFunction = p_405255_.depthTestFunction;
         }

         if (p_405255_.cull.isPresent()) {
            this.cull = p_405255_.cull;
         }

         if (p_405255_.writeColor.isPresent()) {
            this.writeColor = p_405255_.writeColor;
         }

         if (p_405255_.writeAlpha.isPresent()) {
            this.writeAlpha = p_405255_.writeAlpha;
         }

         if (p_405255_.writeDepth.isPresent()) {
            this.writeDepth = p_405255_.writeDepth;
         }

         if (p_405255_.colorLogic.isPresent()) {
            this.colorLogic = p_405255_.colorLogic;
         }

         if (p_405255_.blendFunction.isPresent()) {
            this.blendFunction = p_405255_.blendFunction;
         }

         if (p_405255_.vertexFormat.isPresent()) {
            this.vertexFormat = p_405255_.vertexFormat;
         }

         if (p_405255_.vertexFormatMode.isPresent()) {
            this.vertexFormatMode = p_405255_.vertexFormatMode;
         }

         if (p_405255_.stencilTest.isPresent()) {
            this.stencilTest = p_405255_.stencilTest;
         }
      }

      public RenderPipeline.Snippet buildSnippet() {
         return new RenderPipeline.Snippet(
            this.vertexShader,
            this.fragmentShader,
            this.definesBuilder.map(ShaderDefines.Builder::build),
            this.samplers.map(Collections::unmodifiableList),
            this.uniforms.map(Collections::unmodifiableList),
            this.blendFunction,
            this.depthTestFunction,
            this.polygonMode,
            this.cull,
            this.writeColor,
            this.writeAlpha,
            this.writeDepth,
            this.colorLogic,
            this.vertexFormat,
            this.vertexFormatMode,
            this.stencilTest
         );
      }

      public RenderPipeline build() {
         if (this.location.isEmpty()) {
            throw new IllegalStateException("Missing location");
         } else if (this.vertexShader.isEmpty()) {
            throw new IllegalStateException("Missing vertex shader");
         } else if (this.fragmentShader.isEmpty()) {
            throw new IllegalStateException("Missing fragment shader");
         } else if (this.vertexFormat.isEmpty()) {
            throw new IllegalStateException("Missing vertex buffer format");
         } else if (this.vertexFormatMode.isEmpty()) {
            throw new IllegalStateException("Missing vertex mode");
         } else {
            return new RenderPipeline(
               this.location.get(),
               this.vertexShader.get(),
               this.fragmentShader.get(),
               this.definesBuilder.orElse(ShaderDefines.builder()).build(),
               List.copyOf(this.samplers.orElse(new ArrayList<>())),
               this.uniforms.orElse(Collections.emptyList()),
               this.blendFunction,
               this.depthTestFunction.orElse(DepthTestFunction.LEQUAL_DEPTH_TEST),
               this.polygonMode.orElse(PolygonMode.FILL),
               this.cull.orElse(true),
               this.writeColor.orElse(true),
               this.writeAlpha.orElse(true),
               this.writeDepth.orElse(true),
               this.colorLogic.orElse(LogicOp.NONE),
               this.vertexFormat.get(),
               this.vertexFormatMode.get(),
               this.depthBiasScaleFactor,
               this.depthBiasConstant,
               nextPipelineSortKey++,
               this.stencilTest
            );
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   @DontObfuscate
   public record Snippet(
      Optional<Identifier> vertexShader,
      Optional<Identifier> fragmentShader,
      Optional<ShaderDefines> shaderDefines,
      Optional<List<String>> samplers,
      Optional<List<RenderPipeline.UniformDescription>> uniforms,
      Optional<BlendFunction> blendFunction,
      Optional<DepthTestFunction> depthTestFunction,
      Optional<PolygonMode> polygonMode,
      Optional<Boolean> cull,
      Optional<Boolean> writeColor,
      Optional<Boolean> writeAlpha,
      Optional<Boolean> writeDepth,
      Optional<LogicOp> colorLogic,
      Optional<VertexFormat> vertexFormat,
      Optional<VertexFormat.Mode> vertexFormatMode,
      Optional<StencilTest> stencilTest
   ) {
      @Deprecated
      public Snippet(
         Optional<Identifier> vertexShader,
         Optional<Identifier> fragmentShader,
         Optional<ShaderDefines> shaderDefines,
         Optional<List<String>> samplers,
         Optional<List<RenderPipeline.UniformDescription>> uniforms,
         Optional<BlendFunction> blendFunction,
         Optional<DepthTestFunction> depthTestFunction,
         Optional<PolygonMode> polygonMode,
         Optional<Boolean> cull,
         Optional<Boolean> writeColor,
         Optional<Boolean> writeAlpha,
         Optional<Boolean> writeDepth,
         Optional<LogicOp> colorLogic,
         Optional<VertexFormat> vertexFormat,
         Optional<VertexFormat.Mode> vertexFormatMode
      ) {
         this(
            vertexShader,
            fragmentShader,
            shaderDefines,
            samplers,
            uniforms,
            blendFunction,
            depthTestFunction,
            polygonMode,
            cull,
            writeColor,
            writeAlpha,
            writeDepth,
            colorLogic,
            vertexFormat,
            vertexFormatMode,
            Optional.empty()
         );
      }
   }

   @OnlyIn(Dist.CLIENT)
   @DontObfuscate
   public record UniformDescription(String name, UniformType type, @Nullable TextureFormat textureFormat) {
      public UniformDescription(String p_405787_, UniformType p_410454_) {
         this(p_405787_, p_410454_, null);
         if (p_410454_ == UniformType.TEXEL_BUFFER) {
            throw new IllegalArgumentException("Texel buffer needs a texture format");
         }
      }

      public UniformDescription(String p_418130_, TextureFormat p_418054_) {
         this(p_418130_, UniformType.TEXEL_BUFFER, p_418054_);
      }
   }
}
