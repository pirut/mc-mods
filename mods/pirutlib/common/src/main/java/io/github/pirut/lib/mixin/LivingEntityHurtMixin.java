package io.github.pirut.lib.mixin;

import io.github.pirut.lib.ServerEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fires {@link ServerEvents#onIgnoreDamage} and {@link ServerEvents#onDamage} for every hit, on both loaders. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHurtMixin {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void pirutlib$ignore(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide && ServerEvents.Fire.ignore(self, source, amount)) cir.setReturnValue(false);
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float pirutlib$damage(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        return self.level().isClientSide ? amount : ServerEvents.Fire.damage(self, source, amount);
    }
}
