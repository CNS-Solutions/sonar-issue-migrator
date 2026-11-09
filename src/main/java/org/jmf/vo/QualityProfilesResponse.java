package org.jmf.vo;

import java.util.List;

public class QualityProfilesResponse {

   private List<QualityProfile> profiles;

   public List<QualityProfile> getProfiles() {
      return this.profiles;
   }

   public void setProfiles(final List<QualityProfile> profiles) {
      this.profiles = profiles;
   }
}
