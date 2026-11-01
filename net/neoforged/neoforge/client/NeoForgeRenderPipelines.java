package net.neoforged.neoforge.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

@EventBusSubscriber(
   value = {Dist.CLIENT},
   modid = "neoforge"
)
public final class NeoForgeRenderPipelines {
   public static final RenderPipeline ENTITY_UNLIT_TRANSLUCENT = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
      .withLocation(Identifier.parse("neoforge:pipeline/entity_unlit_translucent"))
      .withShaderDefine("ALPHA_CUTOUT", 0.1F)
      .withShaderDefine("NO_CARDINAL_LIGHTING")
      .withSampler("Sampler1")
      .withBlend(BlendFunction.TRANSLUCENT)
      .withCull(false)
      .build();
   public static final RenderPipeline ENTITY_TRANSLUCENT_CULL = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
      .withLocation(Identifier.parse("neoforge:pipeline/entity_translucent_cull"))
      .withShaderDefine("ALPHA_CUTOUT", 0.1F)
      .withSampler("Sampler1")
      .withBlend(BlendFunction.TRANSLUCENT)
      .build();
   public static final RenderPipeline ENTITY_SMOOTH_CUTOUT_CULL = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
      .withLocation(Identifier.parse("neoforge:pipeline/entity_smooth_cutout_cull"))
      .withShaderDefine("ALPHA_CUTOUT", 0.1F)
      .withSampler("Sampler1")
      .build();

   @SubscribeEvent
   static void registerPipelines(RegisterRenderPipelinesEvent event) {
      event.registerPipeline(ENTITY_UNLIT_TRANSLUCENT);
      event.registerPipeline(ENTITY_TRANSLUCENT_CULL);
      event.registerPipeline(ENTITY_SMOOTH_CUTOUT_CULL);
   }

   private NeoForgeRenderPipelines() {
   }
}
