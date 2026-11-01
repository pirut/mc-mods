package net.minecraft.gametest.framework;

import com.google.common.base.MoreObjects;
import java.util.Arrays;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.apache.commons.lang3.exception.ExceptionUtils;

class ReportGameListener implements GameTestListener {
   private int attempts = 0;
   private int successes = 0;

   public ReportGameListener() {
   }

   @Override
   public void testStructureLoaded(GameTestInfo p_177718_) {
      spawnBeacon(p_177718_, Blocks.LIGHT_GRAY_STAINED_GLASS);
      this.attempts++;
   }

   private void handleRetry(GameTestInfo p_319864_, GameTestRunner p_319782_, boolean p_320742_) {
      RetryOptions $$3 = p_319864_.retryOptions();
      String $$4 = String.format("[Run: %4d, Ok: %4d, Fail: %4d", this.attempts, this.successes, this.attempts - this.successes);
      if (!$$3.unlimitedTries()) {
         $$4 = $$4 + String.format(", Left: %4d", $$3.numberOfTries() - this.attempts);
      }

      $$4 = $$4 + "]";
      String $$5 = p_319864_.getTestName() + " " + (p_320742_ ? "passed" : "failed") + "! " + p_319864_.getRunTime() + "ms";
      String $$6 = String.format("%-53s%s", $$4, $$5);
      if (p_320742_) {
         reportPassed(p_319864_, $$6);
      } else {
         say(p_319864_.getLevel(), ChatFormatting.RED, $$6);
      }

      if ($$3.hasTriesLeft(this.attempts, this.successes)) {
         p_319782_.rerunTest(p_319864_);
      }
   }

   @Override
   public void testPassed(GameTestInfo p_177729_, GameTestRunner p_320359_) {
      this.successes++;
      if (p_177729_.retryOptions().hasRetries()) {
         this.handleRetry(p_177729_, p_320359_, true);
      } else if (!p_177729_.isFlaky()) {
         reportPassed(p_177729_, p_177729_.getTestName() + " passed! (" + p_177729_.getRunTime() + "ms)");
      } else {
         if (this.successes >= p_177729_.requiredSuccesses()) {
            reportPassed(p_177729_, p_177729_ + " passed " + this.successes + " times of " + this.attempts + " attempts.");
         } else {
            say(
               p_177729_.getLevel(),
               ChatFormatting.GREEN,
               "Flaky test " + p_177729_ + " succeeded, attempt: " + this.attempts + " successes: " + this.successes
            );
            p_320359_.rerunTest(p_177729_);
         }
      }
   }

   @Override
   public void testFailed(GameTestInfo p_177737_, GameTestRunner p_320181_) {
      if (!p_177737_.isFlaky()) {
         reportFailure(p_177737_, p_177737_.getError());
         if (p_177737_.retryOptions().hasRetries()) {
            this.handleRetry(p_177737_, p_320181_, false);
         }
      } else {
         TestFunction $$2 = p_177737_.getTestFunction();
         String $$3 = "Flaky test " + p_177737_ + " failed, attempt: " + this.attempts + "/" + $$2.maxAttempts();
         if ($$2.requiredSuccesses() > 1) {
            $$3 = $$3 + ", successes: " + this.successes + " (" + $$2.requiredSuccesses() + " required)";
         }

         say(p_177737_.getLevel(), ChatFormatting.YELLOW, $$3);
         if (p_177737_.maxAttempts() - this.attempts + this.successes >= p_177737_.requiredSuccesses()) {
            p_320181_.rerunTest(p_177737_);
         } else {
            reportFailure(p_177737_, new ExhaustedAttemptsException(this.attempts, this.successes, p_177737_));
         }
      }
   }

   @Override
   public void testAddedForRerun(GameTestInfo p_320478_, GameTestInfo p_320907_, GameTestRunner p_320607_) {
      p_320907_.addListener(this);
   }

   public static void reportPassed(GameTestInfo p_177723_, String p_177724_) {
      updateBeaconGlass(p_177723_, Blocks.LIME_STAINED_GLASS);
      visualizePassedTest(p_177723_, p_177724_);
   }

   private static void visualizePassedTest(GameTestInfo p_177731_, String p_177732_) {
      say(p_177731_.getLevel(), ChatFormatting.GREEN, p_177732_);
      GlobalTestReporter.onTestSuccess(p_177731_);
   }

   protected static void reportFailure(GameTestInfo p_177726_, Throwable p_177727_) {
      updateBeaconGlass(p_177726_, p_177726_.isRequired() ? Blocks.RED_STAINED_GLASS : Blocks.ORANGE_STAINED_GLASS);
      spawnLectern(p_177726_, Util.describeError(p_177727_));
      visualizeFailedTest(p_177726_, p_177727_);
   }

   protected static void visualizeFailedTest(GameTestInfo p_177734_, Throwable p_177735_) {
      String $$2 = p_177735_.getMessage() + (p_177735_.getCause() == null ? "" : " cause: " + Util.describeError(p_177735_.getCause()));
      String $$3 = (p_177734_.isRequired() ? "" : "(optional) ") + p_177734_.getTestName() + " failed! " + $$2;
      say(p_177734_.getLevel(), p_177734_.isRequired() ? ChatFormatting.RED : ChatFormatting.YELLOW, $$3);
      Throwable $$4 = (Throwable)MoreObjects.firstNonNull(ExceptionUtils.getRootCause(p_177735_), p_177735_);
      if ($$4 instanceof GameTestAssertPosException $$5) {
         showRedBox(p_177734_.getLevel(), $$5.getAbsolutePos(), $$5.getMessageToShowAtBlock());
      }

      GlobalTestReporter.onTestFailed(p_177734_);
   }

   protected static void spawnBeacon(GameTestInfo p_177720_, Block p_177721_) {
      ServerLevel $$2 = p_177720_.getLevel();
      BlockPos $$3 = getBeaconPos(p_177720_);
      $$2.setBlockAndUpdate($$3, Blocks.BEACON.defaultBlockState().rotate(p_177720_.getRotation()));
      updateBeaconGlass(p_177720_, p_177721_);

      for (int $$4 = -1; $$4 <= 1; $$4++) {
         for (int $$5 = -1; $$5 <= 1; $$5++) {
            BlockPos $$6 = $$3.offset($$4, -1, $$5);
            $$2.setBlockAndUpdate($$6, Blocks.IRON_BLOCK.defaultBlockState());
         }
      }
   }

   private static BlockPos getBeaconPos(GameTestInfo p_352434_) {
      BlockPos $$1 = p_352434_.getStructureBlockPos();
      BlockPos $$2 = new BlockPos(-1, -2, -1);
      return StructureTemplate.transform($$1.offset($$2), Mirror.NONE, p_352434_.getRotation(), $$1);
   }

   private static void updateBeaconGlass(GameTestInfo p_352228_, Block p_352195_) {
      ServerLevel $$2 = p_352228_.getLevel();
      BlockPos $$3 = getBeaconPos(p_352228_);
      if ($$2.getBlockState($$3).is(Blocks.BEACON)) {
         BlockPos $$4 = $$3.offset(0, 1, 0);
         $$2.setBlockAndUpdate($$4, p_352195_.defaultBlockState());
      }
   }

   private static void spawnLectern(GameTestInfo p_177739_, String p_177740_) {
      ServerLevel $$2 = p_177739_.getLevel();
      BlockPos $$3 = p_177739_.getStructureBlockPos();
      BlockPos $$4 = new BlockPos(-1, 0, -1);
      BlockPos $$5 = StructureTemplate.transform($$3.offset($$4), Mirror.NONE, p_177739_.getRotation(), $$3);
      $$2.setBlockAndUpdate($$5, Blocks.LECTERN.defaultBlockState().rotate(p_177739_.getRotation()));
      BlockState $$6 = $$2.getBlockState($$5);
      ItemStack $$7 = createBook(p_177739_.getTestName(), p_177739_.isRequired(), p_177740_);
      LecternBlock.tryPlaceBook(null, $$2, $$5, $$6, $$7);
   }

   private static ItemStack createBook(String p_177711_, boolean p_177712_, String p_177713_) {
      StringBuffer $$3 = new StringBuffer();
      Arrays.stream(p_177711_.split("\\.")).forEach(p_177716_ -> $$3.append(p_177716_).append('\n'));
      if (!p_177712_) {
         $$3.append("(optional)\n");
      }

      $$3.append("-------------------\n");
      ItemStack $$4 = new ItemStack(Items.WRITABLE_BOOK);
      $$4.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(List.of(Filterable.passThrough($$3 + p_177713_))));
      return $$4;
   }

   protected static void say(ServerLevel p_177701_, ChatFormatting p_177702_, String p_177703_) {
      p_177701_.getPlayers(p_177705_ -> true).forEach(p_177709_ -> p_177709_.sendSystemMessage(Component.literal(p_177703_).withStyle(p_177702_)));
   }

   private static void showRedBox(ServerLevel p_177697_, BlockPos p_177698_, String p_177699_) {
      DebugPackets.sendGameTestAddMarker(p_177697_, p_177698_, p_177699_, -2130771968, Integer.MAX_VALUE);
   }
}
