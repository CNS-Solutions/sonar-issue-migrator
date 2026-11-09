package org.jmf.vo;

import java.util.List;

public class Group {

   private String id;

   private String name;

   private List<String> permissions;

   private boolean managed;

   public String getId() {
      return this.id;
   }

   public void setId(final String id) {
      this.id = id;
   }

   public String getName() {
      return this.name;
   }

   public void setName(final String name) {
      this.name = name;
   }

   public List<String> getPermissions() {
      return this.permissions;
   }

   public void setPermissions(final List<String> permissions) {
      this.permissions = permissions;
   }

   public boolean isManaged() {
      return this.managed;
   }

   public void setManaged(final boolean managed) {
      this.managed = managed;
   }

   @Override
   public String toString() {
      return new StringBuilder("Group ").append(this.name).toString();
   }
}
