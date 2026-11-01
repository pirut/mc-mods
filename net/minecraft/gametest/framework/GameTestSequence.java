package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

public class GameTestSequence {
   final GameTestInfo parent;
   private final List<GameTestEvent> events = Lists.newArrayList();
   private long lastTick;

   public GameTestSequence(GameTestInfo p_177542_) {
      this.parent = p_177542_;
      this.lastTick = p_177542_.getTick();
   }

   public GameTestSequence thenWaitUntil(Runnable p_177553_) {
      this.events.add(GameTestEvent.create(p_177553_));
      return this;
   }

   public GameTestSequence thenWaitUntil(long p_177550_, Runnable p_177551_) {
      this.events.add(GameTestEvent.create(p_177550_, p_177551_));
      return this;
   }

   public GameTestSequence thenIdle(int p_177545_) {
      return this.thenExecuteAfter(p_177545_, () -> {});
   }

   public GameTestSequence thenExecute(Runnable p_177563_) {
      this.events.add(GameTestEvent.create(() -> this.executeWithoutFail(p_177563_)));
      return this;
   }

   public GameTestSequence thenExecuteAfter(int p_177547_, Runnable p_177548_) {
      this.events.add(GameTestEvent.create(() -> {
         if (this.parent.getTick() < this.lastTick + p_177547_) {
            throw new GameTestAssertException("Test timed out before sequence completed");
         } else {
            this.executeWithoutFail(p_177548_);
         }
      }));
      return this;
   }

   public GameTestSequence thenExecuteFor(int p_177560_, Runnable p_177561_) {
      this.events.add(GameTestEvent.create(() -> {
         if (this.parent.getTick() < this.lastTick + p_177560_) {
            this.executeWithoutFail(p_177561_);
            throw new GameTestAssertException("Test timed out before sequence completed");
         }
      }));
      return this;
   }

   public void thenSucceed() {
      this.events.add(GameTestEvent.create(this.parent::succeed));
   }

   public void thenFail(Supplier<Exception> p_177555_) {
      this.events.add(GameTestEvent.create(() -> this.parent.fail(p_177555_.get())));
   }

   public GameTestSequence.Condition thenTrigger() {
      GameTestSequence.Condition $$0 = new GameTestSequence.Condition();
      this.events.add(GameTestEvent.create(() -> $$0.trigger(this.parent.getTick())));
      return $$0;
   }

   public void tickAndContinue(long p_127778_) {
      try {
         this.tick(p_127778_);
      } catch (GameTestAssertException var4) {
      }
   }

   public void tickAndFailIfNotComplete(long p_127780_) {
      try {
         this.tick(p_127780_);
      } catch (GameTestAssertException var4) {
         this.parent.fail(var4);
      }
   }

   private void executeWithoutFail(Runnable p_177571_) {
      try {
         p_177571_.run();
      } catch (GameTestAssertException var3) {
         this.parent.fail(var3);
      }
   }

   private void tick(long p_127782_) {
      Iterator<GameTestEvent> $$1 = this.events.iterator();

      while ($$1.hasNext()) {
         GameTestEvent $$2 = $$1.next();
         $$2.assertion.run();
         $$1.remove();
         long $$3 = p_127782_ - this.lastTick;
         long $$4 = this.lastTick;
         this.lastTick = p_127782_;
         if ($$2.expectedDelay != null && $$2.expectedDelay != $$3) {
            this.parent
               .fail(new GameTestAssertException("Succeeded in invalid tick: expected " + ($$4 + $$2.expectedDelay) + ", but current tick is " + p_127782_));
            break;
         }
      }
   }

   public class Condition {
      private static final long NOT_TRIGGERED = -1L;
      private long triggerTime = -1L;

      void trigger(long p_177584_) {
         if (this.triggerTime != -1L) {
            throw new IllegalStateException("Condition already triggered at " + this.triggerTime);
         } else {
            this.triggerTime = p_177584_;
         }
      }

      public void assertTriggeredThisTick() {
         long $$0 = GameTestSequence.this.parent.getTick();
         if (this.triggerTime != $$0) {
            if (this.triggerTime == -1L) {
               throw new GameTestAssertException("Condition not triggered (t=" + $$0 + ")");
            } else {
               throw new GameTestAssertException("Condition triggered at " + this.triggerTime + ", (t=" + $$0 + ")");
            }
         }
      }
   }
}
