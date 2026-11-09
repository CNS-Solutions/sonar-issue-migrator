package org.jmf.vo;

import java.util.List;

public final class IssuesResponse {

   private Paging paging;

   private List<Issue> issues;

   private List<Rule> rules;

   public List<Rule> getRules() {
      return this.rules;
   }

   public void setRules(final List<Rule> rules) {
      this.rules = rules;
   }

   public Paging getPaging() {
      return this.paging;
   }

   public void setPaging(final Paging paging) {
      this.paging = paging;
   }

   public List<Issue> getIssues() {
      return this.issues;
   }

   public void setIssues(final List<Issue> issues) {
      this.issues = issues;
   }

}
