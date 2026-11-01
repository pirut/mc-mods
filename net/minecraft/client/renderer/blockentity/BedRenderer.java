package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.function.Consumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BedRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.Direction;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BedRenderer implements BlockEntityRenderer<BedBlockEntity, BedRenderState> {
   private final MaterialSet materials;
   private final Model.Simple headModel;
   private final Model.Simple footModel;

   public BedRenderer(BlockEntityRendererProvider.Context p_173540_) {
      this(p_173540_.materials(), p_173540_.entityModelSet());
   }

   public BedRenderer(SpecialModelRenderer.BakingContext p_433173_) {
      this(p_433173_.materials(), p_433173_.entityModelSet());
   }

   public BedRenderer(MaterialSet p_434060_, EntityModelSet p_433002_) {
      this.materials = p_434060_;
      this.headModel = new Model.Simple(p_433002_.bakeLayer(ModelLayers.BED_HEAD), RenderTypes::entitySolid);
      this.footModel = new Model.Simple(p_433002_.bakeLayer(ModelLayers.BED_FOOT), RenderTypes::entitySolid);
   }

   public static LayerDefinition createHeadLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild("main", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 16.0F, 16.0F, 6.0F), PartPose.ZERO);
      partdefinition.addOrReplaceChild(
         "left_leg",
         CubeListBuilder.create().texOffs(50, 6).addBox(0.0F, 6.0F, 0.0F, 3.0F, 3.0F, 3.0F),
         PartPose.rotation((float) (Math.PI / 2), 0.0F, (float) (Math.PI / 2))
      );
      partdefinition.addOrReplaceChild(
         "right_leg",
         CubeListBuilder.create().texOffs(50, 18).addBox(-16.0F, 6.0F, 0.0F, 3.0F, 3.0F, 3.0F),
         PartPose.rotation((float) (Math.PI / 2), 0.0F, (float) Math.PI)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public static LayerDefinition createFootLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild("main", CubeListBuilder.create().texOffs(0, 22).addBox(0.0F, 0.0F, 0.0F, 16.0F, 16.0F, 6.0F), PartPose.ZERO);
      partdefinition.addOrReplaceChild(
         "left_leg", CubeListBuilder.create().texOffs(50, 0).addBox(0.0F, 6.0F, -16.0F, 3.0F, 3.0F, 3.0F), PartPose.rotation((float) (Math.PI / 2), 0.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "right_leg",
         CubeListBuilder.create().texOffs(50, 12).addBox(-16.0F, 6.0F, -16.0F, 3.0F, 3.0F, 3.0F),
         PartPose.rotation((float) (Math.PI / 2), 0.0F, (float) (Math.PI * 3.0 / 2.0))
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public BedRenderState createRenderState() {
      return new BedRenderState();
   }

   public void extractRenderState(
      BedBlockEntity p_445886_, BedRenderState p_447090_, float p_446336_, Vec3 p_445891_, @Nullable ModelFeatureRenderer.CrumblingOverlay p_446152_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_445886_, p_447090_, p_446336_, p_445891_, p_446152_);
      p_447090_.color = p_445886_.getColor();
      p_447090_.facing = p_445886_.getBlockState().getValue(BedBlock.FACING);
      p_447090_.isHead = p_445886_.getBlockState().getValue(BedBlock.PART) == BedPart.HEAD;
      if (p_445886_.getLevel() != null) {
         DoubleBlockCombiner.NeighborCombineResult<? extends BedBlockEntity> neighborcombineresult = DoubleBlockCombiner.combineWithNeigbour(
            BlockEntityType.BED,
            BedBlock::getBlockType,
            BedBlock::getConnectedDirection,
            ChestBlock.FACING,
            p_445886_.getBlockState(),
            p_445886_.getLevel(),
            p_445886_.getBlockPos(),
            (p_112202_, p_112203_) -> false
         );
         p_447090_.lightCoords = neighborcombineresult.apply(new BrightnessCombiner<>()).get(p_447090_.lightCoords);
      }
   }

   public void submit(BedRenderState p_445609_, PoseStack p_439782_, SubmitNodeCollector p_439369_, CameraRenderState p_451216_) {
      Material material = Sheets.getBedMaterial(p_445609_.color);
      this.submitPiece(
         p_439782_,
         p_439369_,
         p_445609_.isHead ? this.headModel : this.footModel,
         p_445609_.facing,
         material,
         p_445609_.lightCoords,
         OverlayTexture.NO_OVERLAY,
         false,
         p_445609_.breakProgress,
         0
      );
   }

   public void submitSpecial(PoseStack p_439640_, SubmitNodeCollector p_439587_, int p_439564_, int p_440267_, Material p_440126_, int p_451664_) {
      this.submitPiece(p_439640_, p_439587_, this.headModel, Direction.SOUTH, p_440126_, p_439564_, p_440267_, false, null, p_451664_);
      this.submitPiece(p_439640_, p_439587_, this.footModel, Direction.SOUTH, p_440126_, p_439564_, p_440267_, true, null, p_451664_);
   }

   private void submitPiece(
      PoseStack p_440523_,
      SubmitNodeCollector p_440584_,
      Model.Simple p_440701_,
      Direction p_439931_,
      Material p_439303_,
      int p_438920_,
      int p_439582_,
      boolean p_440123_,
      @Nullable ModelFeatureRenderer.CrumblingOverlay p_439398_,
      int p_451666_
   ) {
      p_440523_.pushPose();
      preparePose(p_440523_, p_440123_, p_439931_);
      p_440584_.submitModel(
         p_440701_,
         Unit.INSTANCE,
         p_440523_,
         p_439303_.renderType(RenderTypes::entitySolid),
         p_438920_,
         p_439582_,
         -1,
         this.materials.get(p_439303_),
         p_451666_,
         p_439398_
      );
      p_440523_.popPose();
   }

   private static void preparePose(PoseStack p_428361_, boolean p_428252_, Direction p_428231_) {
      p_428361_.translate(0.0F, 0.5625F, p_428252_ ? -1.0F : 0.0F);
      p_428361_.mulPose(Axis.XP.rotationDegrees(90.0F));
      p_428361_.translate(0.5F, 0.5F, 0.5F);
      p_428361_.mulPose(Axis.ZP.rotationDegrees(180.0F + p_428231_.toYRot()));
      p_428361_.translate(-0.5F, -0.5F, -0.5F);
   }

   public void getExtents(Consumer<Vector3fc> p_470840_) {
      PoseStack posestack = new PoseStack();
      preparePose(posestack, false, Direction.SOUTH);
      this.headModel.root().getExtentsForGui(posestack, p_470840_);
      posestack.setIdentity();
      preparePose(posestack, true, Direction.SOUTH);
      this.footModel.root().getExtentsForGui(posestack, p_470840_);
   }
}
