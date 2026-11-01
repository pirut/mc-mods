package net.minecraft.commands.arguments.blocks;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockPredicateArgument implements ArgumentType<BlockPredicateArgument.Result> {
   private static final Collection<String> EXAMPLES = Arrays.asList("stone", "minecraft:stone", "stone[foo=bar]", "#stone", "#stone[foo=bar]{baz=nbt}");
   private final HolderLookup<Block> blocks;

   public BlockPredicateArgument(CommandBuildContext p_234626_) {
      this.blocks = p_234626_.lookupOrThrow(Registries.BLOCK);
   }

   public static BlockPredicateArgument blockPredicate(CommandBuildContext p_234628_) {
      return new BlockPredicateArgument(p_234628_);
   }

   public BlockPredicateArgument.Result parse(StringReader p_115572_) throws CommandSyntaxException {
      return parse(this.blocks, p_115572_);
   }

   public static BlockPredicateArgument.Result parse(HolderLookup<Block> p_234634_, StringReader p_234635_) throws CommandSyntaxException {
      return (BlockPredicateArgument.Result)BlockStateParser.parseForTesting(p_234634_, p_234635_, true)
         .map(
            p_234630_ -> new BlockPredicateArgument.BlockPredicate(p_234630_.blockState(), p_234630_.properties().keySet(), p_234630_.nbt()),
            p_234632_ -> new BlockPredicateArgument.TagPredicate(p_234632_.tag(), p_234632_.vagueProperties(), p_234632_.nbt())
         );
   }

   public static Predicate<BlockInWorld> getBlockPredicate(CommandContext<CommandSourceStack> p_115574_, String p_115575_) throws CommandSyntaxException {
      return (Predicate<BlockInWorld>)p_115574_.getArgument(p_115575_, BlockPredicateArgument.Result.class);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_115587_, SuggestionsBuilder p_115588_) {
      return BlockStateParser.fillSuggestions(this.blocks, p_115588_, true, true);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   static class BlockPredicate implements BlockPredicateArgument.Result {
      private final BlockState state;
      private final Set<Property<?>> properties;
      @Nullable
      private final CompoundTag nbt;

      public BlockPredicate(BlockState p_115595_, Set<Property<?>> p_115596_, @Nullable CompoundTag p_115597_) {
         this.state = p_115595_;
         this.properties = p_115596_;
         this.nbt = p_115597_;
      }

      public boolean test(BlockInWorld p_115599_) {
         BlockState $$1 = p_115599_.getState();
         if (!$$1.is(this.state.getBlock())) {
            return false;
         } else {
            for (Property<?> $$2 : this.properties) {
               if ($$1.getValue($$2) != this.state.getValue($$2)) {
                  return false;
               }
            }

            if (this.nbt == null) {
               return true;
            } else {
               BlockEntity $$3 = p_115599_.getEntity();
               return $$3 != null && NbtUtils.compareNbt(this.nbt, $$3.saveWithFullMetadata(p_115599_.getLevel().registryAccess()), true);
            }
         }
      }

      @Override
      public boolean requiresNbt() {
         return this.nbt != null;
      }
   }

   public interface Result extends Predicate<BlockInWorld> {
      boolean requiresNbt();
   }

   static class TagPredicate implements BlockPredicateArgument.Result {
      private final HolderSet<Block> tag;
      @Nullable
      private final CompoundTag nbt;
      private final Map<String, String> vagueProperties;

      TagPredicate(HolderSet<Block> p_234637_, Map<String, String> p_234638_, @Nullable CompoundTag p_234639_) {
         this.tag = p_234637_;
         this.vagueProperties = p_234638_;
         this.nbt = p_234639_;
      }

      public boolean test(BlockInWorld p_115617_) {
         BlockState $$1 = p_115617_.getState();
         if (!$$1.is(this.tag)) {
            return false;
         } else {
            for (Entry<String, String> $$2 : this.vagueProperties.entrySet()) {
               Property<?> $$3 = $$1.getBlock().getStateDefinition().getProperty($$2.getKey());
               if ($$3 == null) {
                  return false;
               }

               Comparable<?> $$4 = (Comparable<?>)$$3.getValue($$2.getValue()).orElse(null);
               if ($$4 == null) {
                  return false;
               }

               if ($$1.getValue($$3) != $$4) {
                  return false;
               }
            }

            if (this.nbt == null) {
               return true;
            } else {
               BlockEntity $$5 = p_115617_.getEntity();
               return $$5 != null && NbtUtils.compareNbt(this.nbt, $$5.saveWithFullMetadata(p_115617_.getLevel().registryAccess()), true);
            }
         }
      }

      @Override
      public boolean requiresNbt() {
         return this.nbt != null;
      }
   }
}
