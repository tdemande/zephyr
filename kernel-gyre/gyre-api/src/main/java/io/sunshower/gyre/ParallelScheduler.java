package io.sunshower.gyre;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.val;

public class ParallelScheduler<E, V> implements Transformation<E, V, Schedule<E, V>> {

  @Override
  public Schedule<E, V> apply(Graph<E, V> graph) {
    return apply(graph, EdgeFilters.acceptAll(), NodeFilters.acceptAll());
  }

  @Override
  public Schedule<E, V> apply(Graph<E, V> graph, Predicate<E> edgeFilter, Predicate<V> nodeFilter) {
    val copy = removeSelfDependencies(graph);
    val result = new MutableSchedule<E, V>();

    while (!copy.isEmpty()) {
      val frontier = collectFrontier(copy, edgeFilter);
      val ts = new MutableTaskSet<E, V>();
      result.tasks.add(ts);
      for (val node : frontier) {
        copy.delete(node);
        ts.tasks.add(new LabeledTask<>(node, Collections.emptySet()));
      }

      for (val node : frontier) {
        copy.removeDependents(node, edgeFilter);
      }
    }
    return result;
  }

  private Graph<E, V> removeSelfDependencies(Graph<E, V> graph) {
    val copy = graph.clone();
    return copy.clone();
  }

  private List<V> collectFrontier(Graph<E, V> copy, Predicate<E> edgeFilter) {
    return copy.vertexSet()
        .stream()
        .filter(t -> copy.degreeOf(t, edgeFilter) == 0)
        .collect(Collectors.toList());
  }

  static final class MutableSchedule<E, V> implements Schedule<E, V> {

    final List<TaskSet<E, V>> tasks;

    MutableSchedule() {
      tasks = new ArrayList<>();
    }

    @Override
    public List<TaskSet<E, V>> getTasks() {
      return Collections.unmodifiableList(tasks);
    }

    @Override
    public int size() {
      return tasks.size();
    }

    @Override
    public TaskSet<E, V> get(int i) {
      return tasks.get(i);
    }

    @Override
    public Iterator<TaskSet<E, V>> iterator() {
      return tasks.iterator();
    }
  }

  @AllArgsConstructor
  static final class LabeledTask<E, V> implements Task<E, V> {
    final V value;
    final Set<E> edges;

    @Override
    public V getValue() {
      return value;
    }

    @Override
    public Set<E> getEdges() {
      return edges;
    }
  }

  static final class MutableTaskSet<E, V> implements TaskSet<E, V> {
    final List<Task<E, V>> tasks;

    MutableTaskSet(List<Task<E, V>> tasks) {
      this.tasks = tasks;
    }

    MutableTaskSet(Task<E, V> task) {
      this(Collections.singletonList(task));
    }

    MutableTaskSet() {
      tasks = new ArrayList<>();
    }

    @Override
    public List<Task<E, V>> getTasks() {
      return tasks;
    }

    @Override
    public int size() {
      return tasks.size();
    }
  }
}
