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

/**
 * Represents a SonarQube issue.
 *
 * @author jose
 */
public final class Issue {

   private String key;

   private String component;

   private String rule;

   private String status;

   private String resolution;

   private String severity;

   private Integer line;

   private String actionPlan;

   private String assignee;

   private List<Comment> comments;

   public String getActionPlan() {
      return this.actionPlan;
   }

   public List<Comment> getComments() {
      return this.comments;
   }

   public void setComments(final List<Comment> comments) {
      this.comments = comments;
   }

   public void setActionPlan(final String actionPlan) {
      this.actionPlan = actionPlan;
   }

   public String getKey() {
      return this.key;
   }

   public String getComponent() {
      return this.component;
   }

   public String getRule() {
      return this.rule;
   }

   public String getStatus() {
      return this.status;
   }

   public String getSeverity() {
      return this.severity;
   }

   public Integer getLine() {
      return this.line;
   }

   public void setKey(final String key) {
      this.key = key;
   }

   public void setComponent(final String component) {
      this.component = component;
   }

   public void setRule(final String rule) {
      this.rule = rule;
   }

   public void setStatus(final String status) {
      this.status = status;
   }

   public void setSeverity(final String severity) {
      this.severity = severity;
   }

   public void setLine(final Integer line) {
      this.line = line;
   }

   public String getParsedComponent() {
      return this.component == null ? "" : this.component.replaceAll(".*:", "");
   }

   public String getResolution() {
      return this.resolution;
   }

   public void setResolution(final String resolution) {
      this.resolution = resolution;
   }

   public String getAssignee() {
      return this.assignee;
   }

   public void setAssignee(final String assignee) {
      this.assignee = assignee;
   }

}
