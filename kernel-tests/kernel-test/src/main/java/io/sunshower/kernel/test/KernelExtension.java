package io.sunshower.kernel.test;

import io.sunshower.test.common.Tests;
import io.zephyr.kernel.core.Kernel;
import io.zephyr.kernel.module.ModuleInstallationGroup;
import io.zephyr.kernel.module.ModuleInstallationRequest;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.FileSystems;
import lombok.val;
import org.junit.jupiter.api.extension.*;
import org.springframework.beans.factory.annotation.ParameterResolutionDelegate;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.support.TestConstructorUtils;
import org.springframework.util.Assert;

public class KernelExtension
    implements BeforeAllCallback,
        AfterAllCallback,
        TestInstancePostProcessor,
        BeforeEachCallback,
        AfterEachCallback,
        BeforeTestExecutionCallback,
        AfterTestExecutionCallback,
        ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(KernelExtension.class);

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    val ctxManager = getTestContextManager(context);
    ctxManager.beforeTestClass();

    val ctx = ctxManager.getTestContext().getApplicationContext();
    val kernel = ctx.getBean(Kernel.class);
    kernel.start();

    var modules = (ModuleInstallationGroup) getStore(context).get(Module.Type.KernelModule);
    doInstall(kernel, modules);
    modules = (ModuleInstallationGroup) getStore(context).get(Module.Type.Plugin);
    doInstall(kernel, modules);
  }

  private void doInstall(Kernel kernel, ModuleInstallationGroup modules)
      throws InterruptedException, java.util.concurrent.ExecutionException {
    if (modules != null) {
      val prepped = kernel.getModuleManager().prepare(modules);
      prepped.commit().toCompletableFuture().get();
      kernel.stop();
      kernel.start();
    }
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {

    val ctxManager = getTestContextManager(context);
    ctxManager.beforeTestClass();

    val ctx = ctxManager.getTestContext().getApplicationContext();
    val kernel = ctx.getBean(Kernel.class);
    kernel.stop();

    try {
      val fs = FileSystems.getFileSystem(URI.create("droplet://kernel"));
      fs.close();
    } catch (Exception ex) {
      //meh
    }

    try {
      ctxManager.afterTestClass();
    } finally {
      getStore(context).remove(context.getRequiredTestClass());
    }
  }

  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext context)
      throws Exception {

    val ctxManager = getTestContextManager(context);
    ctxManager.prepareTestInstance(testInstance);
    val store = getStore(context);
    extractModules(context, store);
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    Object testInstance = context.getRequiredTestInstance();
    Method testMethod = context.getRequiredTestMethod();
    getTestContextManager(context).beforeTestMethod(testInstance, testMethod);
  }

  @Override
  public void beforeTestExecution(ExtensionContext context) throws Exception {
    Object testInstance = context.getRequiredTestInstance();
    Method testMethod = context.getRequiredTestMethod();
    getTestContextManager(context).beforeTestExecution(testInstance, testMethod);
  }

  @Override
  public void afterTestExecution(ExtensionContext context) throws Exception {
    Object testInstance = context.getRequiredTestInstance();
    Method testMethod = context.getRequiredTestMethod();
    Throwable testException = context.getExecutionException().orElse(null);
    getTestContextManager(context).afterTestExecution(testInstance, testMethod, testException);
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    Object testInstance = context.getRequiredTestInstance();
    Method testMethod = context.getRequiredTestMethod();
    Throwable testException = context.getExecutionException().orElse(null);
    getTestContextManager(context).afterTestMethod(testInstance, testMethod, testException);
  }

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    Parameter parameter = parameterContext.getParameter();
    Executable executable = parameter.getDeclaringExecutable();
    Class<?> testClass = extensionContext.getRequiredTestClass();
    return (TestConstructorUtils.isAutowirableConstructor(executable, testClass)
        || ApplicationContext.class.isAssignableFrom(parameter.getType())
        || ParameterResolutionDelegate.isAutowirable(parameter, parameterContext.getIndex()));
  }

  @Override
  @Nullable
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    Parameter parameter = parameterContext.getParameter();
    int index = parameterContext.getIndex();
    Class<?> testClass = extensionContext.getRequiredTestClass();
    ApplicationContext applicationContext = getApplicationContext(extensionContext);
    return ParameterResolutionDelegate.resolveDependency(
        parameter, index, testClass, applicationContext.getAutowireCapableBeanFactory());
  }

  public static ApplicationContext getApplicationContext(ExtensionContext context) {
    return getTestContextManager(context).getTestContext().getApplicationContext();
  }

  private static TestContextManager getTestContextManager(ExtensionContext context) {
    Assert.notNull(context, "ExtensionContext must not be null");
    Class<?> testClass = context.getRequiredTestClass();
    ExtensionContext.Store store = getStore(context);
    return store.getOrComputeIfAbsent(testClass, TestContextManager::new, TestContextManager.class);
  }

  private static ExtensionContext.Store getStore(ExtensionContext context) {
    return context.getRoot().getStore(NAMESPACE);
  }

  private void extractModules(ExtensionContext context, ExtensionContext.Store store)
      throws MalformedURLException {
    val testClass = context.getRequiredTestClass();

    val modules = testClass.getAnnotation(Modules.class);
    if (modules != null) {
      val kernelModules = new ModuleInstallationGroup();
      val plugins = new ModuleInstallationGroup();
      for (val moduleDef : modules.value()) {
        extractRequest(moduleDef, kernelModules, plugins);
      }
      store.put(Module.Type.Plugin, plugins);
      store.put(Module.Type.KernelModule, kernelModules);
    }
  }

  private void extractRequest(
      Module moduleDef,
      ModuleInstallationGroup kernelModules,
      ModuleInstallationGroup installationRequest)
      throws MalformedURLException {

    if (!Module.NONE.equals(moduleDef.project())) {
      val projectLocation = moduleDef.project();
      val project = Tests.relativeToProjectBuild(projectLocation, "war", "libs");
      val req = new ModuleInstallationRequest();
      req.setLocation(project.toURI().toURL());

      if (moduleDef.type() == Module.Type.Plugin) {
        installationRequest.add(req);
      } else {
        kernelModules.add(req);
      }
    }
  }
}
