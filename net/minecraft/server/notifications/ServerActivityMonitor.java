package net.minecraft.server.notifications;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.util.Util;

public class ServerActivityMonitor {
   private final long minimumMillisBetweenNotifications;
   private final AtomicLong lastNotificationTime = new AtomicLong();
   private final AtomicBoolean serverActivity = new AtomicBoolean(false);
   private final NotificationManager notificationManager;

   public ServerActivityMonitor(NotificationManager p_455252_, int p_455043_) {
      this.notificationManager = p_455252_;
      this.minimumMillisBetweenNotifications = TimeUnit.SECONDS.toMillis(p_455043_);
   }

   public void tick() {
      this.processWithRateLimit();
   }

   public void reportLoginActivity() {
      this.serverActivity.set(true);
      this.processWithRateLimit();
   }

   private void processWithRateLimit() {
      long i = Util.getMillis();
      if (this.serverActivity.get() && i - this.lastNotificationTime.get() >= this.minimumMillisBetweenNotifications) {
         this.notificationManager.serverActivityOccured();
         this.lastNotificationTime.set(Util.getMillis());
      }

      this.serverActivity.set(false);
   }
}
