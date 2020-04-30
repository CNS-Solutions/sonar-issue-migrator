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
package org.jmf.services;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.jmf.vo.Comment;
import org.jmf.vo.Issue;
import org.jmf.vo.JsonIssue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for Sonar web service API.
 *
 * @author jose
 * @author mvlcek
 */
public class SonarClientService {

   /** open status */
   public static final String STATUS_OPEN = "OPEN";

   /** confirmed status */
   public static final String STATUS_CONFIRMED = "CONFIRMED";

   /** reopened status */
   public static final String STATUS_REOPENED = "REOPENED";

   /** resolved status */
   public static final String STATUS_RESOLVED = "RESOLVED";

   /** resolution false positive */
   public static final String RESOLUTION_FALSE_POSITIVE = "FALSE-POSITIVE";

   /** resolution won't fix */
   public static final String RESOLUTION_WONT_FIX = "WONTFIX";

   private static final Logger LOG = LoggerFactory.getLogger(SonarClientService.class);

   private static final String API_SEARCH = "api/issues/search";

   private static final String API_DO_TRANSITION = "api/issues/do_transition";

   private static final String API_ADD_COMMENT = "api/issues/add_comment";

   private static final String API_ASSIGN = "api/issues/assign";

   private static final String PARAM_ISSUE = "issue";

   private static final String PARAM_TRANSITION = "transition";

   private static final String PARAM_TEXT = "text";

   private static final String PARAM_COMPONENT_KEYS = "componentKeys";

   private static final String PARAM_STATUSES = "statuses";

   private static final String PARAM_RESOLUTIONS = "resolutions";

   private static final String PARAM_RULES = "rules";

   private static final String PARAM_ASSIGNEE = "assignee";

   private static final String PARAM_PAGE_INDEX = "pageIndex";

   private static final String PARAM_ADDITIONAL_FIELDS = "additionalFields";

   private static final String TRANSITION_CONFIRM = "confirm";

   private static final String TRANSITION_FALSE_POSITIVE = "falsepositive";

   private static final String TRANSITION_WONT_FIX = "wontfix";

   private static final String FIELD_COMMENTS = "comments";

   private final String baseUrl;

   private final String login;

   private final String password;

   private final boolean readonly;

   private final ObjectMapper mapper;

   /**
    * Constructor.
    *
    * @param baseUrl the base URL, e.g. http://localhost:9000
    * @param login the user name or token
    * @param password the password or empty for a token
    * @param readonly do not actually do any changes
    */
   public SonarClientService(final String baseUrl, final String login, final String password, final boolean readonly) {
      this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
      this.login = login;
      this.password = password;
      this.readonly = readonly;
      this.mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

   }

   /**
    * Update project's issues based on flagged issues list.
    *
    * @param componentKey the component key, e.g. project key
    * @param sourceIssues List of source issues
    * @param deltaLines maximum delta of line numbers to successfully match an issue
    * @param migrateConfirmed if open issues should be confirmed, if the source issue is confirmed
    * @param migrateFalsePositives if unresolved issues should be resolved as false positive, if the source issue is a false positive
    * @param migrateWontFixes if unresolved issues should be resolved as wontfix, if the source issue is a wontfix
    * @param addComments if comments should be migrated, too
    */
   public final void updateIssues(final String componentKey, final List<Issue> sourceIssues, final int deltaLines,
         final boolean migrateConfirmed, final boolean migrateFalsePositives, final boolean migrateWontFixes, final boolean addComments) {
      final Map<String, List<Issue>> issuesByRuleMap = new HashMap<>();

      final int total = sourceIssues.size();
      int processed = 0;
      int updated = 0;
      int unmatched = 0;

      SonarClientService.LOG.info("Processing {} issues...", total);
      try (CloseableHttpClient client = this.createHttpClient(true)) {
         for (final Issue sourceIssue : sourceIssues) {
            final String rule = sourceIssue.getRule();
            final List<Issue> ruleIssues = issuesByRuleMap.computeIfAbsent(rule, r -> this.getIssuesForRule(componentKey, r));
            final Issue targetIssue = ruleIssues.stream()
                  .filter(issue -> issue.getParsedComponent().equals(sourceIssue.getParsedComponent()))
                  .filter(issue -> Math.abs(issue.getLine() - sourceIssue.getLine()) <= deltaLines)
                  .sorted((issue1, issue2) -> Math.abs(issue1.getLine() - sourceIssue.getLine()) - Math.abs(issue2.getLine() - sourceIssue.getLine()))
                  .findFirst()
                  .orElse(null);

            if (targetIssue != null) {
               ruleIssues.remove(targetIssue);

               boolean changed = false;
               String transition = null;

               if (SonarClientService.STATUS_OPEN.equals(targetIssue.getStatus())) {
                  if (migrateConfirmed && SonarClientService.STATUS_CONFIRMED.equals(sourceIssue.getStatus())) {
                     transition = SonarClientService.TRANSITION_CONFIRM;
                  }
               }
               if (!SonarClientService.STATUS_RESOLVED.equals(targetIssue.getStatus())) {
                  if (migrateFalsePositives && SonarClientService.RESOLUTION_FALSE_POSITIVE.equals(sourceIssue.getResolution())) {
                     transition = SonarClientService.TRANSITION_FALSE_POSITIVE;
                  } else if (migrateWontFixes && SonarClientService.RESOLUTION_WONT_FIX.equals(sourceIssue.getResolution())) {
                     transition = SonarClientService.TRANSITION_WONT_FIX;
                  }
               }
               if (transition != null) {
                  if (this.doTransition(client, targetIssue, transition)) {
                     changed = true;
                  }
               }

               if (sourceIssue.getComments() != null && targetIssue.getComments() != null) {
                  for (final Comment comment : sourceIssue.getComments()) {

                     final boolean hasComment = targetIssue.getComments().stream()
                           .anyMatch(c -> c.getMarkdown() != null && c.getMarkdown().equals(comment.getMarkdown()));

                     if (!hasComment) {
                        if (this.addComment(client, targetIssue, comment.getMarkdown())) {
                           changed = true;
                        }
                     }
                  }
               }

               if (changed) {
                  updated++;
               }

            } else {
               unmatched++;
               SonarClientService.LOG.warn("Could not find match for {}/{}", sourceIssue.getParsedComponent(), sourceIssue.getLine());
            }
            processed++;
            SonarClientService.LOG.info("Processed {} and updated {} of {} issues", processed, updated, total);
         }
         SonarClientService.LOG.info("Processed {} issues: {} updated, {} unmatched.", processed, updated, unmatched);
      } catch (final Exception e) {
         SonarClientService.LOG.error("Error updating issues", e);
      }
   }

   private boolean doTransition(final CloseableHttpClient client, final Issue issue, final String transition) {
      if (this.readonly) {
         SonarClientService.LOG.info("Issue {}/{} would be updated: {}", issue.getParsedComponent(), issue.getLine(), transition);
         return true;
      }
      try {
         final StatusLine statusLine = this.post(client, this.baseUrl + SonarClientService.API_DO_TRANSITION,
               new BasicNameValuePair(SonarClientService.PARAM_ISSUE, issue.getKey()),
               new BasicNameValuePair(SonarClientService.PARAM_TRANSITION, transition));
         if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
            SonarClientService.LOG.info("Issue {}/{} updated: {}", issue.getParsedComponent(), issue.getLine(), transition);
            return true;
         } else {
            SonarClientService.LOG.error("Error doing transition '{}' for issue {}/{}: {}", transition, issue.getParsedComponent(), issue.getLine(), statusLine);
         }
      } catch (final Exception e) {
         SonarClientService.LOG.error("Error doing transition '{}' for issue {}/{}: {}", transition, issue.getParsedComponent(), issue.getLine(), e.getMessage(), e);
      }
      return false;
   }

   private boolean addComment(final CloseableHttpClient client, final Issue issue, final String text) {
      if (this.readonly) {
         SonarClientService.LOG.info("Issue {}/{} would be updated with comment: '{}'", issue.getParsedComponent(), issue.getLine(), text);
         return true;
      }
      try {
         final StatusLine statusLine = this.post(client, this.baseUrl + SonarClientService.API_ADD_COMMENT,
               new BasicNameValuePair(SonarClientService.PARAM_ISSUE, issue.getKey()),
               new BasicNameValuePair(SonarClientService.PARAM_TEXT, text));
         if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
            SonarClientService.LOG.info("Issue {}/{} updated with comment: '{}'", issue.getParsedComponent(), issue.getLine(), text);
            return true;
         } else {
            SonarClientService.LOG.error("Error adding comment '{}' to issue {}/{}: {}", text, issue.getParsedComponent(), issue.getLine(), statusLine);
         }
      } catch (final Exception e) {
         SonarClientService.LOG.error("Error adding comment '{}' to issue {}/{}: {}", text, issue.getParsedComponent(), issue.getLine(), e.getMessage(), e);
      }
      return false;
   }

   /** not yet used */
   private boolean assign(final CloseableHttpClient client, final Issue issue, final String assignee) {
      if (this.readonly) {
         SonarClientService.LOG.info("Issue {}/{} would be assigned to {}", issue.getParsedComponent(), issue.getLine(), assignee);
         return true;
      }
      try {
         final StatusLine statusLine = this.post(client, this.baseUrl + SonarClientService.API_ASSIGN,
               new BasicNameValuePair(SonarClientService.PARAM_ISSUE, issue.getKey()),
               new BasicNameValuePair(SonarClientService.PARAM_ASSIGNEE, assignee));
         if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
            SonarClientService.LOG.info("Issue {}/{} assigned to {}", issue.getParsedComponent(), issue.getLine(), assignee);
            return true;
         } else {
            SonarClientService.LOG.error("Error assigning to {} issue {}/{}: {}", assignee, issue.getParsedComponent(), issue.getLine(), statusLine);
         }
      } catch (final Exception e) {
         SonarClientService.LOG.error("Error assigning to {} issue {}/{}: {}", assignee, issue.getParsedComponent(), issue.getLine(), e.getMessage(), e);
      }
      return false;
   }

   /**
    * Get list of issues for a given status.
    *
    * @param componentKey the component key, e.g. project key
    * @param status the status, e.g. CONFIRMED or RESOLVED
    * @param resolutions the resolutions, e.g. FALSE-POSITIVE or WONTFIX
    * @return the issues
    * @throws UnsupportedEncodingException never
    */
   public List<Issue> getIssuesInStatus(final String componentKey, final String status, final String... resolutions) {
      try {
         return this.getIssues(
               new BasicNameValuePair(SonarClientService.PARAM_COMPONENT_KEYS, componentKey),
               new BasicNameValuePair(SonarClientService.PARAM_STATUSES, status),
               new BasicNameValuePair(SonarClientService.PARAM_ADDITIONAL_FIELDS, SonarClientService.FIELD_COMMENTS),
               new BasicNameValuePair(SonarClientService.PARAM_RESOLUTIONS, Arrays.stream(resolutions).collect(Collectors.joining(","))));
      } catch (final Exception e) {
         return new ArrayList<>();
      }
   }

   /**
    * Get list issues for a given rule.
    *
    * @param componentKey the component key, e.g. project key
    * @param rule the rule key, e.g. java:S2384
    * @return the issues
    * @throws UnsupportedEncodingException
    */
   public List<Issue> getIssuesForRule(final String componentKey, final String rule) {
      try {
         return this.getIssues(
               new BasicNameValuePair(SonarClientService.PARAM_COMPONENT_KEYS, componentKey),
               new BasicNameValuePair(SonarClientService.PARAM_RULES, rule),
               new BasicNameValuePair(SonarClientService.PARAM_ADDITIONAL_FIELDS, SonarClientService.FIELD_COMMENTS));
      } catch (final Exception e) {
         return new ArrayList<>();
      }
   }

   private List<Issue> getIssues(final NameValuePair... parameters) {
      final List<Issue> issues = new ArrayList<>();

      Integer pageIndex = 0; // Current page
      JsonIssue obj = null;

      try (CloseableHttpClient client = this.createHttpClient(true)) {
         do {
            final String url = this.getUrl(this.baseUrl + SonarClientService.API_SEARCH,
                  this.addParameters(parameters, new BasicNameValuePair(SonarClientService.PARAM_PAGE_INDEX, String.valueOf(pageIndex + 1))));
            try {
               obj = this.get(client, url, JsonIssue.class);

               // Add list of issues extracted from current page
               issues.addAll(obj.getIssues());
               pageIndex = obj.getPaging().getPageIndex(); // Current page
            } catch (final Exception e) {
               SonarClientService.LOG.error("Error getting issues from URL {}.", url, e);
               break;
            }
         } while (issues.size() < obj.getPaging().getTotal());
      } catch (final Exception e) {
         final String url = this.getUrl(this.baseUrl + SonarClientService.API_SEARCH, parameters);
         SonarClientService.LOG.error("Error getting issues from URL {}: {}.", url, e.getMessage(), e);
      }

      return issues;
   }

   private CloseableHttpClient createHttpClient(final boolean trustAll) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
      if (trustAll) {
         // Accept ALL certificates
         final SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
            @Override
            public boolean isTrusted(final X509Certificate[] arg0, final String arg1) throws CertificateException {
               return true;
            }
         }).build();
         return HttpClients.custom().setSslcontext(sslContext).setSSLHostnameVerifier(new NoopHostnameVerifier()).build();
      } else {
         return HttpClients.createDefault();
      }
   }

   private <T> T get(final CloseableHttpClient client, final String url, final Class<T> clazz, final NameValuePair... parameters) throws IOException {
      final HttpGet request = new HttpGet(this.getUrl(url, parameters));
      this.getAuthenticationHeader().ifPresent(request::addHeader);

      try (CloseableHttpResponse response = client.execute(request)) {
         final String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
         return this.mapper.readValue(body, clazz);
      }
   }

   private StatusLine post(final CloseableHttpClient client, final String url, final NameValuePair... parameters) throws IOException {
      final HttpPost request = new HttpPost(url);
      request.setEntity(new UrlEncodedFormEntity(Arrays.asList(parameters), StandardCharsets.UTF_8));
      this.getAuthenticationHeader().ifPresent(request::addHeader);

      try (CloseableHttpResponse response = client.execute(request)) {
         return response.getStatusLine();
      }
   }

   private NameValuePair[] addParameters(final NameValuePair[] parameters1, final NameValuePair... parameters2) {
      final List<NameValuePair> parameters = new ArrayList<>();
      parameters.addAll(Arrays.asList(parameters1));
      parameters.addAll(Arrays.asList(parameters2));
      return parameters.toArray(new NameValuePair[parameters.size()]);
   }

   private String getUrl(final String url, final NameValuePair... parameters) {
      final StringBuilder sb = new StringBuilder(url);
      try {
         for (final NameValuePair parameter : parameters) {
            sb.append(sb.toString().contains("?") ? "&" : "?")
                  .append(parameter.getName()).append("=").append(URLEncoder.encode(parameter.getValue(), StandardCharsets.UTF_8.toString()));
         }
      } catch (final UnsupportedEncodingException e) {
         // ignore - should never happen
      }
      return sb.toString();
   }

   private Optional<Header> getAuthenticationHeader() {
      if (StringUtils.isNotBlank(this.login)) {
         final String value = this.login + ":" + (StringUtils.isNotBlank(this.password) ? this.password : "");
         return Optional.of(new BasicHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString((value).getBytes(StandardCharsets.UTF_8))));
      }
      return Optional.empty();
   }
}
