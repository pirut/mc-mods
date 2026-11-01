package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.IdentifierException;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.FileUtil;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ShaderManager extends SimplePreparableReloadListener<ShaderManager.Configs> implements AutoCloseable {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final int MAX_LOG_LENGTH = 32768;
   public static final String SHADER_PATH = "shaders";
   private static final String SHADER_INCLUDE_PATH = "shaders/include/";
   private static final FileToIdConverter POST_CHAIN_ID_CONVERTER = FileToIdConverter.json("post_effect");
   final TextureManager textureManager;
   private final Consumer<Exception> recoveryHandler;
   private ShaderManager.CompilationCache compilationCache = new ShaderManager.CompilationCache(ShaderManager.Configs.EMPTY);
   final CachedOrthoProjectionMatrixBuffer postChainProjectionMatrixBuffer = new CachedOrthoProjectionMatrixBuffer("post", 0.1F, 1000.0F, false);

   public ShaderManager(TextureManager p_366478_, Consumer<Exception> p_368560_) {
      this.textureManager = p_366478_;
      this.recoveryHandler = p_368560_;
   }

   protected ShaderManager.Configs prepare(ResourceManager p_366761_, ProfilerFiller p_366562_) {
      Builder<ShaderManager.ShaderSourceKey, String> builder = ImmutableMap.builder();
      Map<Identifier, Resource> map = p_366761_.listResources("shaders", ShaderManager::isShader);

      for (Entry<Identifier, Resource> entry : map.entrySet()) {
         Identifier identifier = entry.getKey();
         ShaderType shadertype = ShaderType.byLocation(identifier);
         if (shadertype != null) {
            loadShader(identifier, entry.getValue(), shadertype, map, builder);
         }
      }

      Builder<Identifier, PostChainConfig> builder1 = ImmutableMap.builder();

      for (Entry<Identifier, Resource> entry1 : POST_CHAIN_ID_CONVERTER.listMatchingResources(p_366761_).entrySet()) {
         loadPostChain(entry1.getKey(), entry1.getValue(), builder1);
      }

      return new ShaderManager.Configs(builder.build(), builder1.build());
   }

   private static void loadShader(
      Identifier p_469401_,
      Resource p_366763_,
      ShaderType p_410200_,
      Map<Identifier, Resource> p_366725_,
      Builder<ShaderManager.ShaderSourceKey, String> p_366733_
   ) {
      Identifier identifier = p_410200_.idConverter().fileToId(p_469401_);
      GlslPreprocessor glslpreprocessor = createPreprocessor(p_366725_, p_469401_);

      try (Reader reader = p_366763_.openAsReader()) {
         String s = IOUtils.toString(reader);
         p_366733_.put(new ShaderManager.ShaderSourceKey(identifier, p_410200_), String.join("", glslpreprocessor.process(s)));
      } catch (IOException var12) {
         LOGGER.error("Failed to load shader source at {}", p_469401_, var12);
      }
   }

   private static GlslPreprocessor createPreprocessor(final Map<Identifier, Resource> p_366891_, Identifier p_469155_) {
      final Identifier identifier = p_469155_.withPath(FileUtil::getFullResourcePath);
      return new GlslPreprocessor() {
         private final Set<Identifier> importedLocations = new ObjectArraySet();

         @Nullable
         @Override
         public String applyImport(boolean p_366551_, String p_366739_) {
            Identifier identifier1;
            try {
               if (p_366551_) {
                  identifier1 = identifier.withPath(p_465617_ -> FileUtil.normalizeResourcePath(p_465617_ + p_366739_));
               } else {
                  identifier1 = Identifier.parse(p_366739_).withPrefix("shaders/include/");
               }
            } catch (IdentifierException var9) {
               ShaderManager.LOGGER.error("Malformed GLSL import {}: {}", p_366739_, var9.getMessage());
               return "#error " + var9.getMessage();
            }

            if (!this.importedLocations.add(identifier1)) {
               return null;
            } else {
               try {
                  String s;
                  try (Reader reader = p_366891_.get(identifier1).openAsReader()) {
                     s = IOUtils.toString(reader);
                  }

                  return s;
               } catch (IOException var11) {
                  ShaderManager.LOGGER.error("Could not open GLSL import {}: {}", identifier1, var11.getMessage());
                  return "#error " + var11.getMessage();
               }
            }
         }
      };
   }

   private static void loadPostChain(Identifier p_468648_, Resource p_366558_, Builder<Identifier, PostChainConfig> p_366892_) {
      Identifier identifier = POST_CHAIN_ID_CONVERTER.fileToId(p_468648_);

      try (Reader reader = p_366558_.openAsReader()) {
         JsonElement jsonelement = StrictJsonParser.parse(reader);
         p_366892_.put(identifier, (PostChainConfig)PostChainConfig.CODEC.parse(JsonOps.INSTANCE, jsonelement).getOrThrow(JsonSyntaxException::new));
      } catch (IOException | JsonParseException var9) {
         LOGGER.error("Failed to parse post chain at {}", p_468648_, var9);
      }
   }

   private static boolean isShader(Identifier p_468212_) {
      return ShaderType.byLocation(p_468212_) != null || p_468212_.getPath().endsWith(".glsl");
   }

   protected void apply(ShaderManager.Configs p_366597_, ResourceManager p_366533_, ProfilerFiller p_366866_) {
      ShaderManager.CompilationCache shadermanager$compilationcache = new ShaderManager.CompilationCache(p_366597_);
      Set<RenderPipeline> set = new HashSet<>(RenderPipelines.getStaticPipelines());
      List<Identifier> list = new ArrayList<>();
      GpuDevice gpudevice = RenderSystem.getDevice();
      gpudevice.clearPipelineCache();

      for (RenderPipeline renderpipeline : set) {
         CompiledRenderPipeline compiledrenderpipeline = gpudevice.precompilePipeline(renderpipeline, shadermanager$compilationcache::getShaderSource);
         if (!compiledrenderpipeline.isValid()) {
            list.add(renderpipeline.getLocation());
         }
      }

      if (!list.isEmpty()) {
         gpudevice.clearPipelineCache();
         throw new RuntimeException(
            "Failed to load required shader programs:\n" + list.stream().map(p_467862_ -> " - " + p_467862_).collect(Collectors.joining("\n"))
         );
      } else {
         this.compilationCache.close();
         this.compilationCache = shadermanager$compilationcache;
      }
   }

   @Override
   public String getName() {
      return "Shader Loader";
   }

   private void tryTriggerRecovery(Exception p_383219_) {
      if (!this.compilationCache.triggeredRecovery) {
         this.recoveryHandler.accept(p_383219_);
         this.compilationCache.triggeredRecovery = true;
      }
   }

   @Nullable
   public PostChain getPostChain(Identifier p_469182_, Set<Identifier> p_366652_) {
      try {
         return this.compilationCache.getOrLoadPostChain(p_469182_, p_366652_);
      } catch (ShaderManager.CompilationException var4) {
         LOGGER.error("Failed to load post chain: {}", p_469182_, var4);
         this.compilationCache.postChains.put(p_469182_, Optional.empty());
         this.tryTriggerRecovery(var4);
         return null;
      }
   }

   @Override
   public void close() {
      this.compilationCache.close();
      this.postChainProjectionMatrixBuffer.close();
   }

   @Nullable
   public String getShader(Identifier p_469373_, ShaderType p_410496_) {
      return this.compilationCache.getShaderSource(p_469373_, p_410496_);
   }

   @OnlyIn(Dist.CLIENT)
   class CompilationCache implements AutoCloseable {
      private final ShaderManager.Configs configs;
      final Map<Identifier, Optional<PostChain>> postChains = new HashMap<>();
      boolean triggeredRecovery;

      CompilationCache(ShaderManager.Configs p_368510_) {
         this.configs = p_368510_;
      }

      @Nullable
      public PostChain getOrLoadPostChain(Identifier p_468305_, Set<Identifier> p_368722_) throws ShaderManager.CompilationException {
         Optional<PostChain> optional = this.postChains.get(p_468305_);
         if (optional != null) {
            return optional.orElse(null);
         } else {
            PostChain postchain = this.loadPostChain(p_468305_, p_368722_);
            this.postChains.put(p_468305_, Optional.of(postchain));
            return postchain;
         }
      }

      private PostChain loadPostChain(Identifier p_466826_, Set<Identifier> p_368581_) throws ShaderManager.CompilationException {
         PostChainConfig postchainconfig = this.configs.postChains.get(p_466826_);
         if (postchainconfig == null) {
            throw new ShaderManager.CompilationException("Could not find post chain with id: " + p_466826_);
         } else {
            return PostChain.load(postchainconfig, ShaderManager.this.textureManager, p_368581_, p_466826_, ShaderManager.this.postChainProjectionMatrixBuffer);
         }
      }

      @Override
      public void close() {
         this.postChains.values().forEach(p_418047_ -> p_418047_.ifPresent(PostChain::close));
         this.postChains.clear();
      }

      @Nullable
      public String getShaderSource(Identifier p_467909_, ShaderType p_410764_) {
         return this.configs.shaderSources.get(new ShaderManager.ShaderSourceKey(p_467909_, p_410764_));
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class CompilationException extends Exception {
      public CompilationException(String p_366771_) {
         super(p_366771_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record Configs(Map<ShaderManager.ShaderSourceKey, String> shaderSources, Map<Identifier, PostChainConfig> postChains) {
      public static final ShaderManager.Configs EMPTY = new ShaderManager.Configs(Map.of(), Map.of());
   }

   @OnlyIn(Dist.CLIENT)
   record ShaderSourceKey(Identifier id, ShaderType type) {
      @Override
      public String toString() {
         return this.id + " (" + this.type + ")";
      }
   }
}
