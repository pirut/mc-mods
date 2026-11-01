package net.minecraft.util;

import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.MoreExecutors;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.jtracy.TracyClient;
import com.mojang.jtracy.Zone;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceImmutableList;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import net.minecraft.CharPredicate;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.ReportType;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.SuppressForbidden;
import net.minecraft.TracingExecutor;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Util {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int DEFAULT_MAX_THREADS = 255;
   private static final int DEFAULT_SAFE_FILE_OPERATION_RETRIES = 10;
   private static final String MAX_THREADS_SYSTEM_PROPERTY = "max.bg.threads";
   private static final TracingExecutor BACKGROUND_EXECUTOR = makeExecutor("Main");
   private static final TracingExecutor IO_POOL = makeIoExecutor("IO-Worker-", false);
   private static final TracingExecutor DOWNLOAD_POOL = makeIoExecutor("Download-", true);
   private static final DateTimeFormatter FILENAME_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss", Locale.ROOT);
   public static final int LINEAR_LOOKUP_THRESHOLD = 8;
   private static final Set<String> ALLOWED_UNTRUSTED_LINK_PROTOCOLS = Set.of("http", "https");
   public static final long NANOS_PER_MILLI = 1000000L;
   public static TimeSource.NanoTimeSource timeSource = System::nanoTime;
   public static final Ticker TICKER = new Ticker() {
      public long read() {
         return Util.timeSource.getAsLong();
      }
   };
   public static final UUID NIL_UUID = new UUID(0L, 0L);
   public static final FileSystemProvider ZIP_FILE_SYSTEM_PROVIDER = FileSystemProvider.installedProviders()
      .stream()
      .filter(p_468786_ -> p_468786_.getScheme().equalsIgnoreCase("jar"))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("No jar file system provider found"));
   private static Consumer<String> thePauser = p_468421_ -> {};

   public static <K, V> Collector<Entry<? extends K, ? extends V>, ?, Map<K, V>> toMap() {
      return Collectors.toMap(Entry::getKey, Entry::getValue);
   }

   public static <T> Collector<T, ?, List<T>> toMutableList() {
      return Collectors.toCollection(Lists::newArrayList);
   }

   public static <T extends Comparable<T>> String getPropertyName(Property<T> p_468140_, Object p_469219_) {
      return p_468140_.getName((T)p_469219_);
   }

   public static String makeDescriptionId(String p_468143_, @Nullable Identifier p_467970_) {
      return p_467970_ == null ? p_468143_ + ".unregistered_sadface" : p_468143_ + "." + p_467970_.getNamespace() + "." + p_467970_.getPath().replace('/', '.');
   }

   public static long getMillis() {
      return getNanos() / 1000000L;
   }

   public static long getNanos() {
      return timeSource.getAsLong();
   }

   public static long getEpochMillis() {
      return Instant.now().toEpochMilli();
   }

   public static String getFilenameFormattedDateTime() {
      return FILENAME_DATE_TIME_FORMATTER.format(ZonedDateTime.now());
   }

   private static TracingExecutor makeExecutor(String p_467685_) {
      int i = maxAllowedExecutorThreads();
      ExecutorService executorservice;
      if (i <= 0) {
         executorservice = MoreExecutors.newDirectExecutorService();
      } else {
         AtomicInteger atomicinteger = new AtomicInteger(1);
         executorservice = new ForkJoinPool(i, p_469342_ -> {
            final String s = "Worker-" + p_467685_ + "-" + atomicinteger.getAndIncrement();
            ForkJoinWorkerThread forkjoinworkerthread = new ForkJoinWorkerThread(p_469342_) {
               @Override
               protected void onStart() {
                  TracyClient.setThreadName(s, p_467685_.hashCode());
                  super.onStart();
               }

               @Override
               protected void onTermination(@Nullable Throwable p_468348_) {
                  if (p_468348_ != null) {
                     Util.LOGGER.warn("{} died", this.getName(), p_468348_);
                  } else {
                     Util.LOGGER.debug("{} shutdown", this.getName());
                  }

                  super.onTermination(p_468348_);
               }
            };
            forkjoinworkerthread.setName(s);
            return forkjoinworkerthread;
         }, Util::onThreadException, true);
      }

      return new TracingExecutor(executorservice);
   }

   public static int maxAllowedExecutorThreads() {
      return Mth.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, getMaxThreads());
   }

   private static int getMaxThreads() {
      String s = System.getProperty("max.bg.threads");
      if (s != null) {
         try {
            int i = Integer.parseInt(s);
            if (i >= 1 && i <= 255) {
               return i;
            }

            LOGGER.error("Wrong {} property value '{}'. Should be an integer value between 1 and {}.", new Object[]{"max.bg.threads", s, 255});
         } catch (NumberFormatException var2) {
            LOGGER.error("Could not parse {} property value '{}'. Should be an integer value between 1 and {}.", new Object[]{"max.bg.threads", s, 255});
         }
      }

      return 255;
   }

   public static TracingExecutor backgroundExecutor() {
      return BACKGROUND_EXECUTOR;
   }

   public static TracingExecutor ioPool() {
      return IO_POOL;
   }

   public static TracingExecutor nonCriticalIoPool() {
      return DOWNLOAD_POOL;
   }

   public static void shutdownExecutors() {
      BACKGROUND_EXECUTOR.shutdownAndAwait(3L, TimeUnit.SECONDS);
      IO_POOL.shutdownAndAwait(3L, TimeUnit.SECONDS);
   }

   private static TracingExecutor makeIoExecutor(String p_469747_, boolean p_467449_) {
      AtomicInteger atomicinteger = new AtomicInteger(1);
      return new TracingExecutor(Executors.newCachedThreadPool(p_467103_ -> {
         Thread thread = new Thread(p_467103_);
         String s = p_469747_ + atomicinteger.getAndIncrement();
         TracyClient.setThreadName(s, p_469747_.hashCode());
         thread.setName(s);
         thread.setDaemon(p_467449_);
         thread.setUncaughtExceptionHandler(Util::onThreadException);
         return thread;
      }));
   }

   public static void throwAsRuntime(Throwable p_467632_) {
      throw p_467632_ instanceof RuntimeException ? (RuntimeException)p_467632_ : new RuntimeException(p_467632_);
   }

   private static void onThreadException(Thread p_469534_, Throwable p_469306_) {
      pauseInIde(p_469306_);
      if (p_469306_ instanceof CompletionException) {
         p_469306_ = p_469306_.getCause();
      }

      if (p_469306_ instanceof ReportedException reportedexception) {
         Bootstrap.realStdoutPrintln(reportedexception.getReport().getFriendlyReport(ReportType.CRASH));
         System.exit(-1);
      }

      LOGGER.error("Caught exception in thread {}", p_469534_, p_469306_);
   }

   @Nullable
   public static Type<?> fetchChoiceType(TypeReference p_468078_, String p_467952_) {
      return !SharedConstants.CHECK_DATA_FIXER_SCHEMA ? null : doFetchChoiceType(p_468078_, p_467952_);
   }

   @Nullable
   private static Type<?> doFetchChoiceType(TypeReference p_469919_, String p_468897_) {
      Type<?> type = null;

      try {
         type = DataFixers.getDataFixer()
            .getSchema(DataFixUtils.makeKey(SharedConstants.getCurrentVersion().dataVersion().version()))
            .getChoiceType(p_469919_, p_468897_);
      } catch (IllegalArgumentException var4) {
         LOGGER.debug("No data fixer registered for {}", p_468897_);
         if (SharedConstants.IS_RUNNING_IN_IDE) {
         }
      }

      return type;
   }

   public static void runNamed(Runnable p_468092_, String p_466922_) {
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         Thread thread = Thread.currentThread();
         String s = thread.getName();
         thread.setName(p_466922_);

         try {
            Zone zone = TracyClient.beginZone(p_466922_, SharedConstants.IS_RUNNING_IN_IDE);

            try {
               p_468092_.run();
            } catch (Throwable var16) {
               if (zone != null) {
                  try {
                     zone.close();
                  } catch (Throwable var14) {
                     var16.addSuppressed(var14);
                  }
               }

               throw var16;
            }

            if (zone != null) {
               zone.close();
            }
         } finally {
            thread.setName(s);
         }
      } else {
         Zone zone1 = TracyClient.beginZone(p_466922_, SharedConstants.IS_RUNNING_IN_IDE);

         try {
            p_468092_.run();
         } catch (Throwable var15) {
            if (zone1 != null) {
               try {
                  zone1.close();
               } catch (Throwable var13) {
                  var15.addSuppressed(var13);
               }
            }

            throw var15;
         }

         if (zone1 != null) {
            zone1.close();
         }
      }
   }

   public static <T> String getRegisteredName(Registry<T> p_466875_, T p_468867_) {
      Identifier identifier = p_466875_.getKey(p_468867_);
      return identifier == null ? "[unregistered]" : identifier.toString();
   }

   public static <T> Predicate<T> allOf() {
      return p_467747_ -> true;
   }

   public static <T> Predicate<T> allOf(Predicate<? super T> p_468363_) {
      return (Predicate<T>)p_468363_;
   }

   public static <T> Predicate<T> allOf(Predicate<? super T> p_468947_, Predicate<? super T> p_468902_) {
      return p_469328_ -> p_468947_.test(p_469328_) && p_468902_.test(p_469328_);
   }

   public static <T> Predicate<T> allOf(Predicate<? super T> p_467616_, Predicate<? super T> p_466869_, Predicate<? super T> p_468037_) {
      return p_469918_ -> p_467616_.test(p_469918_) && p_466869_.test(p_469918_) && p_468037_.test(p_469918_);
   }

   public static <T> Predicate<T> allOf(
      Predicate<? super T> p_466863_, Predicate<? super T> p_469884_, Predicate<? super T> p_466965_, Predicate<? super T> p_469278_
   ) {
      return p_469436_ -> p_466863_.test(p_469436_) && p_469884_.test(p_469436_) && p_466965_.test(p_469436_) && p_469278_.test(p_469436_);
   }

   public static <T> Predicate<T> allOf(
      Predicate<? super T> p_467541_,
      Predicate<? super T> p_468154_,
      Predicate<? super T> p_467878_,
      Predicate<? super T> p_468100_,
      Predicate<? super T> p_468822_
   ) {
      return p_468898_ -> p_467541_.test(p_468898_)
         && p_468154_.test(p_468898_)
         && p_467878_.test(p_468898_)
         && p_468100_.test(p_468898_)
         && p_468822_.test(p_468898_);
   }

   @SafeVarargs
   public static <T> Predicate<T> allOf(Predicate<? super T>... p_469840_) {
      return p_466978_ -> {
         for (Predicate<? super T> predicate : p_469840_) {
            if (!predicate.test(p_466978_)) {
               return false;
            }
         }

         return true;
      };
   }

   public static <T> Predicate<T> allOf(List<? extends Predicate<? super T>> p_468893_) {
      return switch (p_468893_.size()) {
         case 0 -> allOf();
         case 1 -> allOf((Predicate<? super T>)p_468893_.get(0));
         case 2 -> allOf((Predicate<? super T>)p_468893_.get(0), (Predicate<? super T>)p_468893_.get(1));
         case 3 -> allOf((Predicate<? super T>)p_468893_.get(0), (Predicate<? super T>)p_468893_.get(1), (Predicate<? super T>)p_468893_.get(2));
         case 4 -> allOf(
            (Predicate<? super T>)p_468893_.get(0),
            (Predicate<? super T>)p_468893_.get(1),
            (Predicate<? super T>)p_468893_.get(2),
            (Predicate<? super T>)p_468893_.get(3)
         );
         case 5 -> allOf(
            (Predicate<? super T>)p_468893_.get(0),
            (Predicate<? super T>)p_468893_.get(1),
            (Predicate<? super T>)p_468893_.get(2),
            (Predicate<? super T>)p_468893_.get(3),
            (Predicate<? super T>)p_468893_.get(4)
         );
         default -> {
            Predicate<? super T>[] predicate = p_468893_.toArray(Predicate[]::new);
            yield allOf(predicate);
         }
      };
   }

   public static <T> Predicate<T> anyOf() {
      return p_468777_ -> false;
   }

   public static <T> Predicate<T> anyOf(Predicate<? super T> p_469458_) {
      return (Predicate<T>)p_469458_;
   }

   public static <T> Predicate<T> anyOf(Predicate<? super T> p_469516_, Predicate<? super T> p_469291_) {
      return p_469740_ -> p_469516_.test(p_469740_) || p_469291_.test(p_469740_);
   }

   public static <T> Predicate<T> anyOf(Predicate<? super T> p_469539_, Predicate<? super T> p_469634_, Predicate<? super T> p_469378_) {
      return p_467957_ -> p_469539_.test(p_467957_) || p_469634_.test(p_467957_) || p_469378_.test(p_467957_);
   }

   public static <T> Predicate<T> anyOf(
      Predicate<? super T> p_468240_, Predicate<? super T> p_467765_, Predicate<? super T> p_466904_, Predicate<? super T> p_469434_
   ) {
      return p_468076_ -> p_468240_.test(p_468076_) || p_467765_.test(p_468076_) || p_466904_.test(p_468076_) || p_469434_.test(p_468076_);
   }

   public static <T> Predicate<T> anyOf(
      Predicate<? super T> p_468760_,
      Predicate<? super T> p_468172_,
      Predicate<? super T> p_468194_,
      Predicate<? super T> p_467498_,
      Predicate<? super T> p_468022_
   ) {
      return p_467694_ -> p_468760_.test(p_467694_)
         || p_468172_.test(p_467694_)
         || p_468194_.test(p_467694_)
         || p_467498_.test(p_467694_)
         || p_468022_.test(p_467694_);
   }

   @SafeVarargs
   public static <T> Predicate<T> anyOf(Predicate<? super T>... p_466972_) {
      return p_469132_ -> {
         for (Predicate<? super T> predicate : p_466972_) {
            if (predicate.test(p_469132_)) {
               return true;
            }
         }

         return false;
      };
   }

   public static <T> Predicate<T> anyOf(List<? extends Predicate<? super T>> p_467757_) {
      return switch (p_467757_.size()) {
         case 0 -> anyOf();
         case 1 -> anyOf((Predicate<? super T>)p_467757_.get(0));
         case 2 -> anyOf((Predicate<? super T>)p_467757_.get(0), (Predicate<? super T>)p_467757_.get(1));
         case 3 -> anyOf((Predicate<? super T>)p_467757_.get(0), (Predicate<? super T>)p_467757_.get(1), (Predicate<? super T>)p_467757_.get(2));
         case 4 -> anyOf(
            (Predicate<? super T>)p_467757_.get(0),
            (Predicate<? super T>)p_467757_.get(1),
            (Predicate<? super T>)p_467757_.get(2),
            (Predicate<? super T>)p_467757_.get(3)
         );
         case 5 -> anyOf(
            (Predicate<? super T>)p_467757_.get(0),
            (Predicate<? super T>)p_467757_.get(1),
            (Predicate<? super T>)p_467757_.get(2),
            (Predicate<? super T>)p_467757_.get(3),
            (Predicate<? super T>)p_467757_.get(4)
         );
         default -> {
            Predicate<? super T>[] predicate = p_467757_.toArray(Predicate[]::new);
            yield anyOf(predicate);
         }
      };
   }

   public static <T> boolean isSymmetrical(int p_467094_, int p_467306_, List<T> p_467064_) {
      if (p_467094_ == 1) {
         return true;
      } else {
         int i = p_467094_ / 2;

         for (int j = 0; j < p_467306_; j++) {
            for (int k = 0; k < i; k++) {
               int l = p_467094_ - 1 - k;
               T t = p_467064_.get(k + j * p_467094_);
               T t1 = p_467064_.get(l + j * p_467094_);
               if (!t.equals(t1)) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   public static int growByHalf(int p_469481_, int p_469368_) {
      return (int)Math.max(Math.min((long)p_469481_ + (p_469481_ >> 1), 2147483639L), (long)p_469368_);
   }

   @SuppressForbidden(
      reason = "Intentional use of default locale for user-visible date"
   )
   public static DateTimeFormatter localizedDateFormatter(FormatStyle p_467372_) {
      return DateTimeFormatter.ofLocalizedDateTime(p_467372_);
   }

   public static Util.OS getPlatform() {
      String s = System.getProperty("os.name").toLowerCase(Locale.ROOT);
      if (s.contains("win")) {
         return Util.OS.WINDOWS;
      } else if (s.contains("mac")) {
         return Util.OS.OSX;
      } else if (s.contains("solaris")) {
         return Util.OS.SOLARIS;
      } else if (s.contains("sunos")) {
         return Util.OS.SOLARIS;
      } else if (s.contains("linux")) {
         return Util.OS.LINUX;
      } else {
         return s.contains("unix") ? Util.OS.LINUX : Util.OS.UNKNOWN;
      }
   }

   public static boolean isAarch64() {
      String s = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
      return s.equals("aarch64");
   }

   public static URI parseAndValidateUntrustedUri(String p_469477_) throws URISyntaxException {
      URI uri = new URI(p_469477_);
      String s = uri.getScheme();
      if (s == null) {
         throw new URISyntaxException(p_469477_, "Missing protocol in URI: " + p_469477_);
      } else {
         String s1 = s.toLowerCase(Locale.ROOT);
         if (!ALLOWED_UNTRUSTED_LINK_PROTOCOLS.contains(s1)) {
            throw new URISyntaxException(p_469477_, "Unsupported protocol in URI: " + p_469477_);
         } else {
            return uri;
         }
      }
   }

   public static <T> T findNextInIterable(Iterable<T> p_467678_, @Nullable T p_467343_) {
      Iterator<T> iterator = p_467678_.iterator();
      T t = iterator.next();
      if (p_467343_ != null) {
         T t1 = t;

         while (t1 != p_467343_) {
            if (iterator.hasNext()) {
               t1 = iterator.next();
            }
         }

         if (iterator.hasNext()) {
            return iterator.next();
         }
      }

      return t;
   }

   public static <T> T findPreviousInIterable(Iterable<T> p_468213_, @Nullable T p_468859_) {
      Iterator<T> iterator = p_468213_.iterator();
      T t = null;

      while (iterator.hasNext()) {
         T t1 = iterator.next();
         if (t1 == p_468859_) {
            if (t == null) {
               t = (T)(iterator.hasNext() ? Iterators.getLast(iterator) : p_468859_);
            }
            break;
         }

         t = t1;
      }

      return t;
   }

   public static <T> T make(Supplier<T> p_469035_) {
      return p_469035_.get();
   }

   public static <T> T make(T p_469905_, Consumer<? super T> p_466877_) {
      p_466877_.accept(p_469905_);
      return p_469905_;
   }

   public static <K extends Enum<K>, V> Map<K, V> makeEnumMap(Class<K> p_469271_, Function<K, V> p_467589_) {
      EnumMap<K, V> enummap = new EnumMap<>(p_469271_);

      for (K k : (Enum[])p_469271_.getEnumConstants()) {
         enummap.put(k, p_467589_.apply(k));
      }

      return enummap;
   }

   public static <K, V1, V2> Map<K, V2> mapValues(Map<K, V1> p_466975_, Function<? super V1, V2> p_469099_) {
      return p_466975_.entrySet().stream().collect(Collectors.toMap(Entry::getKey, p_468278_ -> p_469099_.apply(p_468278_.getValue())));
   }

   public static <K, V1, V2> Map<K, V2> mapValuesLazy(Map<K, V1> p_467772_, com.google.common.base.Function<V1, V2> p_467662_) {
      return Maps.transformValues(p_467772_, p_467662_);
   }

   public static <V> CompletableFuture<List<V>> sequence(List<? extends CompletableFuture<V>> p_469265_) {
      if (p_469265_.isEmpty()) {
         return CompletableFuture.completedFuture(List.of());
      } else if (p_469265_.size() == 1) {
         return p_469265_.getFirst().thenApply(ObjectLists::singleton);
      } else {
         CompletableFuture<Void> completablefuture = CompletableFuture.allOf(p_469265_.toArray(new CompletableFuture[0]));
         return completablefuture.thenApply(p_469257_ -> p_469265_.stream().map(CompletableFuture::join).toList());
      }
   }

   public static <V> CompletableFuture<List<V>> sequenceFailFast(List<? extends CompletableFuture<? extends V>> p_468426_) {
      CompletableFuture<List<V>> completablefuture = new CompletableFuture<>();
      return fallibleSequence(p_468426_, completablefuture::completeExceptionally).applyToEither(completablefuture, Function.identity());
   }

   public static <V> CompletableFuture<List<V>> sequenceFailFastAndCancel(List<? extends CompletableFuture<? extends V>> p_468642_) {
      CompletableFuture<List<V>> completablefuture = new CompletableFuture<>();
      return fallibleSequence(p_468642_, p_467779_ -> {
         if (completablefuture.completeExceptionally(p_467779_)) {
            for (CompletableFuture<? extends V> completablefuture1 : p_468642_) {
               completablefuture1.cancel(true);
            }
         }
      }).applyToEither(completablefuture, Function.identity());
   }

   private static <V> CompletableFuture<List<V>> fallibleSequence(List<? extends CompletableFuture<? extends V>> p_469032_, Consumer<Throwable> p_468688_) {
      ObjectArrayList<V> objectarraylist = new ObjectArrayList();
      objectarraylist.size(p_469032_.size());
      CompletableFuture<?>[] completablefuture = new CompletableFuture[p_469032_.size()];

      for (int i = 0; i < p_469032_.size(); i++) {
         int j = i;
         completablefuture[i] = p_469032_.get(i).whenComplete((p_469443_, p_469317_) -> {
            if (p_469317_ != null) {
               p_468688_.accept(p_469317_);
            } else {
               objectarraylist.set(j, p_469443_);
            }
         });
      }

      return CompletableFuture.allOf(completablefuture).thenApply(p_467749_ -> objectarraylist);
   }

   public static <T> Optional<T> ifElse(Optional<T> p_468275_, Consumer<T> p_467098_, Runnable p_467701_) {
      if (p_468275_.isPresent()) {
         p_467098_.accept(p_468275_.get());
      } else {
         p_467701_.run();
      }

      return p_468275_;
   }

   public static <T> Supplier<T> name(final Supplier<T> p_469820_, Supplier<String> p_467265_) {
      if (SharedConstants.DEBUG_NAMED_RUNNABLES) {
         final String s = p_467265_.get();
         return new Supplier<T>() {
            @Override
            public T get() {
               return p_469820_.get();
            }

            @Override
            public String toString() {
               return s;
            }
         };
      } else {
         return p_469820_;
      }
   }

   public static Runnable name(final Runnable p_468549_, Supplier<String> p_468608_) {
      if (SharedConstants.DEBUG_NAMED_RUNNABLES) {
         final String s = p_468608_.get();
         return new Runnable() {
            @Override
            public void run() {
               p_468549_.run();
            }

            @Override
            public String toString() {
               return s;
            }
         };
      } else {
         return p_468549_;
      }
   }

   public static void logAndPauseIfInIde(String p_468059_) {
      LOGGER.error(p_468059_);
      if (SharedConstants.IS_RUNNING_WITH_JDWP) {
         doPause(p_468059_);
      }
   }

   public static void logAndPauseIfInIde(String p_467743_, Throwable p_467826_) {
      LOGGER.error(p_467743_, p_467826_);
      if (SharedConstants.IS_RUNNING_WITH_JDWP) {
         doPause(p_467743_);
      }
   }

   public static <T extends Throwable> T pauseInIde(T p_469784_) {
      if (SharedConstants.IS_RUNNING_WITH_JDWP) {
         LOGGER.error("Trying to throw a fatal exception, pausing in IDE", p_469784_);
         doPause(p_469784_.getMessage());
      }

      return p_469784_;
   }

   public static void setPause(Consumer<String> p_466884_) {
      thePauser = p_466884_;
   }

   private static void doPause(String p_468405_) {
      Instant instant = Instant.now();
      LOGGER.warn("Did you remember to set a breakpoint here?");
      boolean flag = Duration.between(instant, Instant.now()).toMillis() > 500L;
      if (!flag) {
         thePauser.accept(p_468405_);
      }
   }

   public static String describeError(Throwable p_469759_) {
      if (p_469759_.getCause() != null) {
         return describeError(p_469759_.getCause());
      } else {
         return p_469759_.getMessage() != null ? p_469759_.getMessage() : p_469759_.toString();
      }
   }

   public static <T> T getRandom(T[] p_467188_, RandomSource p_468635_) {
      return p_467188_[p_468635_.nextInt(p_467188_.length)];
   }

   public static int getRandom(int[] p_469236_, RandomSource p_469725_) {
      return p_469236_[p_469725_.nextInt(p_469236_.length)];
   }

   public static <T> T getRandom(List<T> p_468826_, RandomSource p_468847_) {
      return p_468826_.get(p_468847_.nextInt(p_468826_.size()));
   }

   public static <T> Optional<T> getRandomSafe(List<T> p_467289_, RandomSource p_469749_) {
      return p_467289_.isEmpty() ? Optional.empty() : Optional.of(getRandom(p_467289_, p_469749_));
   }

   private static BooleanSupplier createRenamer(final Path p_469943_, final Path p_468609_) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            try {
               Files.move(p_469943_, p_468609_);
               return true;
            } catch (IOException var2) {
               Util.LOGGER.error("Failed to rename", var2);
               return false;
            }
         }

         @Override
         public String toString() {
            return "rename " + p_469943_ + " to " + p_468609_;
         }
      };
   }

   private static BooleanSupplier createDeleter(final Path p_466916_) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            try {
               Files.deleteIfExists(p_466916_);
               return true;
            } catch (IOException var2) {
               Util.LOGGER.warn("Failed to delete", var2);
               return false;
            }
         }

         @Override
         public String toString() {
            return "delete old " + p_466916_;
         }
      };
   }

   private static BooleanSupplier createFileDeletedCheck(final Path p_469937_) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return !Files.exists(p_469937_);
         }

         @Override
         public String toString() {
            return "verify that " + p_469937_ + " is deleted";
         }
      };
   }

   private static BooleanSupplier createFileCreatedCheck(final Path p_469266_) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return Files.isRegularFile(p_469266_);
         }

         @Override
         public String toString() {
            return "verify that " + p_469266_ + " is present";
         }
      };
   }

   private static boolean executeInSequence(BooleanSupplier... p_467698_) {
      for (BooleanSupplier booleansupplier : p_467698_) {
         if (!booleansupplier.getAsBoolean()) {
            LOGGER.warn("Failed to execute {}", booleansupplier);
            return false;
         }
      }

      return true;
   }

   private static boolean runWithRetries(int p_468312_, String p_469064_, BooleanSupplier... p_467353_) {
      for (int i = 0; i < p_468312_; i++) {
         if (executeInSequence(p_467353_)) {
            return true;
         }

         LOGGER.error("Failed to {}, retrying {}/{}", new Object[]{p_469064_, i, p_468312_});
      }

      LOGGER.error("Failed to {}, aborting, progress might be lost", p_469064_);
      return false;
   }

   public static void safeReplaceFile(Path p_466993_, Path p_469938_, Path p_469625_) {
      safeReplaceOrMoveFile(p_466993_, p_469938_, p_469625_, false);
   }

   public static boolean safeReplaceOrMoveFile(Path p_469004_, Path p_467473_, Path p_467311_, boolean p_468176_) {
      if (Files.exists(p_469004_)
         && !runWithRetries(10, "create backup " + p_467311_, createDeleter(p_467311_), createRenamer(p_469004_, p_467311_), createFileCreatedCheck(p_467311_))
         )
       {
         return false;
      } else if (!runWithRetries(10, "remove old " + p_469004_, createDeleter(p_469004_), createFileDeletedCheck(p_469004_))) {
         return false;
      } else if (!runWithRetries(10, "replace " + p_469004_ + " with " + p_467473_, createRenamer(p_467473_, p_469004_), createFileCreatedCheck(p_469004_))
         && !p_468176_) {
         runWithRetries(10, "restore " + p_469004_ + " from " + p_467311_, createRenamer(p_467311_, p_469004_), createFileCreatedCheck(p_469004_));
         return false;
      } else {
         return true;
      }
   }

   public static int offsetByCodepoints(String p_467147_, int p_468051_, int p_467748_) {
      int i = p_467147_.length();
      if (p_467748_ >= 0) {
         for (int j = 0; p_468051_ < i && j < p_467748_; j++) {
            if (Character.isHighSurrogate(p_467147_.charAt(p_468051_++)) && p_468051_ < i && Character.isLowSurrogate(p_467147_.charAt(p_468051_))) {
               p_468051_++;
            }
         }
      } else {
         for (int k = p_467748_; p_468051_ > 0 && k < 0; k++) {
            p_468051_--;
            if (Character.isLowSurrogate(p_467147_.charAt(p_468051_)) && p_468051_ > 0 && Character.isHighSurrogate(p_467147_.charAt(p_468051_ - 1))) {
               p_468051_--;
            }
         }
      }

      return p_468051_;
   }

   public static Consumer<String> prefix(String p_468108_, Consumer<String> p_469459_) {
      return p_469438_ -> p_469459_.accept(p_468108_ + p_469438_);
   }

   public static DataResult<int[]> fixedSize(IntStream p_468255_, int p_468306_) {
      int[] aint = p_468255_.limit(p_468306_ + 1).toArray();
      if (aint.length != p_468306_) {
         Supplier<String> supplier = () -> "Input is not a list of " + p_468306_ + " ints";
         return aint.length >= p_468306_ ? DataResult.error(supplier, Arrays.copyOf(aint, p_468306_)) : DataResult.error(supplier);
      } else {
         return DataResult.success(aint);
      }
   }

   public static DataResult<long[]> fixedSize(LongStream p_467636_, int p_469509_) {
      long[] along = p_467636_.limit(p_469509_ + 1).toArray();
      if (along.length != p_469509_) {
         Supplier<String> supplier = () -> "Input is not a list of " + p_469509_ + " longs";
         return along.length >= p_469509_ ? DataResult.error(supplier, Arrays.copyOf(along, p_469509_)) : DataResult.error(supplier);
      } else {
         return DataResult.success(along);
      }
   }

   public static <T> DataResult<List<T>> fixedSize(List<T> p_468349_, int p_469120_) {
      if (p_468349_.size() != p_469120_) {
         Supplier<String> supplier = () -> "Input is not a list of " + p_469120_ + " elements";
         return p_468349_.size() >= p_469120_ ? DataResult.error(supplier, p_468349_.subList(0, p_469120_)) : DataResult.error(supplier);
      } else {
         return DataResult.success(p_468349_);
      }
   }

   public static void startTimerHackThread() {
      Thread thread = new Thread("Timer hack thread") {
         @Override
         public void run() {
            while (true) {
               try {
                  Thread.sleep(2147483647L);
               } catch (InterruptedException var2) {
                  Util.LOGGER.warn("Timer hack thread interrupted, that really should not happen");
                  return;
               }
            }
         }
      };
      thread.setDaemon(true);
      thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
      thread.start();
   }

   public static void copyBetweenDirs(Path p_468586_, Path p_467955_, Path p_466957_) throws IOException {
      Path path = p_468586_.relativize(p_466957_);
      Path path1 = p_467955_.resolve(path);
      Files.copy(p_466957_, path1);
   }

   public static String sanitizeName(String p_469246_, CharPredicate p_467125_) {
      return p_469246_.toLowerCase(Locale.ROOT)
         .chars()
         .mapToObj(p_468819_ -> p_467125_.test((char)p_468819_) ? Character.toString((char)p_468819_) : "_")
         .collect(Collectors.joining());
   }

   public static <K, V> SingleKeyCache<K, V> singleKeyCache(Function<K, V> p_468531_) {
      return new SingleKeyCache<>(p_468531_);
   }

   public static <T, R> Function<T, R> memoize(final Function<T, R> p_469591_) {
      return new Function<T, R>() {
         private final Map<T, R> cache = new ConcurrentHashMap<>();

         @Override
         public R apply(T p_469715_) {
            return this.cache.computeIfAbsent(p_469715_, p_469591_);
         }

         @Override
         public String toString() {
            return "memoize/1[function=" + p_469591_ + ", size=" + this.cache.size() + "]";
         }
      };
   }

   public static <T, U, R> BiFunction<T, U, R> memoize(final BiFunction<T, U, R> p_469639_) {
      return new BiFunction<T, U, R>() {
         private final Map<Pair<T, U>, R> cache = new ConcurrentHashMap<>();

         @Override
         public R apply(T p_467751_, U p_468856_) {
            return this.cache.computeIfAbsent(Pair.of(p_467751_, p_468856_), p_469913_ -> p_469639_.apply((T)p_469913_.getFirst(), (U)p_469913_.getSecond()));
         }

         @Override
         public String toString() {
            return "memoize/2[function=" + p_469639_ + ", size=" + this.cache.size() + "]";
         }
      };
   }

   public static <T> List<T> toShuffledList(Stream<T> p_469161_, RandomSource p_468120_) {
      ObjectArrayList<T> objectarraylist = p_469161_.collect(ObjectArrayList.toList());
      shuffle(objectarraylist, p_468120_);
      return objectarraylist;
   }

   public static IntArrayList toShuffledList(IntStream p_469172_, RandomSource p_469961_) {
      IntArrayList intarraylist = IntArrayList.wrap(p_469172_.toArray());
      int i = intarraylist.size();

      for (int j = i; j > 1; j--) {
         int k = p_469961_.nextInt(j);
         intarraylist.set(j - 1, intarraylist.set(k, intarraylist.getInt(j - 1)));
      }

      return intarraylist;
   }

   public static <T> List<T> shuffledCopy(T[] p_468818_, RandomSource p_468693_) {
      ObjectArrayList<T> objectarraylist = new ObjectArrayList(p_468818_);
      shuffle(objectarraylist, p_468693_);
      return objectarraylist;
   }

   public static <T> List<T> shuffledCopy(ObjectArrayList<T> p_468588_, RandomSource p_469953_) {
      ObjectArrayList<T> objectarraylist = new ObjectArrayList(p_468588_);
      shuffle(objectarraylist, p_469953_);
      return objectarraylist;
   }

   public static <T> void shuffle(List<T> p_469288_, RandomSource p_467278_) {
      int i = p_469288_.size();

      for (int j = i; j > 1; j--) {
         int k = p_467278_.nextInt(j);
         p_469288_.set(j - 1, p_469288_.set(k, p_469288_.get(j - 1)));
      }
   }

   public static <T> CompletableFuture<T> blockUntilDone(Function<Executor, CompletableFuture<T>> p_468010_) {
      return blockUntilDone(p_468010_, CompletableFuture::isDone);
   }

   public static <T> T blockUntilDone(Function<Executor, T> p_467291_, Predicate<T> p_469800_) {
      BlockingQueue<Runnable> blockingqueue = new LinkedBlockingQueue<>();
      T t = p_467291_.apply(blockingqueue::add);

      while (!p_469800_.test(t)) {
         try {
            Runnable runnable = blockingqueue.poll(100L, TimeUnit.MILLISECONDS);
            if (runnable != null) {
               runnable.run();
            }
         } catch (InterruptedException var5) {
            LOGGER.warn("Interrupted wait");
            break;
         }
      }

      int i = blockingqueue.size();
      if (i > 0) {
         LOGGER.warn("Tasks left in queue: {}", i);
      }

      return t;
   }

   public static <T> ToIntFunction<T> createIndexLookup(List<T> p_467371_) {
      int i = p_467371_.size();
      if (i < 8) {
         return p_467371_::indexOf;
      } else {
         Object2IntMap<T> object2intmap = new Object2IntOpenHashMap(i);
         object2intmap.defaultReturnValue(-1);

         for (int j = 0; j < i; j++) {
            object2intmap.put(p_467371_.get(j), j);
         }

         return object2intmap;
      }
   }

   public static <T> ToIntFunction<T> createIndexIdentityLookup(List<T> p_468088_) {
      int i = p_468088_.size();
      if (i < 8) {
         ReferenceList<T> referencelist = new ReferenceImmutableList(p_468088_);
         return referencelist::indexOf;
      } else {
         Reference2IntMap<T> reference2intmap = new Reference2IntOpenHashMap(i);
         reference2intmap.defaultReturnValue(-1);

         for (int j = 0; j < i; j++) {
            reference2intmap.put(p_468088_.get(j), j);
         }

         return reference2intmap;
      }
   }

   public static <A, B> Typed<B> writeAndReadTypedOrThrow(Typed<A> p_467644_, Type<B> p_469311_, UnaryOperator<Dynamic<?>> p_469692_) {
      Dynamic<?> dynamic = (Dynamic<?>)p_467644_.write().getOrThrow();
      return readTypedOrThrow(p_469311_, p_469692_.apply(dynamic), true);
   }

   public static <T> Typed<T> readTypedOrThrow(Type<T> p_467402_, Dynamic<?> p_467416_) {
      return readTypedOrThrow(p_467402_, p_467416_, false);
   }

   public static <T> Typed<T> readTypedOrThrow(Type<T> p_467556_, Dynamic<?> p_468226_, boolean p_469309_) {
      DataResult<Typed<T>> dataresult = p_467556_.readTyped(p_468226_).map(Pair::getFirst);

      try {
         return p_469309_ ? (Typed)dataresult.getPartialOrThrow(IllegalStateException::new) : (Typed)dataresult.getOrThrow(IllegalStateException::new);
      } catch (IllegalStateException var7) {
         CrashReport crashreport = CrashReport.forThrowable(var7, "Reading type");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Info");
         crashreportcategory.setDetail("Data", p_468226_);
         crashreportcategory.setDetail("Type", p_467556_);
         throw new ReportedException(crashreport);
      }
   }

   public static <T> List<T> copyAndAdd(List<T> p_468215_, T p_468259_) {
      return ImmutableList.builderWithExpectedSize(p_468215_.size() + 1).addAll(p_468215_).add(p_468259_).build();
   }

   public static <T> List<T> copyAndAdd(T p_468486_, List<T> p_467587_) {
      return ImmutableList.builderWithExpectedSize(p_467587_.size() + 1).add(p_468486_).addAll(p_467587_).build();
   }

   public static <K, V> Map<K, V> copyAndPut(Map<K, V> p_468128_, K p_468415_, V p_467119_) {
      return ImmutableMap.builderWithExpectedSize(p_468128_.size() + 1).putAll(p_468128_).put(p_468415_, p_467119_).buildKeepingLast();
   }

   public static enum OS {
      LINUX("linux"),
      SOLARIS("solaris"),
      WINDOWS("windows") {
         @Override
         protected String[] getOpenUriArguments(URI p_468007_) {
            return new String[]{"rundll32", "url.dll,FileProtocolHandler", p_468007_.toString()};
         }
      },
      OSX("mac") {
         @Override
         protected String[] getOpenUriArguments(URI p_466834_) {
            return new String[]{"open", p_466834_.toString()};
         }
      },
      UNKNOWN("unknown");

      private final String telemetryName;

      private OS(String p_468003_) {
         this.telemetryName = p_468003_;
      }

      public void openUri(URI p_467687_) {
         try {
            Process process = Runtime.getRuntime().exec(this.getOpenUriArguments(p_467687_));
            process.getInputStream().close();
            process.getErrorStream().close();
            process.getOutputStream().close();
         } catch (IOException var3) {
            Util.LOGGER.error("Couldn't open location '{}'", p_467687_, var3);
         }
      }

      public void openFile(File p_467368_) {
         this.openUri(p_467368_.toURI());
      }

      public void openPath(Path p_469273_) {
         this.openUri(p_469273_.toUri());
      }

      protected String[] getOpenUriArguments(URI p_468392_) {
         String s = p_468392_.toString();
         if ("file".equals(p_468392_.getScheme())) {
            s = s.replace("file:", "file://");
         }

         return new String[]{"xdg-open", s};
      }

      public void openUri(String p_468008_) {
         try {
            this.openUri(new URI(p_468008_));
         } catch (URISyntaxException | IllegalArgumentException var3) {
            Util.LOGGER.error("Couldn't open uri '{}'", p_468008_, var3);
         }
      }

      public String telemetryName() {
         return this.telemetryName;
      }
   }
}
