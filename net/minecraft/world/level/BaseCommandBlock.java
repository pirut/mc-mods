package net.minecraft.world.level;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public abstract class BaseCommandBlock {
   private static final Component DEFAULT_NAME = Component.literal("@");
   private static final int NO_LAST_EXECUTION = -1;
   private long lastExecution = -1L;
   private boolean updateLastExecution = true;
   private int successCount;
   private boolean trackOutput = true;
   @Nullable
   Component lastOutput;
   private String command = "";
   @Nullable
   private Component customName;

   public int getSuccessCount() {
      return this.successCount;
   }

   public void setSuccessCount(int p_45411_) {
      this.successCount = p_45411_;
   }

   public Component getLastOutput() {
      return this.lastOutput == null ? CommonComponents.EMPTY : this.lastOutput;
   }

   public void save(ValueOutput p_422186_) {
      p_422186_.putString("Command", this.command);
      p_422186_.putInt("SuccessCount", this.successCount);
      p_422186_.storeNullable("CustomName", ComponentSerialization.CODEC, this.customName);
      p_422186_.putBoolean("TrackOutput", this.trackOutput);
      if (this.trackOutput) {
         p_422186_.storeNullable("LastOutput", ComponentSerialization.CODEC, this.lastOutput);
      }

      p_422186_.putBoolean("UpdateLastExecution", this.updateLastExecution);
      if (this.updateLastExecution && this.lastExecution != -1L) {
         p_422186_.putLong("LastExecution", this.lastExecution);
      }
   }

   public void load(ValueInput p_421770_) {
      this.command = p_421770_.getStringOr("Command", "");
      this.successCount = p_421770_.getIntOr("SuccessCount", 0);
      this.setCustomName(BlockEntity.parseCustomNameSafe(p_421770_, "CustomName"));
      this.trackOutput = p_421770_.getBooleanOr("TrackOutput", true);
      if (this.trackOutput) {
         this.lastOutput = BlockEntity.parseCustomNameSafe(p_421770_, "LastOutput");
      } else {
         this.lastOutput = null;
      }

      this.updateLastExecution = p_421770_.getBooleanOr("UpdateLastExecution", true);
      if (this.updateLastExecution) {
         this.lastExecution = p_421770_.getLongOr("LastExecution", -1L);
      } else {
         this.lastExecution = -1L;
      }
   }

   public void setCommand(String p_45420_) {
      this.command = p_45420_;
      this.successCount = 0;
   }

   public String getCommand() {
      return this.command;
   }

   public boolean performCommand(ServerLevel p_455909_) {
      if (p_455909_.getGameTime() == this.lastExecution) {
         return false;
      } else if ("Searge".equalsIgnoreCase(this.command)) {
         this.lastOutput = Component.literal("#itzlipofutzli");
         this.successCount = 1;
         return true;
      } else {
         this.successCount = 0;
         if (p_455909_.isCommandBlockEnabled() && !StringUtil.isNullOrEmpty(this.command)) {
            try {
               this.lastOutput = null;

               try (BaseCommandBlock.CloseableCommandBlockSource basecommandblock$closeablecommandblocksource = this.createSource(p_455909_)) {
                  CommandSource commandsource = Objects.requireNonNullElse(basecommandblock$closeablecommandblocksource, CommandSource.NULL);
                  CommandSourceStack commandsourcestack = this.createCommandSourceStack(p_455909_, commandsource).withCallback((p_45418_, p_45419_) -> {
                     if (p_45418_) {
                        this.successCount++;
                     }
                  });
                  p_455909_.getServer().getCommands().performPrefixedCommand(commandsourcestack, this.command);
               }
            } catch (Throwable var7) {
               CrashReport crashreport = CrashReport.forThrowable(var7, "Executing command block");
               CrashReportCategory crashreportcategory = crashreport.addCategory("Command to be executed");
               crashreportcategory.setDetail("Command", this::getCommand);
               crashreportcategory.setDetail("Name", () -> this.getName().getString());
               throw new ReportedException(crashreport);
            }
         }

         if (this.updateLastExecution) {
            this.lastExecution = p_455909_.getGameTime();
         } else {
            this.lastExecution = -1L;
         }

         return true;
      }
   }

   private BaseCommandBlock.CloseableCommandBlockSource createSource(ServerLevel p_455401_) {
      return this.trackOutput ? new BaseCommandBlock.CloseableCommandBlockSource(p_455401_) : null;
   }

   public Component getName() {
      return this.customName != null ? this.customName : DEFAULT_NAME;
   }

   @Nullable
   public Component getCustomName() {
      return this.customName;
   }

   public void setCustomName(@Nullable Component p_331531_) {
      this.customName = p_331531_;
   }

   public abstract void onUpdated(ServerLevel var1);

   public void setLastOutput(@Nullable Component p_45434_) {
      this.lastOutput = p_45434_;
   }

   public void setTrackOutput(boolean p_45429_) {
      this.trackOutput = p_45429_;
   }

   public boolean isTrackOutput() {
      return this.trackOutput;
   }

   public abstract CommandSourceStack createCommandSourceStack(ServerLevel var1, CommandSource var2);

   public abstract boolean isValid();

   protected class CloseableCommandBlockSource implements CommandSource, AutoCloseable {
      private final ServerLevel level;
      private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT);
      private boolean closed;

      protected CloseableCommandBlockSource(ServerLevel p_455149_) {
         this.level = p_455149_;
      }

      @Override
      public boolean acceptsSuccess() {
         return !this.closed && this.level.getGameRules().get(GameRules.SEND_COMMAND_FEEDBACK);
      }

      @Override
      public boolean acceptsFailure() {
         return !this.closed;
      }

      @Override
      public boolean shouldInformAdmins() {
         return !this.closed && this.level.getGameRules().get(GameRules.COMMAND_BLOCK_OUTPUT);
      }

      @Override
      public void sendSystemMessage(Component p_443616_) {
         if (!this.closed) {
            BaseCommandBlock.this.lastOutput = Component.literal("[" + TIME_FORMAT.format(ZonedDateTime.now()) + "] ").append(p_443616_);
            BaseCommandBlock.this.onUpdated(this.level);
         }
      }

      @Override
      public void close() throws Exception {
         this.closed = true;
      }
   }
}
