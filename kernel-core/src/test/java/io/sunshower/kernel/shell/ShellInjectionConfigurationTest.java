package io.sunshower.kernel.shell;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import io.sunshower.kernel.core.Kernel;
import io.sunshower.kernel.launch.KernelOptions;
import lombok.ToString;
import lombok.val;
import org.junit.jupiter.api.Test;

class ShellInjectionConfigurationTest {
  @Test
  void ensureKernelIsInjected() {
    val ctcm = new Tcommand();
    DaggerShellModule.factory()
        .create(mock(Kernel.class), new KernelOptions(), mock(ShellConsole.class))
        .inject(ctcm);
    System.out.println(ctcm);
    assertNotNull(ctcm.kernel, "console must be injected");
  }

  @Test
  void ensureRunCommandHasCorrectDependencies() {
    val ctcm = new Tcommand();
    DaggerShellModule.factory()
        .create(mock(Kernel.class), new KernelOptions(), mock(ShellConsole.class))
        .inject(ctcm);
    System.out.println(ctcm);
    assertNotNull(ctcm.console, "console must be injected");
  }

  @ToString
  public static class Tcommand extends Command {}
}
