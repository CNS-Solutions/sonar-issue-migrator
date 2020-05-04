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
package org.jmf.client;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.jmf.services.SonarClientService;
import org.jmf.vo.Issue;
import org.jmf.vo.QualityProfile;
import org.jmf.vo.Setting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class.
 *
 * @author MVlcek
 * @version 29.04.2020
 */
public final class CommandLineClient {

   static {
      CommandLineClient.initializeLogging();
   }

   // Logger
   private static final Logger LOG = LoggerFactory.getLogger(CommandLineClient.class);

   private static final Options OPTIONS = CommandLineClient.createOptions();

   private CommandLineClient() {
      // never instantiated
   }

   /**
    * Main.
    *
    * @param args see {@link #createOptions()}
    */
   public static void main(final String... args) {
      try {
         final CommandLineParser clp = new DefaultParser();
         final CommandLine cl = clp.parse(CommandLineClient.OPTIONS, args);

         final String sourceUrl = cl.getOptionValue("su");
         final String sourceComponentKey = cl.getOptionValue("sc");
         final String sourceLogin = cl.getOptionValue("sl");
         final String sourcePassword = cl.getOptionValue("sp");

         final String targetUrl = Optional.ofNullable(cl.getOptionValue("tu")).orElse(sourceUrl);
         final String targetComponentKey = Optional.ofNullable(cl.getOptionValue("tc")).orElse(sourceComponentKey);
         final String targetLogin = Optional.ofNullable(cl.getOptionValue("tl")).orElse(sourceLogin);
         final String targetPassword = Optional.ofNullable(cl.getOptionValue("tp")).orElse(sourcePassword);

         if (StringUtils.isBlank(sourceUrl) || StringUtils.isBlank(sourceComponentKey) || StringUtils.isBlank(targetUrl) || StringUtils.isBlank(targetComponentKey)) {
            CommandLineClient.LOG.error("Missing source SonarQube URL or source component key");
            CommandLineClient.help();
         } else if (sourceUrl.equals(targetUrl) && sourceComponentKey.equals(targetComponentKey)) {
            CommandLineClient.LOG.error("Invalid target. It must be different than the source.");
            CommandLineClient.help();
         }

         final boolean dryRun = cl.hasOption("d");
         final boolean migrateProject = cl.hasOption("mp");

         if (migrateProject) {
            SonarClientService service = new SonarClientService(sourceUrl, sourceLogin, sourcePassword, true);
            final List<Setting> sourceSettings = service.getSettings(sourceComponentKey);
            final List<QualityProfile> sourceProfilies = service.getQualityProfiles(sourceComponentKey);

            service = new SonarClientService(targetUrl, targetLogin, targetPassword, dryRun);
            service.updateSettings(targetComponentKey, sourceSettings, sourceProfilies);
            return;
         }

         boolean migrateConfirmed = cl.hasOption("mc");
         boolean migrateFalsePositive = cl.hasOption("mf");
         boolean migrateWontFix = cl.hasOption("mw");
         boolean migrateComments = cl.hasOption("mo");

         if (!migrateConfirmed && !migrateFalsePositive && !migrateWontFix && !migrateComments) {
            CommandLineClient.LOG.info("No migration options given. Enabling all options.");
            migrateConfirmed = true;
            migrateFalsePositive = true;
            migrateWontFix = true;
            migrateComments = true;
         } else if (!migrateConfirmed && !migrateFalsePositive && !migrateWontFix) {
            CommandLineClient.LOG.error("Invalid migration options: one of confirmed, false-positives or wont-fix must be given.");
            CommandLineClient.help();
         }

         final int lineDelta = Optional.ofNullable(cl.getOptionValue("dl")).map(Integer::valueOf).orElse(0);

         SonarClientService service = new SonarClientService(sourceUrl, sourceLogin, sourcePassword, true);

         final List<Issue> sourceIssues = new ArrayList<>();
         if (migrateConfirmed) {
            sourceIssues.addAll(service.getIssuesInStatus(sourceComponentKey, SonarClientService.STATUS_CONFIRMED));
         }
         final Set<String> resolutions = new HashSet<>();
         if (migrateFalsePositive) {
            resolutions.add(SonarClientService.RESOLUTION_FALSE_POSITIVE);
         }
         if (migrateWontFix) {
            resolutions.add(SonarClientService.RESOLUTION_WONT_FIX);
         }
         if (!resolutions.isEmpty()) {
            sourceIssues.addAll(service.getIssuesInStatus(sourceComponentKey, SonarClientService.STATUS_RESOLVED, resolutions.toArray(new String[resolutions.size()])));
         }

         service = new SonarClientService(targetUrl, targetLogin, targetPassword, dryRun);

         service.updateIssues(targetComponentKey, sourceIssues, lineDelta, migrateConfirmed, migrateFalsePositive, migrateWontFix, migrateComments);
      } catch (final ParseException e) {
         CommandLineClient.LOG.error(e.getMessage(), e);
         CommandLineClient.help();
      } catch (final Exception e) {
         CommandLineClient.LOG.error("Error migrating sonar issues: {}", e.getMessage(), e);
      } finally {
         LogManager.shutdown();
      }

   }

   private static void help() {
      final StringWriter stringWriter = new StringWriter();
      final PrintWriter printWriter = new PrintWriter(stringWriter);
      final HelpFormatter helpFormatter = new HelpFormatter();
      final String jarName = Optional.ofNullable(new File(CommandLineClient.class.getProtectionDomain().getCodeSource().getLocation().getPath()))
            .map(File::getName)
            .filter(name -> name.endsWith(".jar"))
            .orElse("sims.jar");

      helpFormatter.printHelp(
            printWriter,
            120,
            "java -jar " + jarName,
            "\nA tool for migrating SonarQube issue resolutions and comments from one project to another."
                  + "\n\nOptions:",
            CommandLineClient.OPTIONS, 2, 3,
            "\nIf none of the migration options are given, all are enabled.\n"
                  + "The projects need to be identical or at least very similar to map the issues.\n"
                  + "\nExamples:\n"
                  + "> java -jar " + jarName + " -su https://sonar.test.com -sc com.test:prj1 -tc com.test:prj1-branch -tl 21..."
                  + "\tMigrates resolutions and comments to a branch.\n"
                  + "> java -jar " + jarName + " -su https://sonar1.test.com -sc com.test:prj1 -tu https://sonar2.test.com -tl 21..."
                  + "\tMigrates resolutions and comments from one server to another one.\n",
            true);
      CommandLineClient.LOG.info("{}", stringWriter.getBuffer().toString());
   }

   private static Options createOptions() {
      final Options options = new Options();

      options.addOption("h", "help", false, "print this help");
      options.addOption(Option.builder("su")
            .longOpt("source-url")
            .hasArgs()
            .required()
            .argName("url")
            .desc("URL of source SonarQube")
            .build());
      options.addOption(Option.builder("sc")
            .longOpt("source-component")
            .hasArgs()
            .required()
            .argName("key")
            .desc("Source component key, e.g. project key")
            .build());
      options.addOption(Option.builder("sl")
            .longOpt("source-login")
            .hasArgs()
            .argName("user-or-token")
            .desc("Login user name or token for source")
            .build());
      options.addOption(Option.builder("sp")
            .longOpt("source-password")
            .hasArgs()
            .argName("password")
            .desc("Password for source, if login user name is given")
            .build());
      options.addOption(Option.builder("tu")
            .longOpt("target-url")
            .hasArgs()
            .argName("url")
            .desc("URL of target SonarQube - if not set, the source URL is used")
            .build());
      options.addOption(Option.builder("tc")
            .longOpt("target-component")
            .hasArgs()
            .argName("key")
            .desc("Target component key - if not set the source comonent key is used")
            .build());
      options.addOption(Option.builder("tl")
            .longOpt("target-login")
            .hasArgs()
            .argName("user-or-token")
            .desc("Login user name or token for target - if not set the source login is used")
            .build());
      options.addOption(Option.builder("tp")
            .longOpt("target-password")
            .hasArgs()
            .argName("password")
            .desc("Password for target, if login user  name is given - if not set the source password is used")
            .build());
      options.addOption(Option.builder("dl")
            .longOpt("delta-line")
            .hasArg()
            .argName("delta")
            .desc("Maximum delta of line numbers (default 0)")
            .build());
      options.addOption(Option.builder("mp")
            .longOpt("migrate-project")
            .desc("Migrate project settings")
            .build());
      options.addOption(Option.builder("mc")
            .longOpt("migrate-confirmed")
            .desc("Migrate confirmed")
            .build());
      options.addOption(Option.builder("mf")
            .longOpt("migrate-false-positive")
            .desc("Migrate resolved/false-positive")
            .build());
      options.addOption(Option.builder("mw")
            .longOpt("migrate-wont-fix")
            .desc("Migrate resolved/won't fix")
            .build());
      options.addOption(Option.builder("mo")
            .longOpt("migrate-comments")
            .desc("Migrate comments")
            .build());
      options.addOption(Option.builder("d")
            .longOpt("dry-run")
            .desc("Run without actually updating anything")
            .build());

      return options;
   }

   private static void initializeLogging() {
      final ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
      builder
            .setConfigurationName("CLI")
            .setStatusLevel(Level.ERROR)
            .add(builder.newAppender("stdout", "Console")
                  .addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT)
                  .add(builder.newLayout("PatternLayout")
                        .addAttribute("pattern", "%msg%ex{0}%n"))
                  .add(builder.newFilter("LevelRangeFilter", Filter.Result.ACCEPT, Filter.Result.DENY)
                        .addAttribute("minLevel", "INFO")
                        .addAttribute("maxLevel", "INFO")))
            .add(builder.newAppender("stderr", "Console")
                  .addAttribute("target", ConsoleAppender.Target.SYSTEM_ERR)
                  .add(builder.newLayout("PatternLayout")
                        .addAttribute("pattern", "%msg%ex{0}%n"))
                  .add(builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.DENY)
                        .addAttribute("level", "WARN")))
            .add(builder.newRootLogger(Level.TRACE)
                  .add(builder.newAppenderRef("stdout"))
                  .add(builder.newAppenderRef("stderr")));
      Configurator.initialize(builder.build());
   }

}
