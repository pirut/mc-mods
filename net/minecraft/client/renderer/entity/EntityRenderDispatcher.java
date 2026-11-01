package net.minecraft.client.renderer.entity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class EntityRenderDispatcher implements ResourceManagerReloadListener {
   private Map<EntityType<?>, EntityRenderer<?, ?>> renderers = ImmutableMap.of();
   private Map<PlayerModelType, AvatarRenderer<AbstractClientPlayer>> playerRenderers = Map.of();
   private Map<PlayerModelType, AvatarRenderer<ClientMannequin>> mannequinRenderers = Map.of();
   public final TextureManager textureManager;
   @Nullable
   public Camera camera;
   public Entity crosshairPickEntity;
   private final ItemModelResolver itemModelResolver;
   private final MapRenderer mapRenderer;
   private final BlockRenderDispatcher blockRenderDispatcher;
   private final ItemInHandRenderer itemInHandRenderer;
   private final AtlasManager atlasManager;
   private final Font font;
   public final Options options;
   private final Supplier<EntityModelSet> entityModels;
   private final EquipmentAssetManager equipmentAssets;
   private final PlayerSkinRenderCache playerSkinRenderCache;

   public <E extends Entity> int getPackedLightCoords(E p_114395_, float p_114396_) {
      return this.getRenderer(p_114395_).getPackedLightCoords(p_114395_, p_114396_);
   }

   public EntityRenderDispatcher(
      Minecraft p_234579_,
      TextureManager p_234580_,
      ItemModelResolver p_388181_,
      MapRenderer p_361209_,
      BlockRenderDispatcher p_234582_,
      AtlasManager p_433261_,
      Font p_234583_,
      Options p_234584_,
      Supplier<EntityModelSet> p_387861_,
      EquipmentAssetManager p_386713_,
      PlayerSkinRenderCache p_440218_
   ) {
      this.textureManager = p_234580_;
      this.itemModelResolver = p_388181_;
      this.mapRenderer = p_361209_;
      this.atlasManager = p_433261_;
      this.playerSkinRenderCache = p_440218_;
      this.itemInHandRenderer = new ItemInHandRenderer(p_234579_, this, p_388181_);
      this.blockRenderDispatcher = p_234582_;
      this.font = p_234583_;
      this.options = p_234584_;
      this.entityModels = p_387861_;
      this.equipmentAssets = p_386713_;
   }

   public <T extends Entity> EntityRenderer<? super T, ?> getRenderer(T p_114383_) {
      return (EntityRenderer<? super T, ?>)(switch (p_114383_) {
         case AbstractClientPlayer abstractclientplayer -> this.getAvatarRenderer(
            (Map<PlayerModelType, AvatarRenderer<T>>)this.playerRenderers, (T)abstractclientplayer
         );
         case ClientMannequin clientmannequin -> this.getAvatarRenderer((Map<PlayerModelType, AvatarRenderer<T>>)this.mannequinRenderers, (T)clientmannequin);
         default -> (EntityRenderer)this.renderers.get(p_114383_.getType());
      });
   }

   public AvatarRenderer<AbstractClientPlayer> getPlayerRenderer(AbstractClientPlayer p_446732_) {
      return this.getAvatarRenderer(this.playerRenderers, p_446732_);
   }

   private <T extends Avatar & ClientAvatarEntity> AvatarRenderer<T> getAvatarRenderer(Map<PlayerModelType, AvatarRenderer<T>> p_446539_, T p_446903_) {
      PlayerModelType playermodeltype = p_446903_.getSkin().model();
      AvatarRenderer<T> avatarrenderer = p_446539_.get(playermodeltype);
      return avatarrenderer != null ? avatarrenderer : p_446539_.get(PlayerModelType.WIDE);
   }

   public <S extends EntityRenderState> EntityRenderer<?, ? super S> getRenderer(S p_412312_) {
      if (p_412312_ instanceof AvatarRenderState avatarrenderstate) {
         PlayerModelType playermodeltype = avatarrenderstate.skin.model();
         EntityRenderer<? extends Avatar, ?> entityrenderer = (EntityRenderer<? extends Avatar, ?>)this.playerRenderers.get(playermodeltype);
         return (EntityRenderer<?, ? super S>)(entityrenderer != null ? entityrenderer : (EntityRenderer)this.playerRenderers.get(PlayerModelType.WIDE));
      } else {
         return (EntityRenderer<?, ? super S>)this.renderers.get(p_412312_.entityType);
      }
   }

   public void prepare(Camera p_114410_, Entity p_114411_) {
      this.camera = p_114410_;
      this.crosshairPickEntity = p_114411_;
   }

   public <E extends Entity> boolean shouldRender(E p_114398_, Frustum p_114399_, double p_114400_, double p_114401_, double p_114402_) {
      EntityRenderer<? super E, ?> entityrenderer = this.getRenderer(p_114398_);
      return entityrenderer.shouldRender(p_114398_, p_114399_, p_114400_, p_114401_, p_114402_);
   }

   public <E extends Entity> EntityRenderState extractEntity(E p_433543_, float p_435691_) {
      EntityRenderer<? super E, ?> entityrenderer = this.getRenderer(p_433543_);

      try {
         return entityrenderer.createRenderState(p_433543_, p_435691_);
      } catch (Throwable var8) {
         CrashReport crashreport = CrashReport.forThrowable(var8, "Extracting render state for an entity in world");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Entity being extracted");
         p_433543_.fillCrashReportCategory(crashreportcategory);
         CrashReportCategory crashreportcategory1 = this.fillRendererDetails(entityrenderer, crashreport);
         crashreportcategory1.setDetail("Delta", p_435691_);
         throw new ReportedException(crashreport);
      }
   }

   public <S extends EntityRenderState> void submit(
      S p_435745_, CameraRenderState p_451011_, double p_432978_, double p_435790_, double p_434208_, PoseStack p_435965_, SubmitNodeCollector p_434310_
   ) {
      EntityRenderer<?, ? super S> entityrenderer = this.getRenderer(p_435745_);

      try {
         Vec3 vec3 = entityrenderer.getRenderOffset(p_435745_);
         double d2 = p_432978_ + vec3.x();
         double d0 = p_435790_ + vec3.y();
         double d1 = p_434208_ + vec3.z();
         p_435965_.pushPose();
         p_435965_.translate(d2, d0, d1);
         entityrenderer.submit(p_435745_, p_435965_, p_434310_, p_451011_);
         if (p_435745_.displayFireAnimation) {
            p_434310_.submitFlame(p_435965_, p_435745_, Mth.rotationAroundAxis(Mth.Y_AXIS, p_451011_.orientation, new Quaternionf()));
         }

         if (p_435745_ instanceof AvatarRenderState) {
            p_435965_.translate(-vec3.x(), -vec3.y(), -vec3.z());
         }

         if (!p_435745_.shadowPieces.isEmpty()) {
            p_434310_.submitShadow(p_435965_, p_435745_.shadowRadius, p_435745_.shadowPieces);
         }

         if (!(p_435745_ instanceof AvatarRenderState)) {
            p_435965_.translate(-vec3.x(), -vec3.y(), -vec3.z());
         }

         p_435965_.popPose();
      } catch (Throwable var19) {
         CrashReport crashreport = CrashReport.forThrowable(var19, "Rendering entity in world");
         CrashReportCategory crashreportcategory = crashreport.addCategory("EntityRenderState being rendered");
         p_435745_.fillCrashReportCategory(crashreportcategory);
         this.fillRendererDetails(entityrenderer, crashreport);
         throw new ReportedException(crashreport);
      }
   }

   private <S extends EntityRenderState> CrashReportCategory fillRendererDetails(EntityRenderer<?, S> p_412221_, CrashReport p_412241_) {
      CrashReportCategory crashreportcategory = p_412241_.addCategory("Renderer details");
      crashreportcategory.setDetail("Assigned renderer", p_412221_);
      return crashreportcategory;
   }

   public void resetCamera() {
      this.camera = null;
   }

   public double distanceToSqr(Entity p_114472_) {
      return this.camera.position().distanceToSqr(p_114472_.position());
   }

   public ItemInHandRenderer getItemInHandRenderer() {
      return this.itemInHandRenderer;
   }

   public Map<PlayerModelType, EntityRenderer<? extends Avatar, ?>> getPlayerRenderers() {
      return Collections.unmodifiableMap(this.playerRenderers);
   }

   public Map<PlayerModelType, EntityRenderer<? extends Avatar, ?>> getMannequinRenderers() {
      return Collections.unmodifiableMap(this.mannequinRenderers);
   }

   @Override
   public void onResourceManagerReload(ResourceManager p_174004_) {
      EntityRendererProvider.Context entityrendererprovider$context = new EntityRendererProvider.Context(
         this,
         this.itemModelResolver,
         this.mapRenderer,
         this.blockRenderDispatcher,
         p_174004_,
         this.entityModels.get(),
         this.equipmentAssets,
         this.atlasManager,
         this.font,
         this.playerSkinRenderCache
      );
      this.renderers = EntityRenderers.createEntityRenderers(entityrendererprovider$context);
      this.playerRenderers = EntityRenderers.createAvatarRenderers(entityrendererprovider$context);
      this.mannequinRenderers = EntityRenderers.createAvatarRenderers(entityrendererprovider$context);
      ModLoader.postEvent(new EntityRenderersEvent.AddLayers(this.renderers, this.playerRenderers, this.mannequinRenderers, entityrendererprovider$context));
   }
}
