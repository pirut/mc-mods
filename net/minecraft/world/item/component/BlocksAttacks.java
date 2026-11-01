package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;

public record BlocksAttacks(
   float blockDelaySeconds,
   float disableCooldownScale,
   List<BlocksAttacks.DamageReduction> damageReductions,
   BlocksAttacks.ItemDamageFunction itemDamage,
   Optional<TagKey<DamageType>> bypassedBy,
   Optional<Holder<SoundEvent>> blockSound,
   Optional<Holder<SoundEvent>> disableSound
) {
   public static final Codec<BlocksAttacks> CODEC = RecordCodecBuilder.create(
      p_400940_ -> p_400940_.group(
            ExtraCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("block_delay_seconds", 0.0F).forGetter(BlocksAttacks::blockDelaySeconds),
            ExtraCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("disable_cooldown_scale", 1.0F).forGetter(BlocksAttacks::disableCooldownScale),
            BlocksAttacks.DamageReduction.CODEC
               .listOf()
               .optionalFieldOf("damage_reductions", List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)))
               .forGetter(BlocksAttacks::damageReductions),
            BlocksAttacks.ItemDamageFunction.CODEC
               .optionalFieldOf("item_damage", BlocksAttacks.ItemDamageFunction.DEFAULT)
               .forGetter(BlocksAttacks::itemDamage),
            TagKey.hashedCodec(Registries.DAMAGE_TYPE).optionalFieldOf("bypassed_by").forGetter(BlocksAttacks::bypassedBy),
            SoundEvent.CODEC.optionalFieldOf("block_sound").forGetter(BlocksAttacks::blockSound),
            SoundEvent.CODEC.optionalFieldOf("disabled_sound").forGetter(BlocksAttacks::disableSound)
         )
         .apply(p_400940_, BlocksAttacks::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, BlocksAttacks> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.FLOAT,
      BlocksAttacks::blockDelaySeconds,
      ByteBufCodecs.FLOAT,
      BlocksAttacks::disableCooldownScale,
      BlocksAttacks.DamageReduction.STREAM_CODEC.apply(ByteBufCodecs.list()),
      BlocksAttacks::damageReductions,
      BlocksAttacks.ItemDamageFunction.STREAM_CODEC,
      BlocksAttacks::itemDamage,
      TagKey.streamCodec(Registries.DAMAGE_TYPE).apply(ByteBufCodecs::optional),
      BlocksAttacks::bypassedBy,
      SoundEvent.STREAM_CODEC.apply(ByteBufCodecs::optional),
      BlocksAttacks::blockSound,
      SoundEvent.STREAM_CODEC.apply(ByteBufCodecs::optional),
      BlocksAttacks::disableSound,
      BlocksAttacks::new
   );

   public void onBlocked(ServerLevel p_400270_, LivingEntity p_399896_) {
      this.blockSound
         .ifPresent(
            p_427171_ -> p_400270_.playSound(
               null,
               p_399896_.getX(),
               p_399896_.getY(),
               p_399896_.getZ(),
               (Holder<SoundEvent>)p_427171_,
               p_399896_.getSoundSource(),
               1.0F,
               0.8F + p_400270_.random.nextFloat() * 0.4F
            )
         );
   }

   public void disable(ServerLevel p_400187_, LivingEntity p_399747_, float p_399988_, ItemStack p_399719_) {
      int i = this.disableBlockingForTicks(p_399988_);
      if (i > 0) {
         if (p_399747_ instanceof Player player) {
            player.getCooldowns().addCooldown(p_399719_, i);
         }

         p_399747_.stopUsingItem();
         this.disableSound
            .ifPresent(
               p_427168_ -> p_400187_.playSound(
                  null,
                  p_399747_.getX(),
                  p_399747_.getY(),
                  p_399747_.getZ(),
                  (Holder<SoundEvent>)p_427168_,
                  p_399747_.getSoundSource(),
                  0.8F,
                  0.8F + p_400187_.random.nextFloat() * 0.4F
               )
            );
      }
   }

   public void hurtBlockingItem(Level p_400285_, ItemStack p_399533_, LivingEntity p_400149_, InteractionHand p_400151_, float p_399754_) {
      this.hurtBlockingItem(p_400285_, p_399533_, p_400149_, p_400151_, p_399754_, -1);
   }

   public void hurtBlockingItem(Level p_400285_, ItemStack p_399533_, LivingEntity p_400149_, InteractionHand p_400151_, float p_399754_, int fixedDamage) {
      if (p_400149_ instanceof Player player) {
         if (!p_400285_.isClientSide()) {
            player.awardStat(Stats.ITEM_USED.get(p_399533_.getItem()));
         }

         int i = fixedDamage < 0 ? this.itemDamage.apply(p_399754_) : fixedDamage;
         if (i > 0 && p_400285_ instanceof ServerLevel serverLevel) {
            p_399533_.hurtAndBreak(i, serverLevel, p_400149_, item -> {
               p_400149_.onEquippedItemBroken(item, p_400151_.asEquipmentSlot());
               EventHooks.onPlayerDestroyItem(player, player.getUseItem(), p_400151_);
               player.stopUsingItem();
            });
         }
      }
   }

   private int disableBlockingForTicks(float p_399745_) {
      float f = p_399745_ * this.disableCooldownScale;
      return f > 0.0F ? Math.round(f * 20.0F) : 0;
   }

   public int blockDelayTicks() {
      return Math.round(this.blockDelaySeconds * 20.0F);
   }

   public float resolveBlockedDamage(DamageSource p_399636_, float p_399898_, double p_401279_) {
      float f = 0.0F;

      for (BlocksAttacks.DamageReduction blocksattacks$damagereduction : this.damageReductions) {
         f += blocksattacks$damagereduction.resolve(p_399636_, p_399898_, p_401279_);
      }

      return Mth.clamp(f, 0.0F, p_399898_);
   }

   public record DamageReduction(float horizontalBlockingAngle, Optional<HolderSet<DamageType>> type, float base, float factor) {
      public static final Codec<BlocksAttacks.DamageReduction> CODEC = RecordCodecBuilder.create(
         p_400941_ -> p_400941_.group(
               ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("horizontal_blocking_angle", 90.0F).forGetter(BlocksAttacks.DamageReduction::horizontalBlockingAngle),
               RegistryCodecs.homogeneousList(Registries.DAMAGE_TYPE).optionalFieldOf("type").forGetter(BlocksAttacks.DamageReduction::type),
               Codec.FLOAT.fieldOf("base").forGetter(BlocksAttacks.DamageReduction::base),
               Codec.FLOAT.fieldOf("factor").forGetter(BlocksAttacks.DamageReduction::factor)
            )
            .apply(p_400941_, BlocksAttacks.DamageReduction::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, BlocksAttacks.DamageReduction> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.FLOAT,
         BlocksAttacks.DamageReduction::horizontalBlockingAngle,
         ByteBufCodecs.holderSet(Registries.DAMAGE_TYPE).apply(ByteBufCodecs::optional),
         BlocksAttacks.DamageReduction::type,
         ByteBufCodecs.FLOAT,
         BlocksAttacks.DamageReduction::base,
         ByteBufCodecs.FLOAT,
         BlocksAttacks.DamageReduction::factor,
         BlocksAttacks.DamageReduction::new
      );

      public float resolve(DamageSource p_399864_, float p_400229_, double p_401381_) {
         if (p_401381_ > (float) (Math.PI / 180.0) * this.horizontalBlockingAngle) {
            return 0.0F;
         } else {
            return this.type.isPresent() && !this.type.get().contains(p_399864_.typeHolder())
               ? 0.0F
               : Mth.clamp(this.base + this.factor * p_400229_, 0.0F, p_400229_);
         }
      }
   }

   public record ItemDamageFunction(float threshold, float base, float factor) {
      public static final Codec<BlocksAttacks.ItemDamageFunction> CODEC = RecordCodecBuilder.create(
         p_399571_ -> p_399571_.group(
               ExtraCodecs.NON_NEGATIVE_FLOAT.fieldOf("threshold").forGetter(BlocksAttacks.ItemDamageFunction::threshold),
               Codec.FLOAT.fieldOf("base").forGetter(BlocksAttacks.ItemDamageFunction::base),
               Codec.FLOAT.fieldOf("factor").forGetter(BlocksAttacks.ItemDamageFunction::factor)
            )
            .apply(p_399571_, BlocksAttacks.ItemDamageFunction::new)
      );
      public static final StreamCodec<ByteBuf, BlocksAttacks.ItemDamageFunction> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.FLOAT,
         BlocksAttacks.ItemDamageFunction::threshold,
         ByteBufCodecs.FLOAT,
         BlocksAttacks.ItemDamageFunction::base,
         ByteBufCodecs.FLOAT,
         BlocksAttacks.ItemDamageFunction::factor,
         BlocksAttacks.ItemDamageFunction::new
      );
      public static final BlocksAttacks.ItemDamageFunction DEFAULT = new BlocksAttacks.ItemDamageFunction(1.0F, 0.0F, 1.0F);

      public int apply(float p_399581_) {
         return p_399581_ < this.threshold ? 0 : Mth.floor(this.base + this.factor * p_399581_);
      }
   }
}
