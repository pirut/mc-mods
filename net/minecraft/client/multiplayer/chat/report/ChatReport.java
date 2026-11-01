package net.minecraft.client.multiplayer.chat.report;

import com.google.common.collect.Lists;
import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.minecraft.report.ReportChatMessage;
import com.mojang.authlib.minecraft.report.ReportEvidence;
import com.mojang.authlib.minecraft.report.ReportedEntity;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.Optionull;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.reporting.ChatReportScreen;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageLink;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

@OnlyIn(Dist.CLIENT)
public class ChatReport extends Report {
   final IntSet reportedMessages = new IntOpenHashSet();

   ChatReport(UUID p_300023_, Instant p_299922_, UUID p_299952_) {
      super(p_300023_, p_299922_, p_299952_);
   }

   public void toggleReported(int p_299907_, AbuseReportLimits p_299997_) {
      if (this.reportedMessages.contains(p_299907_)) {
         this.reportedMessages.remove(p_299907_);
      } else if (this.reportedMessages.size() < p_299997_.maxReportedMessageCount()) {
         this.reportedMessages.add(p_299907_);
      }
   }

   public ChatReport copy() {
      ChatReport $$0 = new ChatReport(this.reportId, this.createdAt, this.reportedProfileId);
      $$0.reportedMessages.addAll(this.reportedMessages);
      $$0.comments = this.comments;
      $$0.reason = this.reason;
      $$0.attested = this.attested;
      return $$0;
   }

   @Override
   public Screen createScreen(Screen p_300022_, ReportingContext p_299859_) {
      return new ChatReportScreen(p_300022_, p_299859_, this);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder extends Report.Builder<ChatReport> {
      public Builder(ChatReport p_299929_, AbuseReportLimits p_299930_) {
         super(p_299929_, p_299930_);
      }

      public Builder(UUID p_299973_, AbuseReportLimits p_299942_) {
         super(new ChatReport(UUID.randomUUID(), Instant.now(), p_299973_), p_299942_);
      }

      public IntSet reportedMessages() {
         return this.report.reportedMessages;
      }

      public void toggleReported(int p_299851_) {
         this.report.toggleReported(p_299851_, this.limits);
      }

      public boolean isReported(int p_299830_) {
         return this.report.reportedMessages.contains(p_299830_);
      }

      @Override
      public boolean hasContent() {
         return StringUtils.isNotEmpty(this.comments()) || !this.reportedMessages().isEmpty() || this.reason() != null;
      }

      @Nullable
      @Override
      public Report.CannotBuildReason checkBuildable() {
         if (this.report.reportedMessages.isEmpty()) {
            return Report.CannotBuildReason.NO_REPORTED_MESSAGES;
         } else if (this.report.reportedMessages.size() > this.limits.maxReportedMessageCount()) {
            return Report.CannotBuildReason.TOO_MANY_MESSAGES;
         } else if (this.report.reason == null) {
            return Report.CannotBuildReason.NO_REASON;
         } else {
            return this.report.comments.length() > this.limits.maxOpinionCommentsLength() ? Report.CannotBuildReason.COMMENT_TOO_LONG : super.checkBuildable();
         }
      }

      @Override
      public Either<Report.Result, Report.CannotBuildReason> build(ReportingContext p_299928_) {
         Report.CannotBuildReason $$1 = this.checkBuildable();
         if ($$1 != null) {
            return Either.right($$1);
         } else {
            String $$2 = Objects.requireNonNull(this.report.reason).backendName();
            ReportEvidence $$3 = this.buildEvidence(p_299928_);
            ReportedEntity $$4 = new ReportedEntity(this.report.reportedProfileId);
            AbuseReport $$5 = AbuseReport.chat(this.report.comments, $$2, $$3, $$4, this.report.createdAt);
            return Either.left(new Report.Result(this.report.reportId, ReportType.CHAT, $$5));
         }
      }

      private ReportEvidence buildEvidence(ReportingContext p_299855_) {
         List<ReportChatMessage> $$1 = new ArrayList<>();
         ChatReportContextBuilder $$2 = new ChatReportContextBuilder(this.limits.leadingContextMessageCount());
         $$2.collectAllContext(
            p_299855_.chatLog(),
            this.report.reportedMessages,
            (p_299903_, p_300034_) -> $$1.add(this.buildReportedChatMessage(p_300034_, this.isReported(p_299903_)))
         );
         return new ReportEvidence(Lists.reverse($$1));
      }

      private ReportChatMessage buildReportedChatMessage(LoggedChatMessage.Player p_299916_, boolean p_299962_) {
         SignedMessageLink $$2 = p_299916_.message().link();
         SignedMessageBody $$3 = p_299916_.message().signedBody();
         List<ByteBuffer> $$4 = $$3.lastSeen().entries().stream().map(MessageSignature::asByteBuffer).toList();
         ByteBuffer $$5 = Optionull.map(p_299916_.message().signature(), MessageSignature::asByteBuffer);
         return new ReportChatMessage($$2.index(), $$2.sender(), $$2.sessionId(), $$3.timeStamp(), $$3.salt(), $$4, $$3.content(), $$5, p_299962_);
      }

      public ChatReport.Builder copy() {
         return new ChatReport.Builder(this.report.copy(), this.limits);
      }
   }
}
