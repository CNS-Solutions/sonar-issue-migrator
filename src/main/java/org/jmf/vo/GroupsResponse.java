package org.jmf.vo;

import java.util.List;

public class GroupsResponse extends PagingResponse {

   private List<Group> groups;

   public List<Group> getGroups() {
      return this.groups;
   }

   public void setGroups(final List<Group> groups) {
      this.groups = groups;
   }

}
