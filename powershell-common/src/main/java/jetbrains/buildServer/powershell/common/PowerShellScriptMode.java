

package jetbrains.buildServer.powershell.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 18:00
 */
public enum PowerShellScriptMode {
  CODE,
  FILE,
  ;

  @NotNull
  public String getValue() {
    return this.toString();
  }

  @Nullable
  public static PowerShellScriptMode fromString(@Nullable String sMode) {
    for (PowerShellScriptMode mode : values()) {
      if (mode.getValue().equals(sMode)) {
        return mode;
      }
    }
    return null;
  }
}