package io.zephyr.kernel;

import io.zephyr.api.Startable;
import io.zephyr.api.Stoppable;

public interface Lifecycle extends Startable, Stoppable {

  /**
   * This does not trigger a ModuleLifecycleChangedEvent (use the other methods for that)
   *
   * @param resolved
   */
  void setState(State resolved);

  enum State {
    Installed(0),
    Resolved(1),
    Uninstalled(-1),
    Starting(3),
    Active(4),
    Stopping(5),
    Failed(-2);

    final int value;

    State(final int value) {
      this.value = value;
    }

    public boolean isAtLeast(State state) {
      return value >= state.value;
    }
  }

  Module getModule();

  State getState();

  void reinstall();

  void uninstall();

  void start();

  void stop();

  void restart();
}
