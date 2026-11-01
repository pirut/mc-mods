package io.github.pirut.chunks3.neoforge;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.pirut.chunks3.platform.ClientPlatform;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
//#if MC >= 1.21.11
//$$ import net.minecraft.client.renderer.block.model.BlockModelPart;
//$$ import net.minecraft.client.renderer.block.model.BlockStateModel;
//$$ import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
//$$ import net.minecraft.data.AtlasIds;
//#else
import net.minecraft.client.renderer.RenderType;
//#endif
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
//#if MC < 1.21.11
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.model.data.ModelData;
//#endif
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/** {@link ClientPlatform} on NeoForge: model data, model render types and fluid type extensions. */
public final class NeoForgeClientPlatform implements ClientPlatform {
    //#if MC >= 1.21.11
    //$$ // NeoForge 21.11 models read their model data from the level themselves, through collectParts.
    //$$ @Override public void collectParts(BlockStateModel model, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockModelPart> parts) {
    //$$     model.collectParts(level, pos, state, random, parts);
    //$$ }
    //$$
    //$$ @Override public ChunkSectionLayer layer(BlockModelPart part, BlockState state) { return part.getRenderType(state); }
    //$$
    //$$ @Override public TextureAtlasSprite particleIcon(BlockStateModel model, BlockAndTintGetter level, BlockPos pos, BlockState state) {
    //$$     return model.particleIcon(level, pos, state);
    //$$ }
    //$$
    //$$ @Override public void renderBatched(BlockRenderDispatcher blocks, BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack pose,
    //$$                                    java.util.function.Function<ChunkSectionLayer, VertexConsumer> builders, List<BlockModelPart> parts) {
    //$$     blocks.renderBatched(state, pos, level, pose, builders, true, parts);
    //$$ }
    //$$
    //#else
    private static ModelData data(@Nullable Object data) { return data == null ? ModelData.EMPTY : (ModelData) data; }

    @Override @Nullable public Object blockEntityModelData(BlockEntity entity) {
        ModelData model = entity.getModelData();
        return model == ModelData.EMPTY ? null : model;
    }

    @Override public Object modelData(BakedModel model, BlockAndTintGetter level, BlockPos pos, BlockState state, @Nullable Object fromEntity) {
        return model.getModelData(level, pos, state, data(fromEntity));
    }

    @Override public Iterable<RenderType> renderTypes(BakedModel model, BlockState state, RandomSource random, @Nullable Object data) {
        return model.getRenderTypes(state, random, data(data));
    }

    @Override public List<BakedQuad> quads(BakedModel model, BlockState state, @Nullable Direction side, RandomSource random, @Nullable Object data, @Nullable RenderType type) {
        return model.getQuads(state, side, random, data(data), type);
    }

    @Override public TextureAtlasSprite particleIcon(BakedModel model) { return model.getParticleIcon(ModelData.EMPTY); }

    @Override public void renderBatched(BlockRenderDispatcher blocks, BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack pose,
                                        VertexConsumer builder, RandomSource random, @Nullable Object data, RenderType type) {
        blocks.renderBatched(state, pos, level, pose, builder, true, random, data(data), type);
    }

    //#endif

    @Override public int fluidTint(FluidState fluid, BlockAndTintGetter level, BlockPos pos) {
        return IClientFluidTypeExtensions.of(fluid).getTintColor(fluid, level, pos);
    }

    @Override public TextureAtlasSprite fluidStill(FluidState fluid, BlockAndTintGetter level, BlockPos pos) {
        //#if MC >= 1.21.11
        //$$ return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
        //#else
        return Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS)
        //#endif
                .getSprite(IClientFluidTypeExtensions.of(fluid).getStillTexture(fluid, level, pos));
    }

    @Override public NativeImage originalImage(SpriteContents contents) { return contents.getOriginalImage(); }

}
