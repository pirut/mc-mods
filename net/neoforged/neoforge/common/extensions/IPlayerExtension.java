package net.neoforged.neoforge.common.extensions;

import java.util.OptionalInt;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;

public interface IPlayerExtension {
   private Player self() {
      return (Player)this;
   }

   default boolean isCloseEnough(Entity entity, double dist) {
      Vec3 eye = this.self().getEyePosition();
      AABB aabb = entity.getBoundingBox().inflate(entity.getPickRadius());
      return aabb.distanceToSqr(eye) < dist * dist;
   }

   default OptionalInt openMenu(MenuProvider menuProvider, BlockPos pos) {
      return this.openMenu(menuProvider, buf -> buf.writeBlockPos(pos));
   }

   default OptionalInt openMenu(MenuProvider menuProvider, Consumer<RegistryFriendlyByteBuf> extraDataWriter) {
      return OptionalInt.empty();
   }

   default boolean mayFly() {
      return this.self().getAbilities().mayfly || this.self().getAttributeValue(NeoForgeMod.CREATIVE_FLIGHT) > 0.0;
   }

   default boolean isFakePlayer() {
      return false;
   }
}
