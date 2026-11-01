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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
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

   public ShaderManager(TextureManager textureManager, Consumer<Exception> recoveryHandler) {
      this.textureManager = textureManager;
      this.recoveryHandler = recoveryHandler;
   }

   protected ShaderManager.Configs prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
      Builder<ShaderManager.ShaderSourceKey, String> builder = ImmutableMap.builder();
      Map<Identifier, Resource> map = resourceManager.listResources("shaders", ShaderManager::isShader);

      for (Entry<Identifier, Resource> entry : map.entrySet()) {
         Identifier identifier = entry.getKey();
         ShaderType shaderType = ShaderType.byLocation(identifier);
         if (shaderType != null) {
            loadShader(identifier, entry.getValue(), shaderType, map, builder);
         }
      }

      Builder<Identifier, PostChainConfig> builder2 = ImmutableMap.builder();

      for (Entry<Identifier, Resource> entry2 : POST_CHAIN_ID_CONVERTER.listMatchingResources(resourceManager).entrySet()) {
         loadPostChain(entry2.getKey(), entry2.getValue(), builder2);
      }

      return new ShaderManager.Configs(builder.build(), builder2.build());
   }

   private static void loadShader(
      Identifier location, Resource shader, ShaderType type, Map<Identifier, Resource> shaderResources, Builder<ShaderManager.ShaderSourceKey, String> output
   ) {
      Identifier identifier = type.idConverter().fileToId(location);
      GlslPreprocessor glslPreprocessor = createPreprocessor(shaderResources, location);

      try (Reader reader = shader.openAsReader()) {
         String string = IOUtils.toString(reader);
         output.put(new ShaderManager.ShaderSourceKey(identifier, type), String.join("", glslPreprocessor.process(string)));
      } catch (IOException var12) {
         LOGGER.error("Failed to load shader source at {}", location, var12);
      }
   }

   private static GlslPreprocessor createPreprocessor(Map<Identifier, Resource> shaderResources, Identifier shaderLocation) {
      final Identifier identifier = shaderLocation.withPath(FileUtil::getFullResourcePath);
      return new GlslPreprocessor() {
         private final Set<Identifier> importedLocations = new ObjectArraySet();

         @Nullable
         @Override
         public String applyImport(boolean useFullPath, String directory) {
            Identifier identifierx;
            try {
               if (useFullPath) {
                  identifierx = identifier.withPath(string2 -> FileUtil.normalizeResourcePath(string2 + directory));
               } else {
                  identifierx = Identifier.parse(directory).withPrefix("shaders/include/");
               }
            } catch (IdentifierException var8) {
               ShaderManager.LOGGER.error("Malformed GLSL import {}: {}", directory, var8.getMessage());
               return "#error " + var8.getMessage();
            }

            if (!this.importedLocations.add(identifierx)) {
               return null;
            } else {
               try {
                  String var5;
                  try (Reader reader = shaderResources.get(identifierx).openAsReader()) {
                     var5 = IOUtils.toString(reader);
                  }

                  return var5;
               } catch (IOException var10) {
                  ShaderManager.LOGGER.error("Could not open GLSL import {}: {}", identifierx, var10.getMessage());
                  return "#error " + var10.getMessage();
               }
            }
         }
      };
   }

   private static void loadPostChain(Identifier location, Resource postChain, Builder<Identifier, PostChainConfig> output) {
      Identifier identifier = POST_CHAIN_ID_CONVERTER.fileToId(location);

      try (Reader reader = postChain.openAsReader()) {
         JsonElement jsonElement = StrictJsonParser.parse(reader);
         output.put(identifier, (PostChainConfig)PostChainConfig.CODEC.parse(JsonOps.INSTANCE, jsonElement).getOrThrow(JsonSyntaxException::new));
      } catch (JsonParseException | IOException var9) {
         LOGGER.error("Failed to parse post chain at {}", location, var9);
      }
   }

   private static boolean isShader(Identifier location) {
      return ShaderType.byLocation(location) != null || location.getPath().endsWith(".glsl");
   }

   protected void apply(ShaderManager.Configs configs, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
      ShaderManager.CompilationCache compilationCache = new ShaderManager.CompilationCache(configs);
      Set<RenderPipeline> set = new HashSet<>(RenderPipelines.getStaticPipelines());
      List<Identifier> list = new ArrayList<>();
      GpuDevice gpuDevice = RenderSystem.getDevice();
      gpuDevice.clearPipelineCache();

      for (RenderPipeline renderPipeline : set) {
         CompiledRenderPipeline compiledRenderPipeline = gpuDevice.precompilePipeline(renderPipeline, compilationCache::getShaderSource);
         if (!compiledRenderPipeline.isValid()) {
            list.add(renderPipeline.getLocation());
         }
      }

      if (!list.isEmpty()) {
         gpuDevice.clearPipelineCache();
         throw new RuntimeException(
            "Failed to load required shader programs:\n" + list.stream().map(identifier -> " - " + identifier).collect(Collectors.joining("\n"))
         );
      } else {
         this.compilationCache.close();
         this.compilationCache = compilationCache;
      }
   }

   @Override
   public String getName() {
      return "Shader Loader";
   }

   private void tryTriggerRecovery(Exception exception) {
      if (!this.compilationCache.triggeredRecovery) {
         this.recoveryHandler.accept(exception);
         this.compilationCache.triggeredRecovery = true;
      }
   }

   @Nullable
   public PostChain getPostChain(Identifier id, Set<Identifier> externalTargets) {
      try {
         return this.compilationCache.getOrLoadPostChain(id, externalTargets);
      } catch (ShaderManager.CompilationException var4) {
         LOGGER.error("Failed to load post chain: {}", id, var4);
         this.compilationCache.postChains.put(id, Optional.empty());
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
   public String getShader(Identifier id, ShaderType type) {
      return this.compilationCache.getShaderSource(id, type);
   }

   @Environment(EnvType.CLIENT)
   class CompilationCache implements AutoCloseable {
      private final ShaderManager.Configs configs;
      final Map<Identifier, Optional<PostChain>> postChains = new HashMap<>();
      boolean triggeredRecovery;

      CompilationCache(final ShaderManager.Configs configs) {
         this.configs = configs;
      }

      @Nullable
      public PostChain getOrLoadPostChain(Identifier name, Set<Identifier> externalTargets) throws ShaderManager.CompilationException {
         Optional<PostChain> optional = this.postChains.get(name);
         if (optional != null) {
            return optional.orElse(null);
         } else {
            PostChain postChain = this.loadPostChain(name, externalTargets);
            this.postChains.put(name, Optional.of(postChain));
            return postChain;
         }
      }

      private PostChain loadPostChain(Identifier name, Set<Identifier> externalTargets) throws ShaderManager.CompilationException {
         PostChainConfig postChainConfig = this.configs.postChains.get(name);
         if (postChainConfig == null) {
            throw new ShaderManager.CompilationException("Could not find post chain with id: " + name);
         } else {
            return PostChain.load(postChainConfig, ShaderManager.this.textureManager, externalTargets, name, ShaderManager.this.postChainProjectionMatrixBuffer);
         }
      }

      @Override
      public void close() {
         this.postChains.values().forEach(optional -> optional.ifPresent(PostChain::close));
         this.postChains.clear();
      }

      @Nullable
      public String getShaderSource(Identifier id, ShaderType type) {
         return this.configs.shaderSources.get(new ShaderManager.ShaderSourceKey(id, type));
      }
   }

   @Environment(EnvType.CLIENT)
   public static class CompilationException extends Exception {
      public CompilationException(String message) {
         super(message);
      }
   }

   @Environment(EnvType.CLIENT)
   public record Configs(Map<ShaderManager.ShaderSourceKey, String> shaderSources, Map<Identifier, PostChainConfig> postChains) {
      public static final ShaderManager.Configs EMPTY = new ShaderManager.Configs(Map.of(), Map.of());
   }

   @Environment(EnvType.CLIENT)
   record ShaderSourceKey(Identifier id, ShaderType type) {
      @Override
      public String toString() {
         return this.id + " (" + this.type + ")";
      }
   }
}
