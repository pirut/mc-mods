package net.minecraft.gametest.framework;

import com.google.common.base.MoreObjects;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import org.apache.commons.lang3.exception.ExceptionUtils;

class ReportGameListener implements GameTestListener {
   private int attempts = 0;
   private int successes = 0;

   public ReportGameListener() {
   }

   @Override
   public void testStructureLoaded(GameTestInfo p_177718_) {
      this.attempts++;
   }

   private void handleRetry(GameTestInfo p_319864_, GameTestRunner p_319782_, boolean p_320742_) {
      RetryOptions retryoptions = p_319864_.retryOptions();
      String s = String.format(Locale.ROOT, "[Run: %4d, Ok: %4d, Fail: %4d", this.attempts, this.successes, this.attempts - this.successes);
      if (!retryoptions.unlimitedTries()) {
         s = s + String.format(Locale.ROOT, ", Left: %4d", retryoptions.numberOfTries() - this.attempts);
      }

      s = s + "]";
      String s1 = p_319864_.id() + " " + (p_320742_ ? "passed" : "failed") + "! " + p_319864_.getRunTime() + "ms";
      String s2 = String.format(Locale.ROOT, "%-53s%s", s, s1);
      if (p_320742_) {
         reportPassed(p_319864_, s2);
      } else {
         say(p_319864_.getLevel(), ChatFormatting.RED, s2);
      }

      if (retryoptions.hasTriesLeft(this.attempts, this.successes)) {
         p_319782_.rerunTest(p_319864_);
      }
   }

   @Override
   public void testPassed(GameTestInfo p_177729_, GameTestRunner p_320359_) {
      this.successes++;
      if (p_177729_.retryOptions().hasRetries()) {
         this.handleRetry(p_177729_, p_320359_, true);
      } else if (!p_177729_.isFlaky()) {
         reportPassed(p_177729_, p_177729_.id() + " passed! (" + p_177729_.getRunTime() + "ms / " + p_177729_.getTick() + "gameticks)");
      } else if (this.successes >= p_177729_.requiredSuccesses()) {
         reportPassed(p_177729_, p_177729_ + " passed " + this.successes + " times of " + this.attempts + " attempts.");
      } else {
         say(p_177729_.getLevel(), ChatFormatting.GREEN, "Flaky test " + p_177729_ + " succeeded, attempt: " + this.attempts + " successes: " + this.successes);
         p_320359_.rerunTest(p_177729_);
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
         GameTestInstance gametestinstance = p_177737_.getTest();
         String s = "Flaky test " + p_177737_ + " failed, attempt: " + this.attempts + "/" + gametestinstance.maxAttempts();
         if (gametestinstance.requiredSuccesses() > 1) {
            s = s + ", successes: " + this.successes + " (" + gametestinstance.requiredSuccesses() + " required)";
         }

         say(p_177737_.getLevel(), ChatFormatting.YELLOW, s);
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
      getTestInstanceBlockEntity(p_177723_).ifPresent(p_396406_ -> p_396406_.setSuccess());
      visualizePassedTest(p_177723_, p_177724_);
   }

   private static void visualizePassedTest(GameTestInfo p_177731_, String p_177732_) {
      say(p_177731_.getLevel(), ChatFormatting.GREEN, p_177732_);
      GlobalTestReporter.onTestSuccess(p_177731_);
   }

   protected static void reportFailure(GameTestInfo p_177726_, Throwable p_177727_) {
      Component component;
      if (p_177727_ instanceof GameTestAssertException gametestassertexception) {
         component = gametestassertexception.getDescription();
      } else {
         component = Component.literal(Util.describeError(p_177727_));
      }

      getTestInstanceBlockEntity(p_177726_).ifPresent(p_396408_ -> p_396408_.setErrorMessage(component));
      visualizeFailedTest(p_177726_, p_177727_);
   }

   protected static void visualizeFailedTest(GameTestInfo p_177734_, Throwable p_177735_) {
      String s = p_177735_.getMessage() + (p_177735_.getCause() == null ? "" : " cause: " + Util.describeError(p_177735_.getCause()));
      String s1 = (p_177734_.isRequired() ? "" : "(optional) ") + p_177734_.id() + " failed! " + s;
      say(p_177734_.getLevel(), p_177734_.isRequired() ? ChatFormatting.RED : ChatFormatting.YELLOW, s1);
      Throwable throwable = (Throwable)MoreObjects.firstNonNull(ExceptionUtils.getRootCause(p_177735_), p_177735_);
      if (throwable instanceof GameTestAssertPosException gametestassertposexception) {
         p_177734_.getTestInstanceBlockEntity().markError(gametestassertposexception.getAbsolutePos(), gametestassertposexception.getMessageToShowAtBlock());
      }

      GlobalTestReporter.onTestFailed(p_177734_);
   }

   private static Optional<TestInstanceBlockEntity> getTestInstanceBlockEntity(GameTestInfo p_397435_) {
      ServerLevel serverlevel = p_397435_.getLevel();
      Optional<BlockPos> optional = Optional.ofNullable(p_397435_.getTestBlockPos());
      return optional.flatMap(p_396405_ -> serverlevel.getBlockEntity(p_396405_, BlockEntityType.TEST_INSTANCE_BLOCK));
   }

   protected static void say(ServerLevel p_177701_, ChatFormatting p_177702_, String p_177703_) {
      p_177701_.getPlayers(p_177705_ -> true).forEach(p_177709_ -> p_177709_.sendSystemMessage(Component.literal(p_177703_).withStyle(p_177702_)));
   }
}
