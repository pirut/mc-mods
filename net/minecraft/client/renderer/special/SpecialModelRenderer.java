package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.entity.animation.json.AnimationLoader;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface SpecialModelRenderer<T> {
   void submit(@Nullable T var1, ItemDisplayContext var2, PoseStack var3, SubmitNodeCollector var4, int var5, int var6, boolean var7, int var8);

   void getExtents(Consumer<Vector3fc> var1);

   @Nullable
   T extractArgument(ItemStack var1);

   @OnlyIn(Dist.CLIENT)
   public interface BakingContext {
      EntityModelSet entityModelSet();

      MaterialSet materials();

      PlayerSkinRenderCache playerSkinRenderCache();

      default AnimationLoader.PendingAnimations pendingAnimations() {
         return AnimationLoader.PendingAnimations.EMPTY;
      }

      @OnlyIn(Dist.CLIENT)
      public record Simple(
         EntityModelSet entityModelSet, MaterialSet materials, PlayerSkinRenderCache playerSkinRenderCache, AnimationLoader.PendingAnimations pendingAnimations
      ) implements SpecialModelRenderer.BakingContext {
         @Deprecated
         public Simple(EntityModelSet entityModelSet, MaterialSet materials, PlayerSkinRenderCache playerSkinRenderCache) {
            this(entityModelSet, materials, playerSkinRenderCache, AnimationLoader.PendingAnimations.EMPTY);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface Unbaked {
      @Nullable
      SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext var1);

      MapCodec<? extends SpecialModelRenderer.Unbaked> type();
   }
}
