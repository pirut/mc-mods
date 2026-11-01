package net.minecraft.gametest.framework;

import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;

public class TestFinder implements TestInstanceFinder, TestPosFinder {
   static final TestInstanceFinder NO_FUNCTIONS = Stream::empty;
   static final TestPosFinder NO_STRUCTURES = Stream::empty;
   private final TestInstanceFinder testInstanceFinder;
   private final TestPosFinder testPosFinder;
   private final CommandSourceStack source;

   @Override
   public Stream<BlockPos> findTestPos() {
      return this.testPosFinder.findTestPos();
   }

   public static TestFinder.Builder builder() {
      return new TestFinder.Builder();
   }

   TestFinder(CommandSourceStack p_320004_, TestInstanceFinder p_397279_, TestPosFinder p_397048_) {
      this.source = p_320004_;
      this.testInstanceFinder = p_397279_;
      this.testPosFinder = p_397048_;
   }

   public CommandSourceStack source() {
      return this.source;
   }

   @Override
   public Stream<Holder.Reference<GameTestInstance>> findTests() {
      return this.testInstanceFinder.findTests();
   }

   public static class Builder {
      private final UnaryOperator<Supplier<Stream<Holder.Reference<GameTestInstance>>>> testFinderWrapper;
      private final UnaryOperator<Supplier<Stream<BlockPos>>> structureBlockPosFinderWrapper;

      public Builder() {
         this.testFinderWrapper = p_329857_ -> p_329857_;
         this.structureBlockPosFinderWrapper = p_329858_ -> p_329858_;
      }

      private Builder(UnaryOperator<Supplier<Stream<Holder.Reference<GameTestInstance>>>> p_397152_, UnaryOperator<Supplier<Stream<BlockPos>>> p_398014_) {
         this.testFinderWrapper = p_397152_;
         this.structureBlockPosFinderWrapper = p_398014_;
      }

      public TestFinder.Builder createMultipleCopies(int p_330482_) {
         return new TestFinder.Builder(createCopies(p_330482_), createCopies(p_330482_));
      }

      private static <Q> UnaryOperator<Supplier<Stream<Q>>> createCopies(int p_330804_) {
         return p_454389_ -> {
            List<Q> list = new LinkedList<>();
            List<Q> list1 = ((Stream)p_454389_.get()).toList();

            for (int i = 0; i < p_330804_; i++) {
               list.addAll(list1);
            }

            return list::stream;
         };
      }

      private TestFinder build(CommandSourceStack p_330622_, TestInstanceFinder p_397791_, TestPosFinder p_397288_) {
         return new TestFinder(
            p_330622_, this.testFinderWrapper.apply(p_397791_::findTests)::get, this.structureBlockPosFinderWrapper.apply(p_397288_::findTestPos)::get
         );
      }

      public TestFinder radius(CommandContext<CommandSourceStack> p_320307_, int p_320811_) {
         CommandSourceStack commandsourcestack = (CommandSourceStack)p_320307_.getSource();
         BlockPos blockpos = BlockPos.containing(commandsourcestack.getPosition());
         return this.build(commandsourcestack, TestFinder.NO_FUNCTIONS, () -> StructureUtils.findTestBlocks(blockpos, p_320811_, commandsourcestack.getLevel()));
      }

      public TestFinder nearest(CommandContext<CommandSourceStack> p_320944_) {
         CommandSourceStack commandsourcestack = (CommandSourceStack)p_320944_.getSource();
         BlockPos blockpos = BlockPos.containing(commandsourcestack.getPosition());
         return this.build(
            commandsourcestack, TestFinder.NO_FUNCTIONS, () -> StructureUtils.findNearestTest(blockpos, 15, commandsourcestack.getLevel()).stream()
         );
      }

      public TestFinder allNearby(CommandContext<CommandSourceStack> p_320216_) {
         CommandSourceStack commandsourcestack = (CommandSourceStack)p_320216_.getSource();
         BlockPos blockpos = BlockPos.containing(commandsourcestack.getPosition());
         return this.build(commandsourcestack, TestFinder.NO_FUNCTIONS, () -> StructureUtils.findTestBlocks(blockpos, 250, commandsourcestack.getLevel()));
      }

      public TestFinder lookedAt(CommandContext<CommandSourceStack> p_320178_) {
         CommandSourceStack commandsourcestack = (CommandSourceStack)p_320178_.getSource();
         return this.build(
            commandsourcestack,
            TestFinder.NO_FUNCTIONS,
            () -> StructureUtils.lookedAtTestPos(
               BlockPos.containing(commandsourcestack.getPosition()), commandsourcestack.getPlayer().getCamera(), commandsourcestack.getLevel()
            )
         );
      }

      public TestFinder failedTests(CommandContext<CommandSourceStack> p_320960_, boolean p_320352_) {
         return this.build(
            (CommandSourceStack)p_320960_.getSource(),
            () -> FailedTestTracker.getLastFailedTests().filter(p_396483_ -> !p_320352_ || p_396483_.value().required()),
            TestFinder.NO_STRUCTURES
         );
      }

      public TestFinder byResourceSelection(CommandContext<CommandSourceStack> p_397078_, Collection<Holder.Reference<GameTestInstance>> p_397871_) {
         return this.build((CommandSourceStack)p_397078_.getSource(), p_397871_::stream, TestFinder.NO_STRUCTURES);
      }

      public TestFinder failedTests(CommandContext<CommandSourceStack> p_320220_) {
         return this.failedTests(p_320220_, false);
      }
   }
}
