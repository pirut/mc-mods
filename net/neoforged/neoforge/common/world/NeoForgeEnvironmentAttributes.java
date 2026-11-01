package net.neoforged.neoforge.common.world;

import net.minecraft.resources.Identifier;
import net.minecraft.world.attribute.EnvironmentAttribute;

public final class NeoForgeEnvironmentAttributes {
   public static Identifier DEFAULT_CUSTOM_CLOUDS = Identifier.withDefaultNamespace("default");
   public static EnvironmentAttribute<Identifier> CUSTOM_CLOUDS = EnvironmentAttribute.builder(NeoForgeAttributeTypes.IDENTIFIER)
      .defaultValue(DEFAULT_CUSTOM_CLOUDS)
      .syncable()
      .build();
   public static Identifier DEFAULT_CUSTOM_SKYBOX = Identifier.withDefaultNamespace("default");
   public static EnvironmentAttribute<Identifier> CUSTOM_SKYBOX = EnvironmentAttribute.builder(NeoForgeAttributeTypes.IDENTIFIER)
      .defaultValue(DEFAULT_CUSTOM_SKYBOX)
      .syncable()
      .build();
   public static Identifier DEFAULT_CUSTOM_WEATHER_EFFECTS = Identifier.withDefaultNamespace("default");
   public static EnvironmentAttribute<Identifier> CUSTOM_WEATHER_EFFECTS = EnvironmentAttribute.builder(NeoForgeAttributeTypes.IDENTIFIER)
      .defaultValue(DEFAULT_CUSTOM_WEATHER_EFFECTS)
      .syncable()
      .build();

   private NeoForgeEnvironmentAttributes() {
   }
}
