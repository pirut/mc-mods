package com.mojang.realmsclient.client;

import com.mojang.realmsclient.exception.RealmsHttpException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class Request<T extends Request<T>> {
   protected HttpURLConnection connection;
   private boolean connected;
   protected String url;
   private static final int DEFAULT_READ_TIMEOUT = 60000;
   private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
   private static final String IS_SNAPSHOT_KEY = "Is-Prerelease";
   private static final String COOKIE_KEY = "Cookie";

   public Request(String p_87310_, int p_87311_, int p_87312_) {
      try {
         this.url = p_87310_;
         Proxy $$3 = RealmsClientConfig.getProxy();
         if ($$3 != null) {
            this.connection = (HttpURLConnection)new URL(p_87310_).openConnection($$3);
         } else {
            this.connection = (HttpURLConnection)new URL(p_87310_).openConnection();
         }

         this.connection.setConnectTimeout(p_87311_);
         this.connection.setReadTimeout(p_87312_);
      } catch (MalformedURLException var5) {
         throw new RealmsHttpException(var5.getMessage(), var5);
      } catch (IOException var6) {
         throw new RealmsHttpException(var6.getMessage(), var6);
      }
   }

   public void cookie(String p_87323_, String p_87324_) {
      cookie(this.connection, p_87323_, p_87324_);
   }

   public static void cookie(HttpURLConnection p_87336_, String p_87337_, String p_87338_) {
      String $$3 = p_87336_.getRequestProperty("Cookie");
      if ($$3 == null) {
         p_87336_.setRequestProperty("Cookie", p_87337_ + "=" + p_87338_);
      } else {
         p_87336_.setRequestProperty("Cookie", $$3 + ";" + p_87337_ + "=" + p_87338_);
      }
   }

   public void addSnapshotHeader(boolean p_305796_) {
      this.connection.addRequestProperty("Is-Prerelease", String.valueOf(p_305796_));
   }

   public int getRetryAfterHeader() {
      return getRetryAfterHeader(this.connection);
   }

   public static int getRetryAfterHeader(HttpURLConnection p_87331_) {
      String $$1 = p_87331_.getHeaderField("Retry-After");

      try {
         return Integer.valueOf($$1);
      } catch (Exception var3) {
         return 5;
      }
   }

   public int responseCode() {
      try {
         this.connect();
         return this.connection.getResponseCode();
      } catch (Exception var2) {
         throw new RealmsHttpException(var2.getMessage(), var2);
      }
   }

   public String text() {
      try {
         this.connect();
         String $$0;
         if (this.responseCode() >= 400) {
            $$0 = this.read(this.connection.getErrorStream());
         } else {
            $$0 = this.read(this.connection.getInputStream());
         }

         this.dispose();
         return $$0;
      } catch (IOException var2) {
         throw new RealmsHttpException(var2.getMessage(), var2);
      }
   }

   private String read(@Nullable InputStream p_87315_) throws IOException {
      if (p_87315_ == null) {
         return "";
      } else {
         InputStreamReader $$1 = new InputStreamReader(p_87315_, StandardCharsets.UTF_8);
         StringBuilder $$2 = new StringBuilder();

         for (int $$3 = $$1.read(); $$3 != -1; $$3 = $$1.read()) {
            $$2.append((char)$$3);
         }

         return $$2.toString();
      }
   }

   private void dispose() {
      byte[] $$0 = new byte[1024];

      try {
         InputStream $$1 = this.connection.getInputStream();

         while ($$1.read($$0) > 0) {
         }

         $$1.close();
         return;
      } catch (Exception var9) {
         try {
            InputStream $$3 = this.connection.getErrorStream();
            if ($$3 != null) {
               while ($$3.read($$0) > 0) {
               }

               $$3.close();
               return;
            }
         } catch (IOException var8) {
            return;
         }
      } finally {
         if (this.connection != null) {
            this.connection.disconnect();
         }
      }
   }

   protected T connect() {
      if (this.connected) {
         return (T)this;
      } else {
         T $$0 = this.doConnect();
         this.connected = true;
         return $$0;
      }
   }

   protected abstract T doConnect();

   public static Request<?> get(String p_87317_) {
      return new Request.Get(p_87317_, 5000, 60000);
   }

   public static Request<?> get(String p_87319_, int p_87320_, int p_87321_) {
      return new Request.Get(p_87319_, p_87320_, p_87321_);
   }

   public static Request<?> post(String p_87343_, String p_87344_) {
      return new Request.Post(p_87343_, p_87344_, 5000, 60000);
   }

   public static Request<?> post(String p_87326_, String p_87327_, int p_87328_, int p_87329_) {
      return new Request.Post(p_87326_, p_87327_, p_87328_, p_87329_);
   }

   public static Request<?> delete(String p_87341_) {
      return new Request.Delete(p_87341_, 5000, 60000);
   }

   public static Request<?> put(String p_87354_, String p_87355_) {
      return new Request.Put(p_87354_, p_87355_, 5000, 60000);
   }

   public static Request<?> put(String p_87346_, String p_87347_, int p_87348_, int p_87349_) {
      return new Request.Put(p_87346_, p_87347_, p_87348_, p_87349_);
   }

   public String getHeader(String p_87352_) {
      return getHeader(this.connection, p_87352_);
   }

   public static String getHeader(HttpURLConnection p_87333_, String p_87334_) {
      try {
         return p_87333_.getHeaderField(p_87334_);
      } catch (Exception var3) {
         return "";
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Delete extends Request<Request.Delete> {
      public Delete(String p_87359_, int p_87360_, int p_87361_) {
         super(p_87359_, p_87360_, p_87361_);
      }

      public Request.Delete doConnect() {
         try {
            this.connection.setDoOutput(true);
            this.connection.setRequestMethod("DELETE");
            this.connection.connect();
            return this;
         } catch (Exception var2) {
            throw new RealmsHttpException(var2.getMessage(), var2);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Get extends Request<Request.Get> {
      public Get(String p_87365_, int p_87366_, int p_87367_) {
         super(p_87365_, p_87366_, p_87367_);
      }

      public Request.Get doConnect() {
         try {
            this.connection.setDoInput(true);
            this.connection.setDoOutput(true);
            this.connection.setUseCaches(false);
            this.connection.setRequestMethod("GET");
            return this;
         } catch (Exception var2) {
            throw new RealmsHttpException(var2.getMessage(), var2);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Post extends Request<Request.Post> {
      private final String content;

      public Post(String p_87372_, String p_87373_, int p_87374_, int p_87375_) {
         super(p_87372_, p_87374_, p_87375_);
         this.content = p_87373_;
      }

      public Request.Post doConnect() {
         try {
            if (this.content != null) {
               this.connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            }

            this.connection.setDoInput(true);
            this.connection.setDoOutput(true);
            this.connection.setUseCaches(false);
            this.connection.setRequestMethod("POST");
            OutputStream $$0 = this.connection.getOutputStream();
            OutputStreamWriter $$1 = new OutputStreamWriter($$0, "UTF-8");
            $$1.write(this.content);
            $$1.close();
            $$0.flush();
            return this;
         } catch (Exception var3) {
            throw new RealmsHttpException(var3.getMessage(), var3);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Put extends Request<Request.Put> {
      private final String content;

      public Put(String p_87380_, String p_87381_, int p_87382_, int p_87383_) {
         super(p_87380_, p_87382_, p_87383_);
         this.content = p_87381_;
      }

      public Request.Put doConnect() {
         try {
            if (this.content != null) {
               this.connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            }

            this.connection.setDoOutput(true);
            this.connection.setDoInput(true);
            this.connection.setRequestMethod("PUT");
            OutputStream $$0 = this.connection.getOutputStream();
            OutputStreamWriter $$1 = new OutputStreamWriter($$0, "UTF-8");
            $$1.write(this.content);
            $$1.close();
            $$0.flush();
            return this;
         } catch (Exception var3) {
            throw new RealmsHttpException(var3.getMessage(), var3);
         }
      }
   }
}
