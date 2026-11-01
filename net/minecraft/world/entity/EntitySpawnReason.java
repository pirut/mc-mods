package net.minecraft.world.entity;

public enum EntitySpawnReason {
   NATURAL,
   CHUNK_GENERATION,
   SPAWNER,
   STRUCTURE,
   BREEDING,
   MOB_SUMMONED,
   JOCKEY,
   EVENT,
   CONVERSION,
   REINFORCEMENT,
   TRIGGERED,
   BUCKET,
   SPAWN_ITEM_USE,
   COMMAND,
   DISPENSER,
   PATROL,
   TRIAL_SPAWNER,
   LOAD,
   DIMENSION_TRAVEL;

   public static boolean isSpawner(EntitySpawnReason p_362927_) {
      return p_362927_ == SPAWNER || p_362927_ == TRIAL_SPAWNER;
   }

   public static boolean ignoresLightRequirements(EntitySpawnReason p_364180_) {
      return p_364180_ == TRIAL_SPAWNER;
   }
}
