import java.util.TreeSet;

class Simulation {
  private final Network network;
  private final TreeSet<Event> eventsByTime = new TreeSet<>();

  Simulation(Network network) {
    this.network = network;
  }

  void broadcast(Node source, Message message, double time) {
    for (Node destination : network.getNodes()) {
      double latency = network.getLatency(source, destination);
      double arrivalTime = time + latency;
      eventsByTime.add(new MessageEvent(arrivalTime, destination, message));
    }
  }

  Network getNetwork() {
    return network;
  }

  Node getLeader(int index) {
    return network.getLeader(index);
  }

  void scheduleEvent(Event event) {
    eventsByTime.add(event);
  }

  /**
   * Run until all events have been processed, including any newly added events which may be added
   * while running.
   *
   * @param timeLimit the maximum amount of time before the simulation halts
   * @return whether the simulation completed within the time limit
   */
  boolean run(double timeLimit) {
    for (Node node : network.getNodes()) {
      node.onStart(this);
    }

    while (!eventsByTime.isEmpty()) {
      Event event = eventsByTime.pollFirst();
      if (event.getTime() > timeLimit) {
        return false;
      }

      Node subject = event.getSubject();
      if (event instanceof TimerEvent) {
        subject.onTimerEvent((TimerEvent) event, this);
      } else if (event instanceof MessageEvent) {
        subject.onMessageEvent((MessageEvent) event, this);
      } else {
        throw new AssertionError("Unexpected event: " + event);
      }
    }

    return true;
  }
}