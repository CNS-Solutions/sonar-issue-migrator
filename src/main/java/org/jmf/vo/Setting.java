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

import java.util.Map;
import java.util.Set;

public class Setting {

   private String key;

   private String value;

   private Set<String> values;

   private Set<Map<String, String>> fieldValues;

   private boolean inherited;

   public String getKey() {
      return this.key;
   }

   public void setKey(final String key) {
      this.key = key;
   }

   public String getValue() {
      return this.value;
   }

   public void setValue(final String value) {
      this.value = value;
   }

   public Set<String> getValues() {
      return this.values;
   }

   public void setValues(final Set<String> values) {
      this.values = values;
   }

   public Set<Map<String, String>> getFieldValues() {
      return this.fieldValues;
   }

   public void setFieldValues(final Set<Map<String, String>> fieldValues) {
      this.fieldValues = fieldValues;
   }

   public boolean isInherited() {
      return this.inherited;
   }

   public void setInherited(final boolean inherited) {
      this.inherited = inherited;
   }

}
