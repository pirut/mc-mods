package net.minecraft.network.chat.contents;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public record EntityDataSource(String selectorPattern, @Nullable EntitySelector compiledSelector) implements DataSource {
   public static final MapCodec<EntityDataSource> SUB_CODEC = RecordCodecBuilder.mapCodec(
      p_304696_ -> p_304696_.group(Codec.STRING.fieldOf("entity").forGetter(EntityDataSource::selectorPattern)).apply(p_304696_, EntityDataSource::new)
   );
   public static final DataSource.Type<EntityDataSource> TYPE = new DataSource.Type<>(SUB_CODEC, "entity");

   public EntityDataSource(String p_237330_) {
      this(p_237330_, compileSelector(p_237330_));
   }

   @Nullable
   private static EntitySelector compileSelector(String p_237336_) {
      try {
         EntitySelectorParser $$1 = new EntitySelectorParser(new StringReader(p_237336_), true);
         return $$1.parse();
      } catch (CommandSyntaxException var2) {
         return null;
      }
   }

   @Override
   public Stream<CompoundTag> getData(CommandSourceStack p_237341_) throws CommandSyntaxException {
      if (this.compiledSelector != null) {
         List<? extends Entity> $$1 = this.compiledSelector.findEntities(p_237341_);
         return $$1.stream().map(NbtPredicate::getEntityTagToCompare);
      } else {
         return Stream.empty();
      }
   }

   @Override
   public DataSource.Type<?> type() {
      return TYPE;
   }

   @Override
   public String toString() {
      return "entity=" + this.selectorPattern;
   }

   @Override
   public boolean equals(Object p_237339_) {
      return this == p_237339_ ? true : p_237339_ instanceof EntityDataSource $$1 && this.selectorPattern.equals($$1.selectorPattern);
   }

   @Override
   public int hashCode() {
      return this.selectorPattern.hashCode();
   }
}
