package io.github.pirut.chunks3.platform;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import javax.annotation.Nullable;
//#if MC >= 1.21.11
//$$ import java.util.function.Function;
//$$ import net.minecraft.client.renderer.block.model.BlockModelPart;
//$$ import net.minecraft.client.renderer.block.model.BlockStateModel;
//$$ import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
//#else
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
//#endif
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Client calls that differ between NeoForge and Fabric: NeoForge's block models take extra model
 * data and choose their own render layers, and fluids name their textures and tints through
 * NeoForge's fluid types; on Fabric these are vanilla's (and Fabric API's fluid handlers).
 * Model data travels as a plain object, null for none.
 */
public interface ClientPlatform {
    ClientPlatform INSTANCE = Loader.load(ClientPlatform.class);

    //#if MC >= 1.21.11
    //$$ /** The parts a block model draws at a spot (NeoForge's models can look at the level), added to {@code parts}. */
    //$$ void collectParts(BlockStateModel model, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockModelPart> parts);
    //$$
    //$$ /** The render layer a part of a block's model draws into. */
    //$$ ChunkSectionLayer layer(BlockModelPart part, BlockState state);
    //$$
    //$$ TextureAtlasSprite particleIcon(BlockStateModel model, BlockAndTintGetter level, BlockPos pos, BlockState state);
    //$$
    //$$ /** Draws a block model's parts into the builders for their layers, culling faces against neighbours. */
    //$$ default void renderBatched(BlockRenderDispatcher blocks, BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack pose,
    //$$                            Function<ChunkSectionLayer, VertexConsumer> builders, List<BlockModelPart> parts) {
    //$$     blocks.renderBatched(state, pos, level, pose, builders, true, parts);
    //$$ }
    //$$
    //#else
    /** A block entity's model data, or null when it has none. */
    @Nullable Object blockEntityModelData(BlockEntity entity);

    /** The model data a block model draws with at a spot, from what its block entity supplied (or null). */
    @Nullable Object modelData(BakedModel model, BlockAndTintGetter level, BlockPos pos, BlockState state, @Nullable Object fromEntity);

    /** The render layers a block model draws into. */
    Iterable<RenderType> renderTypes(BakedModel model, BlockState state, RandomSource random, @Nullable Object data);

    List<BakedQuad> quads(BakedModel model, BlockState state, @Nullable Direction side, RandomSource random, @Nullable Object data, @Nullable RenderType type);

    TextureAtlasSprite particleIcon(BakedModel model);

    /** Draws a block model into a builder, culling faces against neighbours. */
    void renderBatched(BlockRenderDispatcher blocks, BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack pose,
                       VertexConsumer builder, RandomSource random, @Nullable Object data, RenderType type);

    //#endif

    /** A fluid's colour multiplier at a spot (ARGB; -1 for none). */
    int fluidTint(FluidState fluid, BlockAndTintGetter level, BlockPos pos);

    /** A fluid's still texture on the block atlas. */
    TextureAtlasSprite fluidStill(FluidState fluid, BlockAndTintGetter level, BlockPos pos);

    /** A sprite's pixels as loaded, before mipmaps. */
    NativeImage originalImage(SpriteContents contents);

}
