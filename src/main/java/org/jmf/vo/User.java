package org.jmf.vo;

import java.util.List;

public class User {

   private String login;

   private String name;

   private String email;

   private List<String> permissions;

   private boolean managed;

   public String getLogin() {
      return this.login;
   }

   public void setLogin(final String login) {
      this.login = login;
   }

   public String getName() {
      return this.name;
   }

   public void setName(final String name) {
      this.name = name;
   }

   public String getEmail() {
      return this.email;
   }

   public void setEmail(final String email) {
      this.email = email;
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
      return new StringBuilder("User ").append(this.name).toString();
   }
}
