package net.neoforged.neoforge.client.pipeline;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

@FunctionalInterface
public interface PipelineModifier {
   ResourceKey<Registry<PipelineModifier>> MODIFIERS_KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath("neoforge", "pipeline_modifiers"));

   RenderPipeline apply(RenderPipeline var1, Identifier var2);
}
