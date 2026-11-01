package net.minecraft.client.tutorial;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.ClientInput;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

@Environment(EnvType.CLIENT)
public interface TutorialStepInstance {
   default void clear() {
   }

   default void tick() {
   }

   default void onInput(ClientInput input) {
   }

   default void onMouse(double velocityX, double velocityY) {
   }

   default void onLookAt(ClientLevel level, HitResult result) {
   }

   default void onDestroyBlock(ClientLevel level, BlockPos pos, BlockState state, float diggingStage) {
   }

   default void onOpenInventory() {
   }

   default void onGetItem(ItemStack stack) {
   }
}
