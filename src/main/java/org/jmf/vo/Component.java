package org.jmf.vo;

public class Component {

   private String key;

   private String name;

   private String visibility;

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

   public String getVisibility() {
      return this.visibility;
   }

   public void setVisibility(final String visibility) {
      this.visibility = visibility;
   }

   @Override
   public String toString() {
      return new StringBuilder("Component ").append(this.key).toString();
   }
}
