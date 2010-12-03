package jetbrains.buildServer.powershell.agent.detect;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.powershell.common.PowerShellVersion;
import jetbrains.buildServer.util.Win32RegistryAccessor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static jetbrains.buildServer.util.Bitness.BIT32;
import static jetbrains.buildServer.util.Win32RegistryAccessor.Hive.LOCAL_MACHINE;

/**
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 *         03.12.10 14:57
 */
public class PowerShellDetectorTest extends BaseTestCase {

  @Test
  public void test_readPowerShellVersion_1() {
    final Mockery m = new Mockery();
    final Win32RegistryAccessor acc = m.mock(Win32RegistryAccessor.class);

    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "PowerShellVersion"); will(returnValue("1.0"));
    }});

    final PowerShellVersion ver = new PowerShellRegistry(BIT32, acc).getInstalledVersion();
    Assert.assertEquals(ver, PowerShellVersion.V_1_0);
  }

  @Test
  public void test_readPowerShellVersion_2() {
    final Mockery m = new Mockery();
    final Win32RegistryAccessor acc = m.mock(Win32RegistryAccessor.class);

    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "PowerShellVersion"); will(returnValue("2.0"));
    }});

    final PowerShellVersion ver = new PowerShellRegistry(BIT32, acc).getInstalledVersion();
    Assert.assertEquals(ver, PowerShellVersion.V_2_0);
  }

  @Test
  public void test_readPowerShellHome() throws IOException {
    final Mockery m = new Mockery();
    final Win32RegistryAccessor acc = m.mock(Win32RegistryAccessor.class);

    final File hom = createTempDir();

    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "ApplicationBase"); will(returnValue(hom.getPath()));
    }});

    final File home = new PowerShellRegistry(BIT32, acc).getPowerShellHome();
    Assert.assertEquals(home, hom);
  }

  @Test
  public void test_readPowerShellHome_notExists() throws IOException {
    final Mockery m = new Mockery();
    final Win32RegistryAccessor acc = m.mock(Win32RegistryAccessor.class);

    final File hom = new File("zzz");
    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1\\PowerShellEngine", "ApplicationBase"); will(returnValue(hom.getPath()));
    }});

    final File home = new PowerShellRegistry(BIT32, acc).getPowerShellHome();
    Assert.assertNull(home);
  }

  @Test
  public void test_isInstalled() {
    final Mockery m = new Mockery();
    final Win32RegistryAccessor acc = m.mock(Win32RegistryAccessor.class);

    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1", "Install"); will(returnValue("1"));
    }});

    final boolean is = new PowerShellRegistry(BIT32, acc).isPowerShellInstalled();
    Assert.assertTrue(is);
  }

  @Test
  public void test_isInstalled_not() {
    final Mockery m = new Mockery();
    final Win32RegistryAccessor acc = m.mock(Win32RegistryAccessor.class);

    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1", "Install"); will(returnValue("z"));
    }});

    final boolean is = new PowerShellRegistry(BIT32, acc).isPowerShellInstalled();
    Assert.assertFalse(is);
  }

  @Test
  public void test_isInstalled_not2() {
    final Mockery m = new Mockery();
    final Win32RegistryAccessor acc = m.mock(Win32RegistryAccessor.class);

    m.checking(new Expectations(){{
      allowing(acc).readRegistryText(LOCAL_MACHINE, BIT32, "SOFTWARE\\Microsoft\\PowerShell\\1", "Install"); will(returnValue(null));
    }});

    final boolean is = new PowerShellRegistry(BIT32, acc).isPowerShellInstalled();
    Assert.assertFalse(is);
  }
}
