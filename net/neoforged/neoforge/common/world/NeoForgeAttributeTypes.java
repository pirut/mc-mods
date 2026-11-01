package net.neoforged.neoforge.common.world;

import net.minecraft.resources.Identifier;
import net.minecraft.world.attribute.AttributeType;

public final class NeoForgeAttributeTypes {
   public static final AttributeType<Identifier> IDENTIFIER = AttributeType.ofNotInterpolated(Identifier.CODEC);

   private NeoForgeAttributeTypes() {
   }
}
