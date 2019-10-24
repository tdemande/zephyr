package io.sunshower.module.phases;

import io.sunshower.kernel.process.KernelProcess;
import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModuleTransferPhaseTest extends AbstractModulePhaseTestCase {

  @Test
  void ensureModuleTransferWorksUpToTransferPhase() throws Exception {
    val transfer = spy(new ModuleTransferPhase());
    val process = new KernelProcess(context);
    process.addPhase(new ModuleDownloadPhase());
    process.addPhase(new ModuleScanPhase());
    process.addPhase(transfer);
    process.call();
    verify(transfer, times(1)).doExecute(any(), any());

    context.getContextValue(ModuleTransferPhase.)
  }
}
