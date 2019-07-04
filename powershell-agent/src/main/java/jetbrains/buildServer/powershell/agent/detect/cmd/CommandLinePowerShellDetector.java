/*
 *  Copyright 2000 - 2017 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  See LICENSE in the project root for license information.
 */

package jetbrains.buildServer.powershell.agent.detect.cmd;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.powershell.agent.Loggers;
import jetbrains.buildServer.powershell.agent.detect.DetectionContext;
import jetbrains.buildServer.powershell.agent.detect.PowerShellDetector;
import jetbrains.buildServer.powershell.agent.detect.PowerShellInfo;
import jetbrains.buildServer.powershell.common.PowerShellBitness;
import jetbrains.buildServer.powershell.common.PowerShellConstants;
import jetbrains.buildServer.powershell.common.PowerShellEdition;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Detects PowerShell using command line and detection script
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class CommandLinePowerShellDetector implements PowerShellDetector {

  @NotNull
  private static final Logger LOG = Loggers.DETECTION_LOGGER;

  @NotNull
  private final BuildAgentConfiguration myConfiguration;

  @NotNull
  private final DetectionRunner myRunner;
  @NotNull
  private final DetectionPaths myDetectionPaths;

  private static final List<String> EXECUTABLES_WIN = Arrays.asList(
          "pwsh.exe",
          "powershell.exe"
  );

  private static final List<String> EXECUTABLES_NIX = Arrays.asList(
          "pwsh",
          "pwsh-preview"
  );

  private static final List<String> EXECUTABLES_NIX_LEGACY = Collections.singletonList(
          "powershell"
  );

  private static final String DETECTION_SCRIPT =
          "Write-Output " +
                  "$PSVersionTable.PSVersion.toString() " + // shell version
                  "$PSVersionTable.PSEdition.toString() " + // shell edition
                  "([IntPtr]::size -eq 8)";                 // shell bitness

  public CommandLinePowerShellDetector(@NotNull final BuildAgentConfiguration configuration,
                                       @NotNull final DetectionRunner runner,
                                       @NotNull final DetectionPaths detectionPaths) {
    myConfiguration = configuration;
    myRunner = runner;
    myDetectionPaths = detectionPaths;
  }

  @NotNull
  @Override
  public Map<String, PowerShellInfo> findShells(@NotNull DetectionContext detectionContext) {
    LOG.info("Detecting PowerShell using CommandLinePowerShellDetector");
    // group by home
    final Map<String, PowerShellInfo> shells = new HashMap<String, PowerShellInfo>();
    final List<String> pathsToCheck = myDetectionPaths.getPaths(detectionContext);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Will be detecting PowerShell in the following locations: [\n" + StringUtil.join(pathsToCheck, "\n") + "\n");
    }

    File script = null;
    try {
      script = prepareDetectionScript();
      if (script != null) {
        final String scriptPath = script.getAbsolutePath();
        if (LOG.isDebugEnabled()) {
          LOG.info("Detection script path is: " + scriptPath);
        }
        if (SystemInfo.isWindows) {
          doDetectionCycle(shells, pathsToCheck, EXECUTABLES_WIN, scriptPath);
        } else {
          // try release versions. powershell has already been renamed to pwsh.
          // pwsh-preview is used for -preview versions of powershell core
          LOG.debug("Detecting PowerShell.Core...");
          doDetectionCycle(shells, pathsToCheck, EXECUTABLES_NIX, scriptPath);
          // if no shells found - try legacy
          if (shells.isEmpty()) {
            LOG.debug("No release versions of PowerShell.Core were detected. Trying to detect legacy and beta versions...");
            doDetectionCycle(shells, pathsToCheck, EXECUTABLES_NIX_LEGACY, scriptPath);
          }
        }
      }
      if (shells.isEmpty()) {
        LOG.info("No PowerShell detected. If it is installed in non-standard location, " +
                "please provide install locations in teamcity.powershell.detector.search.paths " +
                "agent property (with ';' as a separator)");
      }
      return shells;
    } finally {
      if (script != null) {
        FileUtil.delete(script);
      }
    }
  }

  private void doDetectionCycle(Map<String, PowerShellInfo> shells, List<String> pathsToCheck, List<String> executablesToCheck, String scriptPath) {
    for (String path: pathsToCheck) {
      for (String executable: executablesToCheck) {
        final PowerShellInfo detected = doDetect(path, executable, scriptPath);
        if (detected != null) {
          shells.put(detected.getHome().getAbsolutePath(), detected);
        }
      }
    }
  }

  @Nullable
  private PowerShellInfo doDetect(@NotNull final String homePath,
                                  @NotNull final String executable,
                                  @NotNull final String scriptPath) {
    PowerShellInfo result = null;
    final File exeFile = new File(homePath, executable);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Searching for PowerShell at " + exeFile.getAbsolutePath());
    }
    if (exeFile.isFile()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Trying PowerShell executable: " + exeFile.getAbsolutePath());
      }
      String executablePath = exeFile.getAbsolutePath();
      try {
        final List<String> outputLines = myRunner.runDetectionScript(executablePath, scriptPath);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Detection script output at " + executablePath + "\n" + StringUtil.join(outputLines, "\n"));
        }
        if (outputLines.size() == 3) {
          final PowerShellEdition edition = PowerShellEdition.fromString(outputLines.get(1));
          if (edition != null) {
            result = new PowerShellInfo(Boolean.parseBoolean(outputLines.get(2)) ? PowerShellBitness.x64 : PowerShellBitness.x86, exeFile.getParentFile(), outputLines.get(0), edition, executable);
          } else {
            LOG.warn("Failed to determine PowerShell edition for [" + executablePath + "]");
            LOG.debug(StringUtil.join("\n", outputLines));
          }
        } else {
          LOG.warn("Failed to parse output from PowerShell executable [" + executablePath + "]");
          LOG.debug(StringUtil.join("\n", outputLines));
        }
      } catch (ExecutionException e) {
        LOG.warnAndDebugDetails("Failed to run PowerShell detection script [" + scriptPath + "] with executable [" + executablePath + "]", e);
      }
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("PowerShell at " + exeFile.getAbsolutePath() + " was not found");
      }
    }
    return result;
  }

  private File prepareDetectionScript() {
    final File cacheDir = myConfiguration.getCacheDirectory(PowerShellConstants.PLUGIN_NAME);
    final File result = new File(cacheDir, "detect_" + System.currentTimeMillis() + ".ps1");
    try {
      FileUtil.writeFile(result, DETECTION_SCRIPT, "UTF-8");
    } catch (IOException e) {
      LOG.warnAndDebugDetails("Failed to write PowerShell detection script to file [" + result.getAbsolutePath() + "]", e);
      return null;
    }
    return result;
  }
}
