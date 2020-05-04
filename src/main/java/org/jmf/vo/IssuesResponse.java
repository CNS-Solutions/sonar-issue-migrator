package org.jmf.vo;

import java.util.List;

public final class IssuesResponse {
    
    private Boolean maxResultsReached;
    
    private Paging paging; 
    
    private List<Issue> issues;
    
    private List<Rule> rules;


    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    public Paging getPaging() {
        return paging;
    }

    public void setPaging(Paging paging) {
        this.paging = paging;
    }

    public Boolean getMaxResultsReached() {
        return maxResultsReached;
    }

    public List<Issue> getIssues() {
        return issues;
    }

    public void setMaxResultsReached(Boolean maxResultsReached) {
        this.maxResultsReached = maxResultsReached;
    }

    public void setIssues(List<Issue> issues) {
        this.issues = issues;
    }
    

}
