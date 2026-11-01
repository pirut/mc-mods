package net.neoforged.neoforge.client.extensions.common;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector3d;

public interface IClientBlockExtensions {
   IClientBlockExtensions DEFAULT = new IClientBlockExtensions() {};

   static IClientBlockExtensions of(BlockState state) {
      return of(state.getBlock());
   }

   static IClientBlockExtensions of(Block block) {
      return ClientExtensionsManager.BLOCK_EXTENSIONS.getOrDefault(block, DEFAULT);
   }

   default boolean addHitEffects(BlockState state, Level level, HitResult target, ParticleEngine manager) {
      return false;
   }

   default boolean addDestroyEffects(BlockState state, Level Level, BlockPos pos, ParticleEngine manager) {
      return !state.shouldSpawnTerrainParticles();
   }

   default boolean playBreakSound(BlockState state, Level level, BlockPos pos) {
      return false;
   }

   default Vector3d getFogColor(BlockState state, LevelReader level, BlockPos pos, Entity entity, Vector3d originalColor, float partialTick) {
      FluidState fluidState = level.getFluidState(pos);
      if (fluidState.is(FluidTags.WATER)) {
         float f12 = 0.0F;
         if (entity instanceof LivingEntity ent) {
            AttributeInstance attributeinstance = ent.getAttribute(Attributes.OXYGEN_BONUS);
            f12 = (float)(attributeinstance != null ? attributeinstance.getValue() : 0.0) * 0.2F;
            if (ent.hasEffect(MobEffects.WATER_BREATHING)) {
               f12 = f12 * 0.3F + 0.6F;
            }
         }

         return new Vector3d(0.02F + f12, 0.02F + f12, 0.2F + f12);
      } else {
         return fluidState.is(FluidTags.LAVA) ? new Vector3d(0.6F, 0.1F, 0.0) : originalColor;
      }
   }

   default boolean areBreakingParticlesTinted(BlockState state, ClientLevel level, BlockPos pos) {
      return !state.is(Blocks.GRASS_BLOCK);
   }
}
