/*******************************************************************************
 ** COPYRIGHT: CNS-Solutions & Support GmbH
 **            Member of Frequentis Group
 **            Innovationsstrasse 1
 **            A-1100 Vienna
 **            AUSTRIA
 **            Tel. +43 1 81150-0
 ** LANGUAGE:  Java, J2SE JDK
 **
 ** The copyright to the computer program(s) herein is the property of
 ** CNS-Solutions & Support GmbH, Austria. The program(s) shall not be used
 ** and/or copied without the written permission of CNS-Solutions & Support GmbH.
 *******************************************************************************/
package org.jmf.vo;

public class QualityProfile {

   private String key;

   private String name;

   private String language;

   private String languageName;

   private boolean isInherited;

   private boolean isDefault;

   private boolean isBuiltIn;

   private int activeRuleCount;

   private int activeDeprecatedRuleCount;

   public String getKey() {
      return this.key;
   }

   public void setKey(final String key) {
      this.key = key;
   }

   public String getName() {
      return this.name;
   }

   public void setName(final String name) {
      this.name = name;
   }

   public String getLanguage() {
      return this.language;
   }

   public void setLanguage(final String language) {
      this.language = language;
   }

   public String getLanguageName() {
      return this.languageName;
   }

   public void setLanguageName(final String languageName) {
      this.languageName = languageName;
   }

   public boolean isInherited() {
      return this.isInherited;
   }

   public void setInherited(final boolean isInherited) {
      this.isInherited = isInherited;
   }

   public boolean isDefault() {
      return this.isDefault;
   }

   public void setDefault(final boolean isDefault) {
      this.isDefault = isDefault;
   }

   public boolean isBuiltIn() {
      return this.isBuiltIn;
   }

   public void setBuiltIn(final boolean isBuiltIn) {
      this.isBuiltIn = isBuiltIn;
   }

   public int getActiveRuleCount() {
      return this.activeRuleCount;
   }

   public void setActiveRuleCount(final int activeRuleCount) {
      this.activeRuleCount = activeRuleCount;
   }

   public int getActiveDeprecatedRuleCount() {
      return this.activeDeprecatedRuleCount;
   }

   public void setActiveDeprecatedRuleCount(final int activeDeprecatedRuleCount) {
      this.activeDeprecatedRuleCount = activeDeprecatedRuleCount;
   }

}
