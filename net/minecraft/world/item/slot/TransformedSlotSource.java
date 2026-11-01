package net.minecraft.world.item.slot;

import com.mojang.datafixers.Products.P1;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;

public abstract class TransformedSlotSource implements SlotSource {
   protected final SlotSource slotSource;

   protected TransformedSlotSource(SlotSource p_460865_) {
      this.slotSource = p_460865_;
   }

   @Override
   public abstract MapCodec<? extends TransformedSlotSource> codec();

   protected static <T extends TransformedSlotSource> P1<Mu<T>, SlotSource> commonFields(Instance<T> p_461201_) {
      return p_461201_.group(SlotSources.CODEC.fieldOf("slot_source").forGetter(p_460789_ -> p_460789_.slotSource));
   }

   protected abstract SlotCollection transform(SlotCollection var1);

   @Override
   public final SlotCollection provide(LootContext p_460834_) {
      return this.transform(this.slotSource.provide(p_460834_));
   }

   @Override
   public void validate(ValidationContext p_461040_) {
      SlotSource.super.validate(p_461040_);
      this.slotSource.validate(p_461040_.forChild(new ProblemReporter.FieldPathElement("slot_source")));
   }
}
