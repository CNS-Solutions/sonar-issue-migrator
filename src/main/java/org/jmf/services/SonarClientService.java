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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
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
import org.jmf.vo.Component;
import org.jmf.vo.ComponentsResponse;
import org.jmf.vo.Group;
import org.jmf.vo.GroupsResponse;
import org.jmf.vo.Issue;
import org.jmf.vo.IssuesResponse;
import org.jmf.vo.PagingResponse;
import org.jmf.vo.QualityProfile;
import org.jmf.vo.QualityProfilesResponse;
import org.jmf.vo.Setting;
import org.jmf.vo.SettingsResponse;
import org.jmf.vo.User;
import org.jmf.vo.UsersResponse;
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

   /** permission to browse */
   public static final String PERMISSION_USER = "user";

   /** permission to see source code */
   public static final String PERMISSION_CODE_VIEWER = "codeviewer";

   /** permission to administer issues */
   public static final String PERMISSION_ISSUE_ADMIN = "issueadmin";

   /** permission to administer security hotspots */
   public static final String PERMISSION_SECURITY_HOTSPOT_ADMIN = "securityhotspotadmin";

   /** permission to administer architecture */
   public static final String PERMISSION_ARCHITECTURE_ADMIN = "architectureadmin";

   /** permission to administer project */
   public static final String PERMISSION_ADMIN = "admin";

   /** permission to scan project */
   public static final String PERMISSION_SCAN = "scan";

   private static final Logger LOG = LoggerFactory.getLogger(SonarClientService.class);

   private static final String API_SEARCH_PROJECTS = "api/components/search_projects"; // internal

   private static final String API_SEARCH_ISSUES = "api/issues/search";

   private static final String API_DO_TRANSITION = "api/issues/do_transition";

   private static final String API_ADD_COMMENT = "api/issues/add_comment";

   private static final String API_ASSIGN = "api/issues/assign";

   private static final String API_SETTINGS = "api/settings/values";

   private static final String API_SET = "api/settings/set";

   private static final String API_RESET = "api/settings/reset";

   private static final String API_CREATE_PROJECT = "api/projects/create";

   private static final String API_SEARCH_QUALITY_PROFILES = "api/qualityprofiles/search";

   private static final String API_ADD_PROJECT_TO_QUALITY_PROFILE = "api/qualityprofiles/add_project";

   private static final String API_USERS = "api/permissions/users"; // internal

   private static final String API_GROUPS = "api/permissions/groups"; // internal

   private static final String API_ADD_USER_PERMISSION = "api/permissions/add_user";

   private static final String API_ADD_GROUP_PERMISSION = "api/permissions/add_group";

   private static final String API_REMOVE_USER_PERMISSION = "api/permissions/remove_user";

   private static final String API_REMOVE_GROUP_PERMISSION = "api/permissions/remove_group";

   private static final String PARAM_ISSUE = "issue";

   private static final String PARAM_TRANSITION = "transition";

   private static final String PARAM_TEXT = "text";

   private static final String PARAM_COMPONENT_KEYS = "componentKeys";

   private static final String PARAM_STATUSES = "statuses";

   private static final String PARAM_RESOLUTIONS = "resolutions";

   private static final String PARAM_RULES = "rules";

   private static final String PARAM_ASSIGNEE = "assignee";

   private static final String PARAM_PAGE_INDEX = "p";

   private static final String PARAM_PAGE_SIZE = "ps";

   private static final String PARAM_ADDITIONAL_FIELDS = "additionalFields";

   private static final String PARAM_COMPONENT = "component";

   private static final String PARAM_KEY = "key";

   private static final String PARAM_KEYS = "keys";

   private static final String PARAM_VALUE = "value";

   private static final String PARAM_VALUES = "values";

   private static final String PARAM_FIELD_VALUES = "fieldValues";

   private static final String PARAM_PROJECT = "project";

   private static final String PARAM_NAME = "name";

   private static final String PARAM_QUALITY_PROFILE = "qualityProfile";

   private static final String PARAM_LANGUAGE = "language";

   private static final String PARAM_PROJECT_KEY = "projectKey";

   private static final String PARAM_LOGIN = "login";

   private static final String PARAM_PERMISSION = "permission";

   private static final String PARAM_GROUP_NAME = "groupName";

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
    * Get all projects visible to the user.
    *
    * @return the project keys
    */
   public List<String> getProjects() {
      return this.getWithPaging(SonarClientService.API_SEARCH_PROJECTS, List.of(),
            ComponentsResponse.class, ComponentsResponse::getComponents, Component::getKey);
   }

   /**
    * Get all users with permissions in a project.
    *
    * @param projectKey the project key
    * @param withPermissions only return users with at least one permission for the project
    * @return the users with permissions
    */
   public List<User> getUsers(final String projectKey, final boolean withPermissions) {
      return this.getWithPaging(SonarClientService.API_USERS, List.of(
            new BasicNameValuePair(SonarClientService.PARAM_PROJECT_KEY, projectKey)),
            UsersResponse.class, UsersResponse::getUsers, user -> !withPermissions || !user.getPermissions().isEmpty() ? user : null);
   }

   /**
    * Get all groups with permissions in a project.
    *
    * @param projectKey the project key
    * @param withPermissions only return groups with at least one permission for the project
    * @return the groups with permissions
    */
   public List<Group> getGroups(final String projectKey, final boolean withPermissions) {
      return this.getWithPaging(SonarClientService.API_GROUPS, List.of(
            new BasicNameValuePair(SonarClientService.PARAM_PROJECT_KEY, projectKey)),
            GroupsResponse.class, GroupsResponse::getGroups, group -> !withPermissions || !group.getPermissions().isEmpty() ? group : null);
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
   @SuppressWarnings({"checkstyle:parameternumber", "checkstyle:cyclomaticcomplexity"})
   public void updateIssues(final String componentKey, final List<Issue> sourceIssues, final int deltaLines,
         final boolean migrateConfirmed, final boolean migrateFalsePositives, final boolean migrateWontFixes, final boolean addComments) {
      final int total = sourceIssues.size();

      SonarClientService.LOG.info("=== Processing {} issues for project {} ===", total, componentKey);
      try (CloseableHttpClient client = this.createHttpClient(true)) {
         final Map<String, List<Issue>> issuesByRuleMap = new HashMap<>();
         int processed = 0;
         int updated = 0;
         int unmatched = 0;

         for (final Issue sourceIssue : sourceIssues) {
            final String rule = sourceIssue.getRule();
            final List<Issue> ruleIssues = issuesByRuleMap.computeIfAbsent(rule, r -> this.getIssuesForRule(componentKey, r));
            final Issue targetIssue = ruleIssues.stream()
                  .filter(issue -> issue.getLine() != null) // closed issues
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
               SonarClientService.LOG.warn("Could not find match for {}/{}", componentKey, sourceIssue.getLine());
            }
            processed++;
            SonarClientService.LOG.info("Processed {} and updated {} of {} issues", processed, updated, total);
         }
         SonarClientService.LOG.info("Processed {} issues of project {}: {} updated, {} unmatched.", processed, componentKey, updated, unmatched);
      } catch (final Exception e) {
         SonarClientService.LOG.error("Error updating issues: {}", e.getMessage(), e);
      }
   }

   /**
    * Update permissions of a project.
    *
    * @param projectKey the project key
    * @param groups the groups with permissions
    * @param users the users with permissions
    * @param deletePermissions true to delete permissions that are not in the corresponding groups/users
    */
   public void updatePermissions(final String projectKey, final List<Group> groups, final List<User> users, final boolean deletePermissions) {
      SonarClientService.LOG.info("=== Processing permissions for project {} ===", projectKey);
      try (CloseableHttpClient client = this.createHttpClient(true)) {
         final List<Group> targetGroups = this.getGroups(projectKey, false);
         for (final Group sourceGroup : groups) {
            targetGroups.stream().filter(g -> g.getName().equals(sourceGroup.getName())).findFirst().ifPresent(targetGroup -> {
               final List<String> targetPermissions = targetGroup.getPermissions();
               final List<String> permissionsToAdd = sourceGroup.getPermissions().stream().filter(Predicate.not(targetPermissions::contains)).toList();
               permissionsToAdd.forEach(permission -> this.updatePermission(client, projectKey, true, targetGroup.getName(), false, permission));

               if (deletePermissions) {
                  final List<String> permissionsToRemove = targetPermissions.stream().filter(Predicate.not(sourceGroup.getPermissions()::contains)).toList();
                  permissionsToRemove.forEach(permission -> this.updatePermission(client, projectKey, true, targetGroup.getName(), true, permission));
               }
            });
         }
         if (deletePermissions) {
            targetGroups.stream()
                  .filter(tg -> !tg.getPermissions().isEmpty())
                  .filter(tg -> groups.stream().noneMatch(g -> g.getName().equals(tg.getName())))
                  .forEach(tg -> {
                     tg.getPermissions().forEach(permission -> this.updatePermission(client, projectKey, true, tg.getName(), true, permission));
                  });
         }

         final List<User> targetUsers = this.getUsers(projectKey, false);
         for (final User sourceUser : users) {
            targetUsers.stream().filter(u -> u.getName().equals(sourceUser.getName())).findFirst().ifPresent(targetUser -> {
               final List<String> targetPermissions = targetUser.getPermissions();
               final List<String> permissionsToAdd = sourceUser.getPermissions().stream().filter(Predicate.not(targetPermissions::contains)).toList();
               permissionsToAdd.forEach(permission -> this.updatePermission(client, projectKey, false, targetUser.getLogin(), false, permission));

               if (deletePermissions) {
                  final List<String> permissionsToRemove = targetPermissions.stream().filter(Predicate.not(sourceUser.getPermissions()::contains)).toList();
                  permissionsToRemove.forEach(permission -> this.updatePermission(client, projectKey, false, targetUser.getLogin(), true, permission));
               }
            });
         }
         if (deletePermissions) {
            targetUsers.stream()
                  .filter(tu -> !tu.getPermissions().isEmpty())
                  .filter(tu -> users.stream().noneMatch(u -> u.getName().equals(tu.getName())))
                  .forEach(tu -> {
                     tu.getPermissions().forEach(permission -> this.updatePermission(client, projectKey, false, tu.getLogin(), true, permission));
                  });
         }

      } catch (final Exception e) {
         SonarClientService.LOG.error("Error updating permissions: {}", e.getMessage(), e);
      }
   }

   private boolean updatePermission(final CloseableHttpClient client, final String projectKey,
         final boolean isGroup, final String loginOrGroupName, final boolean isRemove, final String permission) {
      if (this.readonly) {
         SonarClientService.LOG.info("Permission {} {}/{} would be {}", isGroup ? "group" : "user", loginOrGroupName, permission, isRemove ? "removed" : "added");
         return true;
      }
      try {
         final String path;
         if (isGroup) {
            path = isRemove ? SonarClientService.API_REMOVE_GROUP_PERMISSION : SonarClientService.API_ADD_GROUP_PERMISSION;
         } else {
            path = isRemove ? SonarClientService.API_REMOVE_USER_PERMISSION : SonarClientService.API_ADD_USER_PERMISSION;
         }
         final StatusLine statusLine = this.post(client, this.baseUrl + path,
               new BasicNameValuePair(isGroup ? SonarClientService.PARAM_GROUP_NAME : SonarClientService.PARAM_LOGIN, loginOrGroupName),
               new BasicNameValuePair(SonarClientService.PARAM_PROJECT_KEY, projectKey),
               new BasicNameValuePair(SonarClientService.PARAM_PERMISSION, permission));
         if (statusLine.getStatusCode() == HttpStatus.SC_NO_CONTENT) {
            SonarClientService.LOG.info("Permission {} {}/{} {}", isGroup ? "group" : "user", loginOrGroupName, permission, isRemove ? "removed" : "added");
            return true;
         } else {
            SonarClientService.LOG.error("Error {} permission {} {}/{}", isRemove ? "removing" : "adding", isGroup ? "group" : "user", loginOrGroupName, permission);
         }
      } catch (final Exception e) {
         SonarClientService.LOG.error("Error {} permission {} {}/{}", isRemove ? "removing" : "adding", isGroup ? "group" : "user", loginOrGroupName, permission, e);
      }
      return false;
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

      try (CloseableHttpClient client = this.createHttpClient(true)) {
         Integer pageIndex = 0; // Current page
         IssuesResponse obj = null;

         do {
            final String url = this.getUrl(this.baseUrl + SonarClientService.API_SEARCH_ISSUES,
                  this.addParameters(parameters, new BasicNameValuePair(SonarClientService.PARAM_PAGE_INDEX, String.valueOf(pageIndex + 1))));
            try {
               obj = this.get(client, url, IssuesResponse.class);

               // Add list of issues extracted from current page
               issues.addAll(obj.getIssues());
               pageIndex = obj.getPaging().getPageIndex(); // Current page
            } catch (final Exception e) {
               SonarClientService.LOG.error("Error getting issues from URL {}.", url, e);
               break;
            }
         } while (issues.size() < obj.getPaging().getTotal());
      } catch (final Exception e) {
         final String url = this.getUrl(this.baseUrl + SonarClientService.API_SEARCH_ISSUES, parameters);
         SonarClientService.LOG.error("Error getting issues from URL {}: {}.", url, e.getMessage(), e);
      }

      return issues;
   }

   /**
    * Update the settings
    *
    * @param componentKey the project key
    * @param sourceSettings the settings of the source project
    * @param sourceProfiles the quality profiles of the source project
    */
   public void updateSettings(final String componentKey, final List<Setting> sourceSettings, final List<QualityProfile> sourceProfiles) {
      SonarClientService.LOG.info("=== Processing settings of project {} ===", componentKey);
      try (CloseableHttpClient client = this.createHttpClient(true)) {

         List<Setting> targetSettings = this.getSettings(componentKey);
         if (targetSettings == null) {
            this.createProject(client, componentKey);
            targetSettings = this.getSettings(componentKey);
         }
         final Map<String, Setting> targetSettingsByKey = targetSettings.stream()
               .collect(Collectors.toMap(Setting::getKey, Function.identity()));

         for (final Setting sourceSetting : sourceSettings) {
            final Setting targetSetting = targetSettingsByKey.remove(sourceSetting.getKey());
            if (targetSetting == null) {
               if (sourceSetting.getValue() != null) {
                  this.setSetting(client, componentKey, sourceSetting.getKey(), sourceSetting.getValue());
               } else if (sourceSetting.getValues() != null) {
                  this.setSetting(client, componentKey, sourceSetting.getKey(), sourceSetting.getValues());
               } else if (sourceSetting.getFieldValues() != null) {
                  this.setSetting(client, componentKey, sourceSetting.getKey(), sourceSetting.getFieldValues());
               }
            } else if (sourceSetting.getValue() != null && !sourceSetting.getValue().equals(targetSetting.getValue())) {
               SonarClientService.LOG.info("Changing setting {}: {} -> {}", sourceSetting.getKey(), targetSetting.getValue(), sourceSetting.getValue());
               this.setSetting(client, componentKey, sourceSetting.getKey(), sourceSetting.getValue());
            } else if (sourceSetting.getValues() != null && !sourceSetting.getValues().equals(targetSetting.getValues())) {
               SonarClientService.LOG.info("Changing setting {}: {} -> {}", sourceSetting.getKey(), targetSetting.getValues(), sourceSetting.getValues());
               this.setSetting(client, componentKey, sourceSetting.getKey(), sourceSetting.getValues());
            } else if (sourceSetting.getFieldValues() != null && !sourceSetting.getFieldValues().equals(targetSetting.getFieldValues())) {
               SonarClientService.LOG.info("Changing setting {}: {} -> {}", sourceSetting.getKey(), targetSetting.getFieldValues(), sourceSetting.getFieldValues());
               this.setSetting(client, componentKey, sourceSetting.getKey(), sourceSetting.getFieldValues());
            }
         }

         if (!targetSettingsByKey.isEmpty()) {
            final String[] keys = targetSettingsByKey.keySet().stream().toArray(String[]::new);
            this.resetSetting(client, componentKey, keys);
         }

         final List<QualityProfile> targetProfiles = this.getQualityProfiles(componentKey);
         final Map<String, QualityProfile> targetProfilesByLanguage = targetProfiles.stream()
               .collect(Collectors.toMap(QualityProfile::getLanguage, Function.identity()));

         for (final QualityProfile sourceProfile : sourceProfiles) {
            final QualityProfile targetProfile = targetProfilesByLanguage.get(sourceProfile.getLanguage());
            if (targetProfile == null || !sourceProfile.getName().equals(targetProfile.getName())) {
               this.setQualityProfile(client, componentKey, sourceProfile.getName(), sourceProfile.getLanguage());
            }
         }

         SonarClientService.LOG.info("Settings for {} updated successfully", componentKey);
      } catch (final Exception e) {
         SonarClientService.LOG.error("Error setting settings: {}", e.getMessage(), e);
      }
   }

   /**
    * Get the settings for a component.
    *
    * @param componentKey the key of the component
    * @return the settings (or null if nothing found)
    */
   public List<Setting> getSettings(final String componentKey) {
      final String url = this.getUrl(this.baseUrl + SonarClientService.API_SETTINGS,
            new BasicNameValuePair(SonarClientService.PARAM_COMPONENT, componentKey));
      try (CloseableHttpClient client = this.createHttpClient(true)) {
         final SettingsResponse obj = this.get(client, url, SettingsResponse.class);
         return obj.getSettings();
      } catch (final Exception e) {
         SonarClientService.LOG.error("Error getting settings from URL {}: {}.", url, e.getMessage(), e);
      }
      return null;
   }

   /** not yet used */
   private boolean createProject(final CloseableHttpClient client, final String componentKey) {
      if (this.readonly) {
         SonarClientService.LOG.info("Project {} would be created", componentKey);
         return true;
      }
      try {
         final StatusLine statusLine = this.post(client, this.baseUrl + SonarClientService.API_CREATE_PROJECT,
               new BasicNameValuePair(SonarClientService.PARAM_PROJECT, componentKey),
               new BasicNameValuePair(SonarClientService.PARAM_NAME, "Project " + componentKey));
         if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
            SonarClientService.LOG.info("Project {} created", componentKey);
            return true;
         } else {
            SonarClientService.LOG.error("Error creating project {}: {}", componentKey, statusLine);
         }
      } catch (final Exception e) {
         SonarClientService.LOG.error("Error creating project {}: {}", componentKey, e.getMessage(), e);
      }
      return false;
   }

   private boolean setSetting(final CloseableHttpClient client, final String componentKey, final String key, final Object value) {
      if (this.readonly) {
         SonarClientService.LOG.info("Setting {} would be updated to '{}'", key, value);
         return true;
      }
      try {
         StatusLine statusLine = null;
         if (value instanceof String) {
            statusLine = this.post(client, this.baseUrl + SonarClientService.API_SET,
                  new BasicNameValuePair(SonarClientService.PARAM_COMPONENT, componentKey),
                  new BasicNameValuePair(SonarClientService.PARAM_KEY, key),
                  new BasicNameValuePair(SonarClientService.PARAM_VALUE, value.toString()));
         } else if (value instanceof Collection<?> && !((Collection<?>) value).isEmpty()) {
            final List<?> values = new ArrayList<>((Collection<?>) value);
            final List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair(SonarClientService.PARAM_COMPONENT, componentKey));
            params.add(new BasicNameValuePair(SonarClientService.PARAM_KEY, key));
            if (values.get(0) instanceof String) {
               values.forEach(v -> params.add(new BasicNameValuePair(SonarClientService.PARAM_VALUES, v.toString())));
            } else {
               for (final Object v : values) {
                  params.add(new BasicNameValuePair(SonarClientService.PARAM_FIELD_VALUES, this.mapper.writeValueAsString(v)));
               }
            }
            statusLine = this.post(client, this.baseUrl + SonarClientService.API_SET, params.toArray(new NameValuePair[params.size()]));
         }
         if (statusLine != null && statusLine.getStatusCode() == HttpStatus.SC_NO_CONTENT) {
            SonarClientService.LOG.info("Setting {} updated to '{}'", key, value);
            return true;
         } else {
            SonarClientService.LOG.error("Error updating setting {} to '{}': {}", key, value, statusLine);
         }
      } catch (final Exception e) {
         SonarClientService.LOG.error("Error updating setting {} to '{}': {}", key, value, e.getMessage(), e);
      }
      return false;
   }

   private boolean resetSetting(final CloseableHttpClient client, final String componentKey, final String... keys) {
      if (this.readonly) {
         SonarClientService.LOG.info("Settings {} would be reset", String.join(", ", keys));
         return true;
      }
      try {
         final StatusLine statusLine = this.post(client, this.baseUrl + SonarClientService.API_RESET,
               new BasicNameValuePair(SonarClientService.PARAM_COMPONENT, componentKey),
               new BasicNameValuePair(SonarClientService.PARAM_KEYS, String.join(",", keys)));
         if (statusLine.getStatusCode() == HttpStatus.SC_NO_CONTENT) {
            SonarClientService.LOG.info("Settings {} reset", String.join(", ", keys));
            return true;
         } else {
            SonarClientService.LOG.error("Error resetting settings {}: {}", String.join(", ", keys), statusLine);
         }
      } catch (final Exception e) {
         SonarClientService.LOG.error("Error resetting settings {}: {}", String.join(", ", keys), e.getMessage(), e);
      }
      return false;
   }

   /**
    * Get the quality profiles for a project.
    *
    * @param componentKey the key of the project
    * @return the quality profiles (or null if the project does not exist)
    */
   public List<QualityProfile> getQualityProfiles(final String componentKey) {
      final String url = this.getUrl(this.baseUrl + SonarClientService.API_SEARCH_QUALITY_PROFILES,
            new BasicNameValuePair(SonarClientService.PARAM_PROJECT, componentKey));
      try (CloseableHttpClient client = this.createHttpClient(true)) {
         final QualityProfilesResponse obj = this.get(client, url, QualityProfilesResponse.class);
         return obj.getProfiles();
      } catch (final Exception e) {
         SonarClientService.LOG.error("Error getting quality profiles for project {}: {}.", componentKey, e.getMessage(), e);
      }
      return null;
   }

   private boolean setQualityProfile(final CloseableHttpClient client, final String componentKey, final String name, final String language) {
      if (this.readonly) {
         SonarClientService.LOG.info("Quality profile for language {} would be set to {}", language, name);
         return true;
      }
      try {
         final StatusLine statusLine = this.post(client, this.baseUrl + SonarClientService.API_ADD_PROJECT_TO_QUALITY_PROFILE,
               new BasicNameValuePair(SonarClientService.PARAM_PROJECT, componentKey),
               new BasicNameValuePair(SonarClientService.PARAM_LANGUAGE, language),
               new BasicNameValuePair(SonarClientService.PARAM_QUALITY_PROFILE, name));
         if (statusLine.getStatusCode() == HttpStatus.SC_NO_CONTENT) {
            SonarClientService.LOG.info("Quality profile for language {} set to {}", language, name);
            return true;
         } else {
            SonarClientService.LOG.error("Error setting quality profile {} for language {}: {}", name, language, statusLine);
         }
      } catch (final Exception e) {
         SonarClientService.LOG.error("Error setting quality profile {} for language {}: {}", name, language, e.getMessage(), e);
      }
      return false;

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

   private <I, R extends PagingResponse, T> List<I> getWithPaging(final String path, final List<NameValuePair> parameters, final Class<R> responseClass, final Function<R, List<T>> responseItemsGetter,
         final Function<T, I> resultItemGetter) {
      final List<I> result = new ArrayList<>();

      int pageIndex = 0; // Current page
      R obj = null;

      try (CloseableHttpClient client = this.createHttpClient(true)) {
         do {
            final String url = this.getUrl(this.baseUrl + path,
                  this.addParameters(parameters.toArray(NameValuePair[]::new), new BasicNameValuePair(SonarClientService.PARAM_PAGE_INDEX, String.valueOf(pageIndex + 1))));
            try {
               obj = this.get(client, url, responseClass);

               // Add list of issues extracted from current page
               result.addAll(responseItemsGetter.apply(obj).stream()
                     .map(resultItemGetter::apply)
                     .filter(Objects::nonNull)
                     .toList());
               pageIndex = obj.getPaging().getPageIndex(); // Current page
            } catch (final Exception e) {
               SonarClientService.LOG.error("Error getting issues from URL {}.", url, e);
               break;
            }
         } while (obj.getPaging().getPageIndex() * obj.getPaging().getPageSize() < obj.getPaging().getTotal());
      } catch (final Exception e) {
         final String url = this.getUrl(this.baseUrl + SonarClientService.API_SEARCH_ISSUES, parameters.toArray(NameValuePair[]::new));
         SonarClientService.LOG.error("Error getting issues from URL {}: {}.", url, e.getMessage(), e);
      }

      return result;
   }

}
