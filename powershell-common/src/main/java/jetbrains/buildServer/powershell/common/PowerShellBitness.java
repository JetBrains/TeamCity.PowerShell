

package jetbrains.buildServer.powershell.common;

import jetbrains.buildServer.util.Bitness;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 16:50
 */
public enum PowerShellBitness {
  x86 ("x86", "32-bit", Bitness.BIT32),
  x64 ("x64", "64-bit", Bitness.BIT64);

  private final String myValue;
  private final String myDisplayName;
  private final Bitness myBitness;

  PowerShellBitness(String value, String displayName, Bitness bitness) {
    myValue = value;
    myDisplayName = displayName;
    myBitness = bitness;
  }

  @NotNull
  public String getValue() {
    return myValue;
  }

  @NotNull
  public String getDisplayName() {
    return myDisplayName;
  }

  @Nullable
  public static PowerShellBitness fromString(@Nullable String bit) {
    for (PowerShellBitness b : values()) {
      if (b.getValue().equals(bit)) {
        return b;
      }
    }
    return null;
  }

  @NotNull
  public Bitness toBitness() {
    return myBitness;
  }

  @Override
  public String toString() {
    return myDisplayName;
  }
}