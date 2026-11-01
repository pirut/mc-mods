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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class PostChain implements AutoCloseable {
   public static final Identifier MAIN_TARGET_ID = Identifier.withDefaultNamespace("main");
   private final List<PostPass> passes;
   private final Map<Identifier, PostChainConfig.InternalTarget> internalTargets;
   private final Set<Identifier> externalTargets;
   private final Map<Identifier, RenderTarget> persistentTargets = new HashMap<>();
   private final CachedOrthoProjectionMatrixBuffer projectionMatrixBuffer;

   private PostChain(
      List<PostPass> passes,
      Map<Identifier, PostChainConfig.InternalTarget> internalTargets,
      Set<Identifier> externalTargets,
      CachedOrthoProjectionMatrixBuffer projectionMatrixBuffer
   ) {
      this.passes = passes;
      this.internalTargets = internalTargets;
      this.externalTargets = externalTargets;
      this.projectionMatrixBuffer = projectionMatrixBuffer;
   }

   public static PostChain load(
      PostChainConfig config,
      TextureManager textureManager,
      Set<Identifier> externalTargets,
      Identifier name,
      CachedOrthoProjectionMatrixBuffer projectionMatrixBuffer
   ) throws ShaderManager.CompilationException {
      Stream<Identifier> stream = config.passes().stream().flatMap(PostChainConfig.Pass::referencedTargets);
      Set<Identifier> set = stream.filter(identifier -> !config.internalTargets().containsKey(identifier)).collect(Collectors.toSet());
      Set<Identifier> set2 = Sets.difference(set, externalTargets);
      if (!set2.isEmpty()) {
         throw new ShaderManager.CompilationException("Referenced external targets are not available in this context: " + set2);
      } else {
         Builder<PostPass> builder = ImmutableList.builder();

         for (int i = 0; i < config.passes().size(); i++) {
            PostChainConfig.Pass pass = config.passes().get(i);
            builder.add(createPass(textureManager, pass, name.withSuffix("/" + i)));
         }

         return new PostChain(builder.build(), config.internalTargets(), set, projectionMatrixBuffer);
      }
   }

   private static PostPass createPass(TextureManager textureManager, PostChainConfig.Pass pass, Identifier location) throws ShaderManager.CompilationException {
      RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
         .withFragmentShader(pass.fragmentShaderId())
         .withVertexShader(pass.vertexShaderId())
         .withLocation(location);

      for (PostChainConfig.Input input : pass.inputs()) {
         builder.withSampler(input.samplerName() + "Sampler");
      }

      builder.withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER);

      for (String string : pass.uniforms().keySet()) {
         builder.withUniform(string, UniformType.UNIFORM_BUFFER);
      }

      RenderPipeline renderPipeline = builder.build();
      List<PostPass.Input> list = new ArrayList<>();

      for (PostChainConfig.Input input2 : pass.inputs()) {
         switch (input2) {
            case PostChainConfig.TextureInput(String var35, Identifier var36, int var37, int var38, boolean var39):
               AbstractTexture abstractTexture = textureManager.getTexture(var36.withPath(string -> "textures/effect/" + string + ".png"));
               list.add(new PostPass.TextureInput(var35, abstractTexture, var37, var38, var39));
               break;
            case PostChainConfig.TargetInput(String var21, Identifier var41, boolean var42, boolean var43):
               list.add(new PostPass.TargetInput(var21, var41, var42, var43));
               break;
            default:
               throw new MatchException(null, null);
         }
      }

      return new PostPass(renderPipeline, pass.outputTarget(), pass.uniforms(), list);
   }

   public void addToFrame(FrameGraphBuilder frameGraphBuilder, int width, int height, PostChain.TargetBundle targetBundle) {
      GpuBufferSlice gpuBufferSlice = this.projectionMatrixBuffer.getBuffer(width, height);
      Map<Identifier, ResourceHandle<RenderTarget>> map = new HashMap<>(this.internalTargets.size() + this.externalTargets.size());

      for (Identifier identifier : this.externalTargets) {
         map.put(identifier, targetBundle.getOrThrow(identifier));
      }

      for (Entry<Identifier, PostChainConfig.InternalTarget> entry : this.internalTargets.entrySet()) {
         Identifier identifier2 = entry.getKey();
         PostChainConfig.InternalTarget internalTarget = entry.getValue();
         RenderTargetDescriptor renderTargetDescriptor = new RenderTargetDescriptor(
            internalTarget.width().orElse(width), internalTarget.height().orElse(height), true, internalTarget.clearColor()
         );
         if (internalTarget.persistent()) {
            RenderTarget renderTarget = this.getOrCreatePersistentTarget(identifier2, renderTargetDescriptor);
            map.put(identifier2, frameGraphBuilder.importExternal(identifier2.toString(), renderTarget));
         } else {
            map.put(identifier2, frameGraphBuilder.createInternal(identifier2.toString(), renderTargetDescriptor));
         }
      }

      for (PostPass postPass : this.passes) {
         postPass.addToFrame(frameGraphBuilder, map, gpuBufferSlice);
      }

      for (Identifier identifier : this.externalTargets) {
         targetBundle.replace(identifier, map.get(identifier));
      }
   }

   @Deprecated
   public void process(RenderTarget target, GraphicsResourceAllocator graphicsResourceAllocator) {
      FrameGraphBuilder frameGraphBuilder = new FrameGraphBuilder();
      PostChain.TargetBundle targetBundle = PostChain.TargetBundle.of(MAIN_TARGET_ID, frameGraphBuilder.importExternal("main", target));
      this.addToFrame(frameGraphBuilder, target.width, target.height, targetBundle);
      frameGraphBuilder.execute(graphicsResourceAllocator);
   }

   private RenderTarget getOrCreatePersistentTarget(Identifier name, RenderTargetDescriptor descriptor) {
      RenderTarget renderTarget = this.persistentTargets.get(name);
      if (renderTarget == null || renderTarget.width != descriptor.width() || renderTarget.height != descriptor.height()) {
         if (renderTarget != null) {
            renderTarget.destroyBuffers();
         }

         renderTarget = descriptor.allocate();
         descriptor.prepare(renderTarget);
         this.persistentTargets.put(name, renderTarget);
      }

      return renderTarget;
   }

   @Override
   public void close() {
      this.persistentTargets.values().forEach(RenderTarget::destroyBuffers);
      this.persistentTargets.clear();

      for (PostPass postPass : this.passes) {
         postPass.close();
      }
   }

   @Environment(EnvType.CLIENT)
   public interface TargetBundle {
      static PostChain.TargetBundle of(Identifier id, ResourceHandle<RenderTarget> handle) {
         return new PostChain.TargetBundle() {
            private ResourceHandle<RenderTarget> handle = handle;

            @Override
            public void replace(Identifier id, ResourceHandle<RenderTarget> handle) {
               if (id.equals(id)) {
                  this.handle = handle;
               } else {
                  throw new IllegalArgumentException("No target with id " + id);
               }
            }

            @Nullable
            @Override
            public ResourceHandle<RenderTarget> get(Identifier id) {
               return id.equals(id) ? this.handle : null;
            }
         };
      }

      void replace(Identifier id, ResourceHandle<RenderTarget> handle);

      @Nullable
      ResourceHandle<RenderTarget> get(Identifier id);

      default ResourceHandle<RenderTarget> getOrThrow(Identifier id) {
         ResourceHandle<RenderTarget> resourceHandle = this.get(id);
         if (resourceHandle == null) {
            throw new IllegalArgumentException("Missing target with id " + id);
         } else {
            return resourceHandle;
         }
      }
   }
}
