package io.sunshower.module.phases;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import io.sunshower.kernel.Module;
import io.sunshower.kernel.WeakReferenceClassLoader;
import io.sunshower.kernel.dependencies.DependencyGraph;
import io.sunshower.kernel.lifecycle.KernelModuleLoader;
import io.sunshower.kernel.misc.SuppressFBWarnings;
import java.io.IOException;
import java.util.Collections;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
  "PMD.JUnitAssertionsShouldIncludeMessage",
  "PMD.JUnitTestsShouldIncludeAssert",
  "PMD.UseProperClassLoader"
})
@SuppressFBWarnings
public class ModuleClassloaderModuleTest extends AbstractModulePhaseTestCase {

  private Module module;
  private String moduleId;
  private KernelModuleLoader contextLoader;
  private DependencyGraph dependencyGraph;
  private InstallationContext installationContext;
  private org.jboss.modules.Module moduleClasspath;

  @Override
  @BeforeEach
  void setUp() throws Exception {
    super.setUp();
    installationContext = resolve("test-plugin-1");

    module = installationContext.getInstalledModule();
    moduleId = module.getCoordinate().toCanonicalForm();
    dependencyGraph = DependencyGraph.create(Collections.singleton(module));
    contextLoader = new KernelModuleLoader(dependencyGraph);
    contextLoader.install(installationContext.getInstalledModule());
    moduleClasspath = contextLoader.loadModule(moduleId);
  }

  @Test
  void ensureResolvingModuleResultsInModuleBeingResolved() throws Exception {
    Class.forName("plugin1.Test", true, moduleClasspath.getClassLoader());
  }

  @Test
  void ensureLoadingManifestWorks() {
    assertNotNull(moduleClasspath.getClassLoader().getResource("META-INF/MANIFEST.MF"));
  }

  @Test
  void ensureLoadingFromLibraryWorks() throws ClassNotFoundException {
    Class.forName(
        "com.esotericsoftware.yamlbeans.YamlWriter", true, moduleClasspath.getClassLoader());
  }

  @Test
  void ensureLoadingFromLibraryManifestWorks() {
    assertNotNull(
        moduleClasspath
            .getClassLoader()
            .getResource("META-INF/services/io.sunshower.kernel.ext.PluginDescriptorReader"));
  }

  @Test
  void ensureLoadingFromModuleDependencyWorks() throws Exception {
    val ic = resolve("test-plugin-2");
    val imod = ic.getInstalledModule();
    contextLoader.install(imod);

    try {
      val cl = contextLoader.loadModule(imod.getCoordinate().toCanonicalForm()).getClassLoader();
      Class.forName("plugin1.Test", true, cl);
    } finally {
      ic.getInstalledModule().getFileSystem().close();
    }
  }

  @Test
  void ensureUnloadingModuleWorks() throws Exception {
    val ic = resolve("test-plugin-2");
    val imod = ic.getInstalledModule();
    contextLoader.install(imod);

    try {
      var cl =
          new WeakReferenceClassLoader(
              contextLoader.loadModule(imod.getCoordinate().toCanonicalForm()).getClassLoader());
      try {
        val clazz = Class.forName("plugin1.Test", true, cl);
        val obj = clazz.getConstructor().newInstance();
        assertNotNull(obj);
      } catch (Exception ex) {
        fail("Should not have reached here");
      }

      contextLoader.uninstall(installationContext.getInstalledModule());
    } finally {
      ic.getInstalledModule().getFileSystem().close();
    }
  }

  @Override
  @AfterEach
  void tearDown() throws IOException {
    super.tearDown();
    installationContext.getInstalledModule().getFileSystem().close();
  }
}