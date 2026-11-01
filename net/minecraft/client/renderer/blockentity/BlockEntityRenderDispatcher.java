package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BlockEntityRenderDispatcher implements ResourceManagerReloadListener {
   private Map<BlockEntityType<?>, BlockEntityRenderer<?, ?>> renderers = ImmutableMap.of();
   private final Font font;
   private final Supplier<EntityModelSet> entityModelSet;
   private Vec3 cameraPos;
   private final BlockRenderDispatcher blockRenderDispatcher;
   private final ItemModelResolver itemModelResolver;
   private final ItemRenderer itemRenderer;
   private final EntityRenderDispatcher entityRenderer;
   private final MaterialSet materials;
   private final PlayerSkinRenderCache playerSkinRenderCache;

   public BlockEntityRenderDispatcher(
      Font p_234432_,
      Supplier<EntityModelSet> p_234434_,
      BlockRenderDispatcher p_388018_,
      ItemModelResolver p_387266_,
      ItemRenderer p_387651_,
      EntityRenderDispatcher p_387562_,
      MaterialSet p_434137_,
      PlayerSkinRenderCache p_439489_
   ) {
      this.itemRenderer = p_387651_;
      this.itemModelResolver = p_387266_;
      this.entityRenderer = p_387562_;
      this.font = p_234432_;
      this.entityModelSet = p_234434_;
      this.blockRenderDispatcher = p_388018_;
      this.materials = p_434137_;
      this.playerSkinRenderCache = p_439489_;
   }

   @Nullable
   public <E extends BlockEntity, S extends BlockEntityRenderState> BlockEntityRenderer<E, S> getRenderer(E p_112266_) {
      return (BlockEntityRenderer<E, S>)this.renderers.get(p_112266_.getType());
   }

   @Nullable
   public <E extends BlockEntity, S extends BlockEntityRenderState> BlockEntityRenderer<E, S> getRenderer(S p_446792_) {
      return (BlockEntityRenderer<E, S>)this.renderers.get(p_446792_.blockEntityType);
   }

   public void prepare(Camera p_173566_) {
      this.cameraPos = p_173566_.position();
   }

   /** @deprecated */
   @Nullable
   public <E extends BlockEntity, S extends BlockEntityRenderState> S tryExtractRenderState(
      E p_445799_, float p_446989_, @Nullable ModelFeatureRenderer.CrumblingOverlay p_447358_
   ) {
      return this.tryExtractRenderState(p_445799_, p_446989_, p_447358_, null);
   }

   @Nullable
   public <E extends BlockEntity, S extends BlockEntityRenderState> S tryExtractRenderState(
      E p_445799_, float p_446989_, @Nullable ModelFeatureRenderer.CrumblingOverlay p_447358_, @Nullable Frustum frustum
   ) {
      BlockEntityRenderer<E, S> blockentityrenderer = this.getRenderer(p_445799_);
      if (blockentityrenderer == null) {
         return null;
      } else if (!p_445799_.hasLevel() || !p_445799_.getType().isValid(p_445799_.getBlockState())) {
         return null;
      } else if (frustum != null && !frustum.isVisible(blockentityrenderer.getRenderBoundingBox(p_445799_))) {
         return null;
      } else if (!blockentityrenderer.shouldRender(p_445799_, this.cameraPos)) {
         return null;
      } else {
         Vec3 vec3 = this.cameraPos;
         S s = blockentityrenderer.createRenderState();
         blockentityrenderer.extractRenderState(p_445799_, s, p_446989_, vec3, p_447358_);
         return s;
      }
   }

   public <S extends BlockEntityRenderState> void submit(S p_446872_, PoseStack p_440352_, SubmitNodeCollector p_440247_, CameraRenderState p_451247_) {
      BlockEntityRenderer<?, S> blockentityrenderer = this.getRenderer(p_446872_);
      if (blockentityrenderer != null) {
         try {
            blockentityrenderer.submit(p_446872_, p_440352_, p_440247_, p_451247_);
         } catch (Throwable var9) {
            CrashReport crashreport = CrashReport.forThrowable(var9, "Rendering Block Entity");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block Entity Details");
            p_446872_.fillCrashReportCategory(crashreportcategory);
            throw new ReportedException(crashreport);
         }
      }
   }

   @Override
   public void onResourceManagerReload(ResourceManager p_173563_) {
      BlockEntityRendererProvider.Context blockentityrendererprovider$context = new BlockEntityRendererProvider.Context(
         this,
         this.blockRenderDispatcher,
         this.itemModelResolver,
         this.itemRenderer,
         this.entityRenderer,
         this.entityModelSet.get(),
         this.font,
         this.materials,
         this.playerSkinRenderCache
      );
      this.renderers = BlockEntityRenderers.createEntityRenderers(blockentityrendererprovider$context);
   }
}
