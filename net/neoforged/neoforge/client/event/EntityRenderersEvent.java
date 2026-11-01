package net.neoforged.neoforge.client.event;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.object.skull.SkullModel;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.client.ClientHooks;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

public abstract class EntityRenderersEvent extends Event implements IModBusEvent {
   @Internal
   protected EntityRenderersEvent() {
   }

   public static class AddLayers extends EntityRenderersEvent {
      private final Map<EntityType<?>, EntityRenderer<?, ?>> renderers;
      private final Map<PlayerModelType, AvatarRenderer<AbstractClientPlayer>> playerRenderers;
      private final Map<PlayerModelType, AvatarRenderer<ClientMannequin>> mannequinRenderers;
      private final EntityRendererProvider.Context context;

      @Internal
      public AddLayers(
         Map<EntityType<?>, EntityRenderer<?, ?>> renderers,
         Map<PlayerModelType, AvatarRenderer<AbstractClientPlayer>> playerRenderers,
         Map<PlayerModelType, AvatarRenderer<ClientMannequin>> mannequinRenderers,
         EntityRendererProvider.Context context
      ) {
         this.renderers = renderers;
         this.playerRenderers = playerRenderers;
         this.mannequinRenderers = mannequinRenderers;
         this.context = context;
      }

      public Set<PlayerModelType> getSkins() {
         return this.playerRenderers.keySet();
      }

      @Nullable
      public <R extends AvatarRenderer<AbstractClientPlayer>> R getPlayerRenderer(PlayerModelType skinModel) {
         return (R)this.playerRenderers.get(skinModel);
      }

      @Nullable
      public <R extends AvatarRenderer<ClientMannequin>> R getMannequinRenderer(PlayerModelType skinModel) {
         return (R)this.mannequinRenderers.get(skinModel);
      }

      public Set<EntityType<?>> getEntityTypes() {
         return this.renderers.keySet();
      }

      @Nullable
      public <T extends Entity, R extends EntityRenderer<T, ?>> R getRenderer(EntityType<? extends T> entityType) {
         return (R)this.renderers.get(entityType);
      }

      public EntityModelSet getEntityModels() {
         return this.context.getModelSet();
      }

      public EntityRendererProvider.Context getContext() {
         return this.context;
      }
   }

   public static class CreateSkullModels extends EntityRenderersEvent {
      private final Map<SkullBlock.Type, Function<EntityModelSet, SkullModelBase>> skullModels;
      private final Map<SkullBlock.Type, Identifier> skullTextures;

      @Internal
      public CreateSkullModels(Map<SkullBlock.Type, Function<EntityModelSet, SkullModelBase>> skullModels, Map<SkullBlock.Type, Identifier> skullTextures) {
         this.skullModels = skullModels;
         this.skullTextures = skullTextures;
      }

      public void registerSkullModel(SkullBlock.Type type, ModelLayerLocation layerLocation, @Nullable Identifier skullTexture) {
         this.registerSkullModel(type, layerLocation, SkullModel::new, skullTexture);
      }

      public void registerSkullModel(
         SkullBlock.Type type, ModelLayerLocation layerLocation, Function<ModelPart, SkullModelBase> factory, @Nullable Identifier skullTexture
      ) {
         this.registerSkullModel(type, modelSet -> factory.apply(modelSet.bakeLayer(layerLocation)), skullTexture);
      }

      public void registerSkullModel(SkullBlock.Type type, Function<EntityModelSet, SkullModelBase> factory, @Nullable Identifier skullTexture) {
         if (type instanceof SkullBlock.Types) {
            throw new IllegalArgumentException("Cannot register skull model for vanilla skull type: " + type.getSerializedName());
         } else if (this.skullModels.putIfAbsent(type, factory) != null) {
            throw new IllegalArgumentException("Factory already registered for provided skull type: " + type.getSerializedName());
         } else if (skullTexture != null) {
            if (this.skullTextures.putIfAbsent(type, skullTexture) != null) {
               throw new IllegalArgumentException("Texture already registered for provided skull type: " + type.getSerializedName());
            }
         }
      }
   }

   public static class RegisterLayerDefinitions extends EntityRenderersEvent {
      public void registerLayerDefinition(ModelLayerLocation layerLocation, Supplier<LayerDefinition> supplier) {
         ClientHooks.registerLayerDefinition(layerLocation, supplier);
      }
   }

   public static class RegisterRenderers extends EntityRenderersEvent {
      public <T extends Entity> void registerEntityRenderer(EntityType<? extends T> entityType, EntityRendererProvider<T> entityRendererProvider) {
         EntityRenderers.register(entityType, entityRendererProvider);
      }

      public <T extends BlockEntity, S extends BlockEntityRenderState> void registerBlockEntityRenderer(
         BlockEntityType<? extends T> blockEntityType, BlockEntityRendererProvider<T, S> blockEntityRendererProvider
      ) {
         BlockEntityRenderers.register(blockEntityType, blockEntityRendererProvider);
      }
   }
}
