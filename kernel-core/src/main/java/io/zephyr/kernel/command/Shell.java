package io.zephyr.kernel.command;

import io.zephyr.api.*;
import java.rmi.Remote;
import java.rmi.RemoteException;
import lombok.NonNull;
import lombok.val;
import picocli.CommandLine;

public class Shell implements Invoker, Remote {
  protected final transient Console console;

  private final transient DefaultHistory history;
  private final transient CommandRegistry registry;

  final CommandDelegate delegate;

  protected Shell(
      @NonNull CommandRegistry registry,
      @NonNull CommandContext context,
      @NonNull Console console) {
    this.console = console;
    this.registry = registry;
    this.history = new DefaultHistory();
    this.delegate = new CommandDelegate(registry, history, context);
  }

  @Override
  public Console getConsole() throws RemoteException {
    return console;
  }

  @Override
  public Result invoke(Parameters parameters) throws RemoteException {

    val cli = new CommandLine(delegate).setUnmatchedArgumentsAllowed(true);
    try {
      cli.execute(parameters.formals());
    } catch (Exception ex) {
      cli.usage(System.out);
    }
    return null;
  }

  @Override
  public final CommandRegistry getRegistry() {
    return registry;
  }

  @Override
  public final History getHistory() {
    return history;
  }
}