package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.skull.DragonHeadModel;
import net.minecraft.client.model.object.skull.PiglinHeadModel;
import net.minecraft.client.model.object.skull.SkullModel;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.SkullBlockRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SkullBlockRenderer implements BlockEntityRenderer<SkullBlockEntity, SkullBlockRenderState> {
   private final Function<SkullBlock.Type, SkullModelBase> modelByType;
   public static final Map<SkullBlock.Type, Identifier> SKIN_BY_TYPE = Util.make(Maps.newHashMap(), p_465624_ -> {
      p_465624_.put(SkullBlock.Types.SKELETON, Identifier.withDefaultNamespace("textures/entity/skeleton/skeleton.png"));
      p_465624_.put(SkullBlock.Types.WITHER_SKELETON, Identifier.withDefaultNamespace("textures/entity/skeleton/wither_skeleton.png"));
      p_465624_.put(SkullBlock.Types.ZOMBIE, Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png"));
      p_465624_.put(SkullBlock.Types.CREEPER, Identifier.withDefaultNamespace("textures/entity/creeper/creeper.png"));
      p_465624_.put(SkullBlock.Types.DRAGON, Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon.png"));
      p_465624_.put(SkullBlock.Types.PIGLIN, Identifier.withDefaultNamespace("textures/entity/piglin/piglin.png"));
      p_465624_.put(SkullBlock.Types.PLAYER, DefaultPlayerSkin.getDefaultTexture());
   });
   private final PlayerSkinRenderCache playerSkinRenderCache;

   @Nullable
   public static SkullModelBase createModel(EntityModelSet p_387840_, SkullBlock.Type p_388801_) {
      if (p_388801_ instanceof SkullBlock.Types skullblock$types) {
         return (SkullModelBase)(switch (skullblock$types) {
            case SKELETON -> new SkullModel(p_387840_.bakeLayer(ModelLayers.SKELETON_SKULL));
            case WITHER_SKELETON -> new SkullModel(p_387840_.bakeLayer(ModelLayers.WITHER_SKELETON_SKULL));
            case PLAYER -> new SkullModel(p_387840_.bakeLayer(ModelLayers.PLAYER_HEAD));
            case ZOMBIE -> new SkullModel(p_387840_.bakeLayer(ModelLayers.ZOMBIE_HEAD));
            case CREEPER -> new SkullModel(p_387840_.bakeLayer(ModelLayers.CREEPER_HEAD));
            case DRAGON -> new DragonHeadModel(p_387840_.bakeLayer(ModelLayers.DRAGON_SKULL));
            case PIGLIN -> new PiglinHeadModel(p_387840_.bakeLayer(ModelLayers.PIGLIN_HEAD));
         });
      } else {
         return ClientHooks.getModdedSkullModel(p_387840_, p_388801_);
      }
   }

   public SkullBlockRenderer(BlockEntityRendererProvider.Context p_173660_) {
      EntityModelSet entitymodelset = p_173660_.entityModelSet();
      this.playerSkinRenderCache = p_173660_.playerSkinRenderCache();
      this.modelByType = Util.memoize(p_477732_ -> createModel(entitymodelset, p_477732_));
   }

   public SkullBlockRenderState createRenderState() {
      return new SkullBlockRenderState();
   }

   public void extractRenderState(
      SkullBlockEntity p_446674_, SkullBlockRenderState p_446649_, float p_447371_, Vec3 p_445526_, @Nullable ModelFeatureRenderer.CrumblingOverlay p_446270_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_446674_, p_446649_, p_447371_, p_445526_, p_446270_);
      p_446649_.animationProgress = p_446674_.getAnimation(p_447371_);
      BlockState blockstate = p_446674_.getBlockState();
      boolean flag = blockstate.getBlock() instanceof WallSkullBlock;
      p_446649_.direction = flag ? blockstate.getValue(WallSkullBlock.FACING) : null;
      int i = flag ? RotationSegment.convertToSegment(p_446649_.direction.getOpposite()) : blockstate.getValue(SkullBlock.ROTATION);
      p_446649_.rotationDegrees = RotationSegment.convertToDegrees(i);
      p_446649_.skullType = ((AbstractSkullBlock)blockstate.getBlock()).getType();
      p_446649_.renderType = this.resolveSkullRenderType(p_446649_.skullType, p_446674_);
   }

   public void submit(SkullBlockRenderState p_445740_, PoseStack p_440693_, SubmitNodeCollector p_440619_, CameraRenderState p_451265_) {
      SkullModelBase skullmodelbase = this.modelByType.apply(p_445740_.skullType);
      submitSkull(
         p_445740_.direction,
         p_445740_.rotationDegrees,
         p_445740_.animationProgress,
         p_440693_,
         p_440619_,
         p_445740_.lightCoords,
         skullmodelbase,
         p_445740_.renderType,
         0,
         p_445740_.breakProgress
      );
   }

   public static void submitSkull(
      @Nullable Direction p_440001_,
      float p_432798_,
      float p_434724_,
      PoseStack p_433744_,
      SubmitNodeCollector p_434108_,
      int p_433427_,
      SkullModelBase p_478700_,
      RenderType p_458939_,
      int p_433497_,
      @Nullable ModelFeatureRenderer.CrumblingOverlay p_442757_
   ) {
      p_433744_.pushPose();
      if (p_440001_ == null) {
         p_433744_.translate(0.5F, 0.0F, 0.5F);
      } else {
         float f = 0.25F;
         p_433744_.translate(0.5F - p_440001_.getStepX() * 0.25F, 0.25F, 0.5F - p_440001_.getStepZ() * 0.25F);
      }

      p_433744_.scale(-1.0F, -1.0F, 1.0F);
      SkullModelBase.State skullmodelbase$state = new SkullModelBase.State();
      skullmodelbase$state.animationPos = p_434724_;
      skullmodelbase$state.yRot = p_432798_;
      p_434108_.submitModel(p_478700_, skullmodelbase$state, p_433744_, p_458939_, p_433427_, OverlayTexture.NO_OVERLAY, p_433497_, p_442757_);
      p_433744_.popPose();
   }

   private RenderType resolveSkullRenderType(SkullBlock.Type p_439442_, SkullBlockEntity p_439775_) {
      if (p_439442_ == SkullBlock.Types.PLAYER) {
         ResolvableProfile resolvableprofile = p_439775_.getOwnerProfile();
         if (resolvableprofile != null) {
            return this.playerSkinRenderCache.getOrDefault(resolvableprofile).renderType();
         }
      }

      return getSkullRenderType(p_439442_, null);
   }

   public static RenderType getSkullRenderType(SkullBlock.Type p_428198_, @Nullable Identifier p_468625_) {
      return RenderTypes.entityCutoutNoCullZOffset(p_468625_ != null ? p_468625_ : SKIN_BY_TYPE.get(p_428198_));
   }

   public static RenderType getPlayerSkinRenderType(Identifier p_468590_) {
      return RenderTypes.entityTranslucent(p_468590_);
   }

   public AABB getRenderBoundingBox(SkullBlockEntity blockEntity) {
      SkullBlock.Type type = ((AbstractSkullBlock)blockEntity.getBlockState().getBlock()).getType();
      if (type == SkullBlock.Types.DRAGON) {
         BlockPos pos = blockEntity.getBlockPos();
         return new AABB(pos.getX() - 0.75, pos.getY() - 0.35, pos.getZ() - 0.75, pos.getX() + 1.75, pos.getY() + 1.0, pos.getZ() + 1.75);
      } else {
         return BlockEntityRenderer.super.getRenderBoundingBox(blockEntity);
      }
   }
}
