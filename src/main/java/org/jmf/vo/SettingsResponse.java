package org.jmf.vo;

import java.util.List;

public class SettingsResponse {

   private List<Setting> settings;

   public List<Setting> getSettings() {
      return this.settings;
   }

   public void setSettings(final List<Setting> settings) {
      this.settings = settings;
   }

}
