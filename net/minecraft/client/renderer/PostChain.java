package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.shaders.UniformType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class PostChain implements AutoCloseable {
   public static final Identifier MAIN_TARGET_ID = Identifier.withDefaultNamespace("main");
   private final List<PostPass> passes;
   private final Map<Identifier, PostChainConfig.InternalTarget> internalTargets;
   private final Set<Identifier> externalTargets;
   private final Map<Identifier, RenderTarget> persistentTargets = new HashMap<>();
   private final CachedOrthoProjectionMatrixBuffer projectionMatrixBuffer;

   private PostChain(
      List<PostPass> p_364536_,
      Map<Identifier, PostChainConfig.InternalTarget> p_361479_,
      Set<Identifier> p_361607_,
      CachedOrthoProjectionMatrixBuffer p_418200_
   ) {
      this.passes = p_364536_;
      this.internalTargets = p_361479_;
      this.externalTargets = p_361607_;
      this.projectionMatrixBuffer = p_418200_;
   }

   public static PostChain load(
      PostChainConfig p_366777_, TextureManager p_110034_, Set<Identifier> p_362436_, Identifier p_468637_, CachedOrthoProjectionMatrixBuffer p_418231_
   ) throws ShaderManager.CompilationException {
      Stream<Identifier> stream = p_366777_.passes().stream().flatMap(PostChainConfig.Pass::referencedTargets);
      Set<Identifier> set = stream.filter(p_467997_ -> !p_366777_.internalTargets().containsKey(p_467997_)).collect(Collectors.toSet());
      Set<Identifier> set1 = Sets.difference(set, p_362436_);
      if (!set1.isEmpty()) {
         throw new ShaderManager.CompilationException("Referenced external targets are not available in this context: " + set1);
      } else {
         Builder<PostPass> builder = ImmutableList.builder();

         for (int i = 0; i < p_366777_.passes().size(); i++) {
            PostChainConfig.Pass postchainconfig$pass = p_366777_.passes().get(i);
            builder.add(createPass(p_110034_, postchainconfig$pass, p_468637_.withSuffix("/" + i)));
         }

         return new PostChain(builder.build(), p_366777_.internalTargets(), set, p_418231_);
      }
   }

   private static PostPass createPass(TextureManager p_364641_, PostChainConfig.Pass p_361826_, Identifier p_469922_) throws ShaderManager.CompilationException {
      RenderPipeline.Builder renderpipeline$builder = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
         .withFragmentShader(p_361826_.fragmentShaderId())
         .withVertexShader(p_361826_.vertexShaderId())
         .withLocation(p_469922_);

      for (PostChainConfig.Input postchainconfig$input : p_361826_.inputs()) {
         renderpipeline$builder.withSampler(postchainconfig$input.samplerName() + "Sampler");
      }

      renderpipeline$builder.withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER);

      for (String s1 : p_361826_.uniforms().keySet()) {
         renderpipeline$builder.withUniform(s1, UniformType.UNIFORM_BUFFER);
      }

      RenderPipeline renderpipeline = renderpipeline$builder.build();
      List<PostPass.Input> list = new ArrayList<>();

      for (PostChainConfig.Input postchainconfig$input1 : p_361826_.inputs()) {
         switch (postchainconfig$input1) {
            case PostChainConfig.TextureInput(String abstracttexture, Identifier var36, int var37, int var38, boolean var39):
               AbstractTexture abstracttexturex = p_364641_.getTexture(var36.withPath(p_359199_ -> "textures/effect/" + p_359199_ + ".png"));
               list.add(new PostPass.TextureInput(abstracttexture, abstracttexturex, var37, var38, var39));
               break;
            case PostChainConfig.TargetInput(String var22, Identifier var41, boolean var42, boolean var43):
               list.add(new PostPass.TargetInput(var22, var41, var42, var43));
               break;
            default:
               throw new MatchException(null, null);
         }
      }

      return new PostPass(renderpipeline, p_361826_.outputTarget(), p_361826_.uniforms(), list);
   }

   public void addToFrame(FrameGraphBuilder p_362523_, int p_361423_, int p_362735_, PostChain.TargetBundle p_361871_) {
      GpuBufferSlice gpubufferslice = this.projectionMatrixBuffer.getBuffer(p_361423_, p_362735_);
      Map<Identifier, ResourceHandle<RenderTarget>> map = new HashMap<>(this.internalTargets.size() + this.externalTargets.size());

      for (Identifier identifier : this.externalTargets) {
         map.put(identifier, p_361871_.getOrThrow(identifier));
      }

      for (Entry<Identifier, PostChainConfig.InternalTarget> entry : this.internalTargets.entrySet()) {
         Identifier identifier1 = entry.getKey();
         PostChainConfig.InternalTarget postchainconfig$internaltarget = entry.getValue();
         RenderTargetDescriptor rendertargetdescriptor = new RenderTargetDescriptor(
            postchainconfig$internaltarget.width().orElse(p_361423_),
            postchainconfig$internaltarget.height().orElse(p_362735_),
            true,
            postchainconfig$internaltarget.clearColor()
         );
         if (postchainconfig$internaltarget.persistent()) {
            RenderTarget rendertarget = this.getOrCreatePersistentTarget(identifier1, rendertargetdescriptor);
            map.put(identifier1, p_362523_.importExternal(identifier1.toString(), rendertarget));
         } else {
            map.put(identifier1, p_362523_.createInternal(identifier1.toString(), rendertargetdescriptor));
         }
      }

      for (PostPass postpass : this.passes) {
         postpass.addToFrame(p_362523_, map, gpubufferslice);
      }

      for (Identifier identifier2 : this.externalTargets) {
         p_361871_.replace(identifier2, map.get(identifier2));
      }
   }

   @Deprecated
   public void process(RenderTarget p_361528_, GraphicsResourceAllocator p_361187_) {
      FrameGraphBuilder framegraphbuilder = new FrameGraphBuilder();
      PostChain.TargetBundle postchain$targetbundle = PostChain.TargetBundle.of(MAIN_TARGET_ID, framegraphbuilder.importExternal("main", p_361528_));
      this.addToFrame(framegraphbuilder, p_361528_.width, p_361528_.height, postchain$targetbundle);
      framegraphbuilder.execute(p_361187_);
   }

   private RenderTarget getOrCreatePersistentTarget(Identifier p_467569_, RenderTargetDescriptor p_418259_) {
      RenderTarget rendertarget = this.persistentTargets.get(p_467569_);
      if (rendertarget == null || rendertarget.width != p_418259_.width() || rendertarget.height != p_418259_.height()) {
         if (rendertarget != null) {
            rendertarget.destroyBuffers();
         }

         rendertarget = p_418259_.allocate();
         p_418259_.prepare(rendertarget);
         this.persistentTargets.put(p_467569_, rendertarget);
      }

      return rendertarget;
   }

   @Override
   public void close() {
      this.persistentTargets.values().forEach(RenderTarget::destroyBuffers);
      this.persistentTargets.clear();

      for (PostPass postpass : this.passes) {
         postpass.close();
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface TargetBundle {
      static PostChain.TargetBundle of(final Identifier p_468055_, final ResourceHandle<RenderTarget> p_365173_) {
         return new PostChain.TargetBundle() {
            private ResourceHandle<RenderTarget> handle = p_365173_;

            @Override
            public void replace(Identifier p_468670_, ResourceHandle<RenderTarget> p_360603_) {
               if (p_468670_.equals(p_468055_)) {
                  this.handle = p_360603_;
               } else {
                  throw new IllegalArgumentException("No target with id " + p_468670_);
               }
            }

            @Nullable
            @Override
            public ResourceHandle<RenderTarget> get(Identifier p_468117_) {
               return p_468117_.equals(p_468055_) ? this.handle : null;
            }
         };
      }

      void replace(Identifier var1, ResourceHandle<RenderTarget> var2);

      @Nullable
      ResourceHandle<RenderTarget> get(Identifier var1);

      default ResourceHandle<RenderTarget> getOrThrow(Identifier p_467866_) {
         ResourceHandle<RenderTarget> resourcehandle = this.get(p_467866_);
         if (resourcehandle == null) {
            throw new IllegalArgumentException("Missing target with id " + p_467866_);
         } else {
            return resourcehandle;
         }
      }
   }
}
