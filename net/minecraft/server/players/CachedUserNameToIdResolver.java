package net.minecraft.server.players;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import net.minecraft.util.StringUtil;
import org.slf4j.Logger;

public class CachedUserNameToIdResolver implements UserNameToIdResolver {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int GAMEPROFILES_MRU_LIMIT = 1000;
   private static final int GAMEPROFILES_EXPIRATION_MONTHS = 1;
   private boolean resolveOfflineUsers = true;
   private final Map<String, CachedUserNameToIdResolver.GameProfileInfo> profilesByName = new ConcurrentHashMap<>();
   private final Map<UUID, CachedUserNameToIdResolver.GameProfileInfo> profilesByUUID = new ConcurrentHashMap<>();
   private final GameProfileRepository profileRepository;
   private final Gson gson = new GsonBuilder().create();
   private final File file;
   private final AtomicLong operationCount = new AtomicLong();

   public CachedUserNameToIdResolver(GameProfileRepository p_433715_, File p_434510_) {
      this.profileRepository = p_433715_;
      this.file = p_434510_;
      Lists.reverse(this.load()).forEach(this::safeAdd);
   }

   private void safeAdd(CachedUserNameToIdResolver.GameProfileInfo p_434413_) {
      NameAndId nameandid = p_434413_.nameAndId();
      p_434413_.setLastAccess(this.getNextOperation());
      this.profilesByName.put(nameandid.name().toLowerCase(Locale.ROOT), p_434413_);
      this.profilesByUUID.put(nameandid.id(), p_434413_);
   }

   private Optional<NameAndId> lookupGameProfile(GameProfileRepository p_435754_, String p_433605_) {
      if (!StringUtil.isValidPlayerName(p_433605_)) {
         return this.createUnknownProfile(p_433605_);
      } else {
         Optional<NameAndId> optional = p_435754_.findProfileByName(p_433605_).map(NameAndId::new);
         return optional.isEmpty() ? this.createUnknownProfile(p_433605_) : optional;
      }
   }

   private Optional<NameAndId> createUnknownProfile(String p_434455_) {
      return this.resolveOfflineUsers ? Optional.of(NameAndId.createOffline(p_434455_)) : Optional.empty();
   }

   @Override
   public void resolveOfflineUsers(boolean p_435710_) {
      this.resolveOfflineUsers = p_435710_;
   }

   @Override
   public void add(NameAndId p_433575_) {
      this.addInternal(p_433575_);
   }

   private CachedUserNameToIdResolver.GameProfileInfo addInternal(NameAndId p_432796_) {
      Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.ROOT);
      calendar.setTime(new Date());
      calendar.add(2, 1);
      Date date = calendar.getTime();
      CachedUserNameToIdResolver.GameProfileInfo cachedusernametoidresolver$gameprofileinfo = new CachedUserNameToIdResolver.GameProfileInfo(p_432796_, date);
      this.safeAdd(cachedusernametoidresolver$gameprofileinfo);
      this.save();
      return cachedusernametoidresolver$gameprofileinfo;
   }

   private long getNextOperation() {
      return this.operationCount.incrementAndGet();
   }

   @Override
   public Optional<NameAndId> get(String p_434791_) {
      String s = p_434791_.toLowerCase(Locale.ROOT);
      CachedUserNameToIdResolver.GameProfileInfo cachedusernametoidresolver$gameprofileinfo = this.profilesByName.get(s);
      boolean flag = false;
      if (cachedusernametoidresolver$gameprofileinfo != null && new Date().getTime() >= cachedusernametoidresolver$gameprofileinfo.expirationDate.getTime()) {
         this.profilesByUUID.remove(cachedusernametoidresolver$gameprofileinfo.nameAndId().id());
         this.profilesByName.remove(cachedusernametoidresolver$gameprofileinfo.nameAndId().name().toLowerCase(Locale.ROOT));
         flag = true;
         cachedusernametoidresolver$gameprofileinfo = null;
      }

      Optional<NameAndId> optional;
      if (cachedusernametoidresolver$gameprofileinfo != null) {
         cachedusernametoidresolver$gameprofileinfo.setLastAccess(this.getNextOperation());
         optional = Optional.of(cachedusernametoidresolver$gameprofileinfo.nameAndId());
      } else {
         Optional<NameAndId> optional1 = this.lookupGameProfile(this.profileRepository, s);
         if (optional1.isPresent()) {
            optional = Optional.of(this.addInternal(optional1.get()).nameAndId());
            flag = false;
         } else {
            optional = Optional.empty();
         }
      }

      if (flag) {
         this.save();
      }

      return optional;
   }

   @Override
   public Optional<NameAndId> get(UUID p_434410_) {
      CachedUserNameToIdResolver.GameProfileInfo cachedusernametoidresolver$gameprofileinfo = this.profilesByUUID.get(p_434410_);
      if (cachedusernametoidresolver$gameprofileinfo == null) {
         return Optional.empty();
      } else {
         cachedusernametoidresolver$gameprofileinfo.setLastAccess(this.getNextOperation());
         return Optional.of(cachedusernametoidresolver$gameprofileinfo.nameAndId());
      }
   }

   private static DateFormat createDateFormat() {
      return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT);
   }

   private List<CachedUserNameToIdResolver.GameProfileInfo> load() {
      List<CachedUserNameToIdResolver.GameProfileInfo> list = Lists.newArrayList();

      try {
         Object object;
         try (Reader reader = Files.newReader(this.file, StandardCharsets.UTF_8)) {
            JsonArray jsonarray = (JsonArray)this.gson.fromJson(reader, JsonArray.class);
            if (jsonarray != null) {
               DateFormat dateformat = createDateFormat();
               jsonarray.forEach(p_433973_ -> readGameProfile(p_433973_, dateformat).ifPresent(list::add));
               return list;
            }

            object = list;
         }

         return (List<CachedUserNameToIdResolver.GameProfileInfo>)object;
      } catch (FileNotFoundException var9) {
      } catch (IOException | JsonParseException var10) {
         LOGGER.warn("Failed to load profile cache {}", this.file, var10);
      }

      return list;
   }

   @Override
   public void save() {
      JsonArray jsonarray = new JsonArray();
      DateFormat dateformat = createDateFormat();
      this.getTopMRUProfiles(1000).forEach(p_433148_ -> jsonarray.add(writeGameProfile(p_433148_, dateformat)));
      String s = this.gson.toJson(jsonarray);

      try (Writer writer = Files.newWriter(this.file, StandardCharsets.UTF_8)) {
         writer.write(s);
      } catch (IOException var9) {
      }
   }

   private Stream<CachedUserNameToIdResolver.GameProfileInfo> getTopMRUProfiles(int p_434027_) {
      return ImmutableList.copyOf(this.profilesByUUID.values())
         .stream()
         .sorted(Comparator.comparing(CachedUserNameToIdResolver.GameProfileInfo::lastAccess).reversed())
         .limit(p_434027_);
   }

   private static JsonElement writeGameProfile(CachedUserNameToIdResolver.GameProfileInfo p_432792_, DateFormat p_435791_) {
      JsonObject jsonobject = new JsonObject();
      p_432792_.nameAndId().appendTo(jsonobject);
      jsonobject.addProperty("expiresOn", p_435791_.format(p_432792_.expirationDate()));
      return jsonobject;
   }

   private static Optional<CachedUserNameToIdResolver.GameProfileInfo> readGameProfile(JsonElement p_435574_, DateFormat p_435392_) {
      if (p_435574_.isJsonObject()) {
         JsonObject jsonobject = p_435574_.getAsJsonObject();
         NameAndId nameandid = NameAndId.fromJson(jsonobject);
         if (nameandid != null) {
            JsonElement jsonelement = jsonobject.get("expiresOn");
            if (jsonelement != null) {
               String s = jsonelement.getAsString();

               try {
                  Date date = p_435392_.parse(s);
                  return Optional.of(new CachedUserNameToIdResolver.GameProfileInfo(nameandid, date));
               } catch (ParseException var7) {
                  LOGGER.warn("Failed to parse date {}", s, var7);
               }
            }
         }
      }

      return Optional.empty();
   }

   static class GameProfileInfo {
      private final NameAndId nameAndId;
      final Date expirationDate;
      private volatile long lastAccess;

      GameProfileInfo(NameAndId p_435526_, Date p_435479_) {
         this.nameAndId = p_435526_;
         this.expirationDate = p_435479_;
      }

      public NameAndId nameAndId() {
         return this.nameAndId;
      }

      public Date expirationDate() {
         return this.expirationDate;
      }

      public void setLastAccess(long p_435342_) {
         this.lastAccess = p_435342_;
      }

      public long lastAccess() {
         return this.lastAccess;
      }
   }
}
