package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.minecraft.report.ReportedEntity;
import com.mojang.datafixers.util.Either;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.reporting.SkinReportScreen;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

@OnlyIn(Dist.CLIENT)
public class SkinReport extends Report {
   final Supplier<PlayerSkin> skinGetter;

   SkinReport(UUID p_299908_, Instant p_299882_, UUID p_299986_, Supplier<PlayerSkin> p_299871_) {
      super(p_299908_, p_299882_, p_299986_);
      this.skinGetter = p_299871_;
   }

   public Supplier<PlayerSkin> getSkinGetter() {
      return this.skinGetter;
   }

   public SkinReport copy() {
      SkinReport $$0 = new SkinReport(this.reportId, this.createdAt, this.reportedProfileId, this.skinGetter);
      $$0.comments = this.comments;
      $$0.reason = this.reason;
      $$0.attested = this.attested;
      return $$0;
   }

   @Override
   public Screen createScreen(Screen p_299975_, ReportingContext p_299961_) {
      return new SkinReportScreen(p_299975_, p_299961_, this);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder extends Report.Builder<SkinReport> {
      public Builder(SkinReport p_299828_, AbuseReportLimits p_299881_) {
         super(p_299828_, p_299881_);
      }

      public Builder(UUID p_299890_, Supplier<PlayerSkin> p_299967_, AbuseReportLimits p_300039_) {
         super(new SkinReport(UUID.randomUUID(), Instant.now(), p_299890_, p_299967_), p_300039_);
      }

      @Override
      public boolean hasContent() {
         return StringUtils.isNotEmpty(this.comments()) || this.reason() != null;
      }

      @Nullable
      @Override
      public Report.CannotBuildReason checkBuildable() {
         if (this.report.reason == null) {
            return Report.CannotBuildReason.NO_REASON;
         } else {
            return this.report.comments.length() > this.limits.maxOpinionCommentsLength() ? Report.CannotBuildReason.COMMENT_TOO_LONG : super.checkBuildable();
         }
      }

      @Override
      public Either<Report.Result, Report.CannotBuildReason> build(ReportingContext p_299847_) {
         Report.CannotBuildReason $$1 = this.checkBuildable();
         if ($$1 != null) {
            return Either.right($$1);
         } else {
            String $$2 = Objects.requireNonNull(this.report.reason).backendName();
            ReportedEntity $$3 = new ReportedEntity(this.report.reportedProfileId);
            PlayerSkin $$4 = this.report.skinGetter.get();
            String $$5 = $$4.textureUrl();
            AbuseReport $$6 = AbuseReport.skin(this.report.comments, $$2, $$5, $$3, this.report.createdAt);
            return Either.left(new Report.Result(this.report.reportId, ReportType.SKIN, $$6));
         }
      }
   }
}
