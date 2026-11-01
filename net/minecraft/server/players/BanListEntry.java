package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public abstract class BanListEntry<T> extends StoredUserEntry<T> {
   public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT);
   public static final String EXPIRES_NEVER = "forever";
   protected final Date created;
   protected final String source;
   @Nullable
   protected final Date expires;
   @Nullable
   protected final String reason;

   public BanListEntry(@Nullable T user, @Nullable Date created, @Nullable String source, @Nullable Date expires, @Nullable String reason) {
      super(user);
      this.created = created == null ? new Date() : created;
      this.source = source == null ? "(Unknown)" : source;
      this.expires = expires;
      this.reason = reason;
   }

   protected BanListEntry(@Nullable T user, JsonObject entryData) {
      super(user);

      Date date;
      try {
         date = entryData.has("created") ? DATE_FORMAT.parse(entryData.get("created").getAsString()) : new Date();
      } catch (ParseException var7) {
         date = new Date();
      }

      this.created = date;
      this.source = entryData.has("source") ? entryData.get("source").getAsString() : "(Unknown)";

      Date date2;
      try {
         date2 = entryData.has("expires") ? DATE_FORMAT.parse(entryData.get("expires").getAsString()) : null;
      } catch (ParseException var6) {
         date2 = null;
      }

      this.expires = date2;
      this.reason = entryData.has("reason") ? entryData.get("reason").getAsString() : null;
   }

   public Date getCreated() {
      return this.created;
   }

   public String getSource() {
      return this.source;
   }

   @Nullable
   public Date getExpires() {
      return this.expires;
   }

   @Nullable
   public String getReason() {
      return this.reason;
   }

   public Component getReasonMessage() {
      String string = this.getReason();
      return string == null ? Component.translatable("multiplayer.disconnect.banned.reason.default") : Component.literal(string);
   }

   public abstract Component getDisplayName();

   @Override
   boolean hasExpired() {
      return this.expires == null ? false : this.expires.before(new Date());
   }

   @Override
   protected void serialize(JsonObject data) {
      data.addProperty("created", DATE_FORMAT.format(this.created));
      data.addProperty("source", this.source);
      data.addProperty("expires", this.expires == null ? "forever" : DATE_FORMAT.format(this.expires));
      data.addProperty("reason", this.reason);
   }

   @Override
   public boolean equals(Object object) {
      if (this == object) {
         return true;
      } else if (object != null && this.getClass() == object.getClass()) {
         BanListEntry<?> banListEntry = (BanListEntry<?>)object;
         return Objects.equals(this.source, banListEntry.source)
            && Objects.equals(this.expires, banListEntry.expires)
            && Objects.equals(this.reason, banListEntry.reason)
            && Objects.equals(this.getUser(), banListEntry.getUser());
      } else {
         return false;
      }
   }
}
