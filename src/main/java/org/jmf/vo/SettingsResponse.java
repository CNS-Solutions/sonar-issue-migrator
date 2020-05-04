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
