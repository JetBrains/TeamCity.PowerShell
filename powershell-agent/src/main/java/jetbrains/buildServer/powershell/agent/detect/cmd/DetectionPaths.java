package jetbrains.buildServer.powershell.agent.detect.cmd;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import jetbrains.buildServer.powershell.agent.Loggers;
import jetbrains.buildServer.powershell.agent.detect.DetectionContext;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

import static com.intellij.openapi.util.text.StringUtil.isEmptyOrSpaces;

public class DetectionPaths {

  @NotNull
  private static final Logger LOG = Loggers.DETECTION_LOGGER;

  /**
   * https://docs.microsoft.com/en-us/powershell/scripting/setup/installing-powershell-core-on-windows?view=powershell-5.1
   *
   * By default the package is installed to $env:ProgramFiles\PowerShell\
   */
  private final Set<String> WINDOWS_PATHS = getWindowsBasePaths();

  private static final List<String> PATHS = Arrays.asList(
          "/usr/local/bin", // mac os
          "/usr/bin"        // linux
  );


  public List<String> getPaths(@NotNull DetectionContext detectionContext) {
    // add predefined paths
    final List<String> propertyPaths = detectionContext.getSearchPaths();
    if (LOG.isDebugEnabled()) {
      if (!propertyPaths.isEmpty()) {
        LOG.debug("Adding PowerShell detection paths from [teamcity.powershell.detector.search.paths] property.");
        LOG.debug(StringUtil.join(propertyPaths, "\n"));
      }
    }
    final List<String> result = new ArrayList<String>(getPaths(propertyPaths));
    if (SystemInfo.isWindows) {
      result.addAll(getPaths(WINDOWS_PATHS));
    } else {
      result.addAll(PATHS);
    }
    return result;
  }

  private List<String> getPaths(@NotNull final Collection<String> paths) {
    final List<String> result = new ArrayList<String>();
    for (String base: paths) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Processing PowerShell.Core Windows path: " + base);
      }
      File f = new File(base);
      if (f.isDirectory()) {
        result.add(f.getAbsolutePath());
        result.addAll(populateWithChildren(f));
      }
    }
    return result;
  }

  private List<String> populateWithChildren(@NotNull File base) {
    List<String> result = new ArrayList<String>();
    for (File subDir: FileUtil.getSubDirectories(base)) {
      result.add(subDir.getAbsolutePath());
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Paths under PowerShell home that will be searched for PowerShell.Core install: "
              + Arrays.toString(result.toArray()));
    }
    return result;
  }

  private Set<String> getWindowsBasePaths() {
    final Set<String> result = new HashSet<String>();
    checkPathAndAdd(result, System.getenv("ProgramFiles"));
    checkPathAndAdd(result, System.getenv("ProgramFiles(x86)"));
    checkPathAndAdd(result, System.getenv("ProgramW6432"));
    // nanoserver powershell core location
    result.add(System.getenv("windir") + "\\System32\\WindowsPowerShell\\");
    return result;
  }

  private void checkPathAndAdd(@NotNull final Set<String> paths, String path) {
    if (!isEmptyOrSpaces(path)) {
      File base = new File(path, "PowerShell");
      if (base.isDirectory()) {
        paths.add(base.getAbsolutePath());
      }
    }
  }
}