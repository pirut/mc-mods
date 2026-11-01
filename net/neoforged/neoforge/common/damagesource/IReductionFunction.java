package net.neoforged.neoforge.common.damagesource;

@FunctionalInterface
public interface IReductionFunction {
   float modify(DamageContainer var1, float var2);
}
