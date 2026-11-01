package net.minecraft.client.renderer.block.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.model.NeoForgeModelProperties;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record SimpleModelWrapper(QuadCollection quads, boolean useAmbientOcclusion, TextureAtlasSprite particleIcon, @Nullable ChunkSectionLayer renderType)
   implements BlockModelPart {
   private static final Logger LOGGER = LogUtils.getLogger();

   @Deprecated
   public SimpleModelWrapper(QuadCollection quads, boolean useAmbientOcclusion, TextureAtlasSprite particleIcon) {
      this(quads, useAmbientOcclusion, particleIcon, null);
   }

   public static BlockModelPart bake(ModelBaker p_405335_, Identifier p_468685_, ModelState p_405869_) {
      ResolvedModel resolvedmodel = p_405335_.getModel(p_468685_);
      return bake(p_405335_, resolvedmodel, p_405869_);
   }

   public static BlockModelPart bake(ModelBaker p_405335_, ResolvedModel resolvedmodel, ModelState p_405869_) {
      TextureSlots textureslots = resolvedmodel.getTopTextureSlots();
      boolean flag = resolvedmodel.getTopAmbientOcclusion();
      TextureAtlasSprite textureatlassprite = resolvedmodel.resolveParticleSprite(textureslots, p_405335_);
      QuadCollection quadcollection = resolvedmodel.bakeTopGeometry(textureslots, p_405335_, p_405869_);
      Multimap<Identifier, Identifier> multimap = null;

      for (BakedQuad bakedquad : quadcollection.getAll()) {
         TextureAtlasSprite textureatlassprite1 = bakedquad.sprite();
         if (!textureatlassprite1.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)) {
            if (multimap == null) {
               multimap = HashMultimap.create();
            }

            multimap.put(textureatlassprite1.atlasLocation(), textureatlassprite1.contents().name());
         }
      }

      if (multimap != null) {
         LOGGER.warn("Rejecting block model {}, since it contains sprites from outside of supported atlas: {}", resolvedmodel.debugName(), multimap);
         return p_405335_.missingBlockModelPart();
      } else {
         RenderTypeGroup renderTypeGroup = resolvedmodel.getTopAdditionalProperties().getOptional(NeoForgeModelProperties.RENDER_TYPE);
         ChunkSectionLayer renderTypes = renderTypeGroup != null && !renderTypeGroup.isEmpty() ? renderTypeGroup.block() : null;
         return new SimpleModelWrapper(quadcollection, flag, textureatlassprite, renderTypes);
      }
   }

   @Override
   public List<BakedQuad> getQuads(@Nullable Direction p_405263_) {
      return this.quads.getQuads(p_405263_);
   }

   @Override
   public ChunkSectionLayer getRenderType(BlockState state) {
      return this.renderType != null ? this.renderType : BlockModelPart.super.getRenderType(state);
   }
}
