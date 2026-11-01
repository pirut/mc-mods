package net.neoforged.neoforge.common.property;

import net.minecraft.client.resources.model.ModelState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.client.model.data.ModelProperty;

public class Properties {
   public static final BooleanProperty StaticProperty = BooleanProperty.create("static");
   public static final ModelProperty<ModelState> AnimationProperty = new ModelProperty<>();
}
