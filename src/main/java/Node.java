abstract class Node {
  private final EarthPosition position;
  private Proposal output;
  private double terminationTime;

  Node(EarthPosition position) {
    this.position = position;
  }

  abstract void onStart(Simulation simulation);

  abstract void onTimerEvent(TimerEvent timerEvent, Simulation simulation);

  abstract void onMessageEvent(MessageEvent messageEvent, Simulation simulation);

  boolean hasTerminated() {
    return output != null;
  }

  void terminate(Proposal output, double terminationTime) {
    this.output = output;
    this.terminationTime = terminationTime;
  }

  /** The great-circle distance to another node, in meters. */
  double getDistance(Node that) {
    return this.position.getDistance(that.position);
  }

  double getTerminationTime() {
    return terminationTime;
  }
}

/** A node which has simply failed, and thus ignores all events. */
class FailedNode extends Node {
  FailedNode(EarthPosition position) {
    super(position);
  }

  @Override public void onStart(Simulation simulation) {
    // No-op.
  }

  @Override public void onTimerEvent(TimerEvent timerEvent, Simulation simulation) {
    // No-op.
  }

  @Override public void onMessageEvent(MessageEvent messageEvent, Simulation simulation) {
    // No-op.
  }
}

