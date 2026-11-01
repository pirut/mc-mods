package net.neoforged.neoforge.client.model.standalone;

import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;

public interface UnbakedStandaloneModel<T> extends ResolvableModel {
   T bake(ModelBaker var1);
}
