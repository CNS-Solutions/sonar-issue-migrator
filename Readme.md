# Introduction

Command-line tool to copy
* project settings 
* issue features (status, resolution) from one SonarQube project to another. 

Both projects (source and target) can be on **different** SonarQube servers.

Project settings currently covered:

- Assigned quality profiles
- General project settings

Issue features currently covered:

- Status: Confirmed
- Status: Resolved with Resolution: False Positive, Won't Fix
- Comments

**Scenario Example**:

A SonarQube instance with Project_A containing thousands of issues. 

Some of them have been manually flagged to status "Confirmed", "False Positive" or "Won't Fix". For some of those comments were added.

At some point, Project_A is forked/branched and a new analysis is run. Analysis results are stored in a new branch (let's call it "Production") 
on the same (or a different) SonarQube server.
 
So, now we have:

- Project_A : Mix of "Open", "Confirmed", "False Positive" and "Won't Fix" issues.
- Project_A Production : Same list of issues as in Project_A, but all are "Open".
  
As a developer, I want to flag the issues in the 'Project_A Production' branch before starting to change code, so that the new analysis contains the same "flags" 
(Confirmed, False Positive, Won't Fix) including the comments as the original one.

That is what this tool is for. 

# Requirements

JRE 8

# Build

```sh
> mvn clean install
```

A all-in-one jar will be created in the target directory.

# Installation

1. None. Just run the all-in-one jar, e.g. 

```
> java -jar sonar-issue-migrator-standalone.jar -su ...
```

# Usage

Usage and options copied from the help:

```
usage: java -jar sonar-issue-migrator-standalone.jar [-d] [-dl <delta>] [-h] [-mc] [-mf] [-mo] [-mw] -sc <key> [-sl
       <user-or-token>] [-sp <password>] -su <url> [-tc <key>] [-tl <user-or-token>] [-tp <password>] [-tu <url>]

Options:
  -d,--dry-run                         Run without actually updating anything
  -dl,--delta-line <delta>             Maximum delta of line numbers (default 0)
  -h,--help                            print this help
  -mc,--migrate-confirmed              Migrate confirmed
  -mf,--migrate-false-positive         Migrate resolved/false-positive
  -mo,--migrate-comments               Migrate comments
  -mp,--migrate-project                Migrate project settings
  -mw,--migrate-wont-fix               Migrate resolved/won't fix
  -sc,--source-component <key>         Source component key, e.g. project key
  -sl,--source-login <user-or-token>   Login user name or token for source
  -sp,--source-password <password>     Password for source, if login user name is given
  -su,--source-url <url>               URL of source SonarQube
  -tc,--target-component <key>         Target component key - if not set the source comonent key is used
  -tl,--target-login <user-or-token>   Login user name or token for target - if not set the source login is used
  -tp,--target-password <password>     Password for target, if login user  name is given - if not set the source
                                       password is used
  -tu,--target-url <url>               URL of target SonarQube - if not set, the source URL is used
```

If none of the migration options are given, all issue related migration options are enabled.

The projects need to be identical or at least very similar to map the issues, as the matching of issues is by file name and line number.
If there are small changes between the projects, you might want to set a delta line number greater than 0.

Whenever possible, you should use a security token (which you can create from your SonarQube account page) instead of user name/password.
To update the project settings, you need the permission "Administer Quality Profiles", the edit right on the quality profiles and the administration right on the target project. 
To update the issue status, you need the permission "Administer Issues" in SonarQube for the target project.
The original creator of the comments will be lost, as all comments are marked as added by the user used for the migration.

# Examples

To copy a project from `com.test:prj1` to project/branch `com.test:prj1-branch` (the new project is created if necessary), use:

```
> java -jar sonar-issue-migrator-standalone.jar -su https://sonar.test.com -sc com.test:prj1 -tc com.test:prj1-branch -tl 21... -mp
```

To migrate the issue status from project `com.test:prj1` to project/branch `com.test:prj1-branch`, use:

```
> java -jar sonar-issue-migrator-standalone.jar -su https://sonar.test.com -sc com.test:prj1 -tc com.test:prj1-branch -tl 21...
```

To migrate resolutions and comments from one server to another one, use:

```
> java -jar sonar-issue-migrator-standalone.jar -su https://sonar1.test.com -sc com.test:prj1 -tu https://sonar2.test.com -tl 21...
```

# Steps to copy a project

The following steps detail, how to copy a project `com.test:prj1` to a new project/branch `com.test:prj1-branch` with all settings and migrate the issue status of all manually confirmed/resolved issues along with the comments:

1. Copy the project settings using this tool with options `-su ... -sc com.test:prj1 -tc com.test:prj1-branch -tl ... -mp`
2. Adjust project settings not migrated with the tool, e.g. permissions, quality gates, etc.
3. Run the analysis, e.g. with Jenkins. Make sure to specify `-Dsonar.projectKey=com.test:prj1-branch -Dsonar.projectName=...` with a maven build, so that the analysis is done in the new project (you could also set the maven group ID and artifactID to the new values).
4. Check that the analysis was completed on the SonarQube server
5. Migrate the manual issue states using this tool with options `-su ... -sc com.test:prj1 -tc com.test:prj1-branch -tl ... -mc -mf -mw -mo`
6. Check the project on the SonarQube server: the issue states should be updated


