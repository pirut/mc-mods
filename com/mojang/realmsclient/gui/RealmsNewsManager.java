package com.mojang.realmsclient.gui;

import com.mojang.realmsclient.dto.RealmsNews;
import com.mojang.realmsclient.util.RealmsPersistence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsNewsManager {
   private final RealmsPersistence newsLocalStorage;
   private boolean hasUnreadNews;
   private String newsLink;

   public RealmsNewsManager(RealmsPersistence p_239304_) {
      this.newsLocalStorage = p_239304_;
      RealmsPersistence.RealmsPersistenceData $$1 = p_239304_.read();
      this.hasUnreadNews = $$1.hasUnreadNews;
      this.newsLink = $$1.newsLink;
   }

   public boolean hasUnreadNews() {
      return this.hasUnreadNews;
   }

   public String newsLink() {
      return this.newsLink;
   }

   public void updateUnreadNews(RealmsNews p_239191_) {
      RealmsPersistence.RealmsPersistenceData $$1 = this.updateNewsStorage(p_239191_);
      this.hasUnreadNews = $$1.hasUnreadNews;
      this.newsLink = $$1.newsLink;
   }

   private RealmsPersistence.RealmsPersistenceData updateNewsStorage(RealmsNews p_240153_) {
      RealmsPersistence.RealmsPersistenceData $$1 = new RealmsPersistence.RealmsPersistenceData();
      $$1.newsLink = p_240153_.newsLink;
      RealmsPersistence.RealmsPersistenceData $$2 = this.newsLocalStorage.read();
      boolean $$3 = $$1.newsLink == null || $$1.newsLink.equals($$2.newsLink);
      if ($$3) {
         return $$2;
      } else {
         $$1.hasUnreadNews = true;
         this.newsLocalStorage.save($$1);
         return $$1;
      }
   }
}
