package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.minecraft.report.ReportedEntity;
import com.mojang.datafixers.util.Either;
import java.time.Instant;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.reporting.NameReportScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

@OnlyIn(Dist.CLIENT)
public class NameReport extends Report {
   private final String reportedName;

   NameReport(UUID p_299985_, Instant p_299857_, UUID p_299900_, String p_300040_) {
      super(p_299985_, p_299857_, p_299900_);
      this.reportedName = p_300040_;
   }

   public String getReportedName() {
      return this.reportedName;
   }

   public NameReport copy() {
      NameReport $$0 = new NameReport(this.reportId, this.createdAt, this.reportedProfileId, this.reportedName);
      $$0.comments = this.comments;
      $$0.attested = this.attested;
      return $$0;
   }

   @Override
   public Screen createScreen(Screen p_299843_, ReportingContext p_299844_) {
      return new NameReportScreen(p_299843_, p_299844_, this);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder extends Report.Builder<NameReport> {
      public Builder(NameReport p_299841_, AbuseReportLimits p_299879_) {
         super(p_299841_, p_299879_);
      }

      public Builder(UUID p_299951_, String p_299988_, AbuseReportLimits p_299993_) {
         super(new NameReport(UUID.randomUUID(), Instant.now(), p_299951_, p_299988_), p_299993_);
      }

      @Override
      public boolean hasContent() {
         return StringUtils.isNotEmpty(this.comments());
      }

      @Nullable
      @Override
      public Report.CannotBuildReason checkBuildable() {
         return this.report.comments.length() > this.limits.maxOpinionCommentsLength() ? Report.CannotBuildReason.COMMENT_TOO_LONG : super.checkBuildable();
      }

      @Override
      public Either<Report.Result, Report.CannotBuildReason> build(ReportingContext p_299891_) {
         Report.CannotBuildReason $$1 = this.checkBuildable();
         if ($$1 != null) {
            return Either.right($$1);
         } else {
            ReportedEntity $$2 = new ReportedEntity(this.report.reportedProfileId);
            AbuseReport $$3 = AbuseReport.name(this.report.comments, $$2, this.report.createdAt);
            return Either.left(new Report.Result(this.report.reportId, ReportType.USERNAME, $$3));
         }
      }
   }
}
