package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockEntityRenderers {
   private static final Map<BlockEntityType<?>, BlockEntityRendererProvider<?, ?>> PROVIDERS = new ConcurrentHashMap<>();

   public static <T extends BlockEntity, S extends BlockEntityRenderState> void register(
      BlockEntityType<? extends T> p_173591_, BlockEntityRendererProvider<T, S> p_173592_
   ) {
      PROVIDERS.put(p_173591_, p_173592_);
   }

   public static Map<BlockEntityType<?>, BlockEntityRenderer<?, ?>> createEntityRenderers(BlockEntityRendererProvider.Context p_173599_) {
      Builder<BlockEntityType<?>, BlockEntityRenderer<?, ?>> builder = ImmutableMap.builder();
      PROVIDERS.forEach((p_465622_, p_465623_) -> {
         try {
            builder.put(p_465622_, p_465623_.create(p_173599_));
         } catch (Exception var5) {
            throw new IllegalStateException("Failed to create model for " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey((BlockEntityType<?>)p_465622_), var5);
         }
      });
      return builder.build();
   }

   static {
      register(BlockEntityType.SIGN, SignRenderer::new);
      register(BlockEntityType.HANGING_SIGN, HangingSignRenderer::new);
      register(BlockEntityType.MOB_SPAWNER, SpawnerRenderer::new);
      register(BlockEntityType.PISTON, p_445226_ -> new PistonHeadRenderer());
      register(BlockEntityType.CHEST, ChestRenderer::new);
      register(BlockEntityType.ENDER_CHEST, ChestRenderer::new);
      register(BlockEntityType.TRAPPED_CHEST, ChestRenderer::new);
      register(BlockEntityType.ENCHANTING_TABLE, EnchantTableRenderer::new);
      register(BlockEntityType.LECTERN, LecternRenderer::new);
      register(BlockEntityType.END_PORTAL, p_445221_ -> new TheEndPortalRenderer());
      register(BlockEntityType.END_GATEWAY, p_445222_ -> new TheEndGatewayRenderer());
      register(BlockEntityType.BEACON, p_445223_ -> new BeaconRenderer());
      register(BlockEntityType.SKULL, SkullBlockRenderer::new);
      register(BlockEntityType.BANNER, BannerRenderer::new);
      register(BlockEntityType.STRUCTURE_BLOCK, p_445224_ -> new BlockEntityWithBoundingBoxRenderer());
      register(BlockEntityType.TEST_INSTANCE_BLOCK, p_454262_ -> new TestInstanceRenderer());
      register(BlockEntityType.SHULKER_BOX, ShulkerBoxRenderer::new);
      register(BlockEntityType.BED, BedRenderer::new);
      register(BlockEntityType.CONDUIT, ConduitRenderer::new);
      register(BlockEntityType.BELL, BellRenderer::new);
      register(BlockEntityType.CAMPFIRE, CampfireRenderer::new);
      register(BlockEntityType.BRUSHABLE_BLOCK, BrushableBlockRenderer::new);
      register(BlockEntityType.DECORATED_POT, DecoratedPotRenderer::new);
      register(BlockEntityType.TRIAL_SPAWNER, TrialSpawnerRenderer::new);
      register(BlockEntityType.VAULT, VaultRenderer::new);
      register(BlockEntityType.COPPER_GOLEM_STATUE, CopperGolemStatueBlockRenderer::new);
      register(BlockEntityType.SHELF, ShelfRenderer::new);
   }
}
