package org.jmf.vo;

import java.util.List;

public class ComponentsResponse extends PagingResponse {

   private List<Component> components;

   public List<Component> getComponents() {
      return this.components;
   }

   public void setComponents(final List<Component> components) {
      this.components = components;
   }

}
