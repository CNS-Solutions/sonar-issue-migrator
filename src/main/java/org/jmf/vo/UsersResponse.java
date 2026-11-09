package org.jmf.vo;

import java.util.List;

public class UsersResponse extends PagingResponse {

   private List<User> users;

   public List<User> getUsers() {
      return this.users;
   }

   public void setUsers(final List<User> users) {
      this.users = users;
   }

}
