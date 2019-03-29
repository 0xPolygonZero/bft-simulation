import java.util.List;

abstract class Network {
  /** The speed of light in a vacuum, in meters per second. */
  static double SPEED_OF_LIGHT = 299792458.0;

  /** The speed of light through a typical fiber optic cable, in meters per second. */
  static double SPEED_OF_FIBER = speedOfLight(1.4682);

  private final List<Node> nodes;

  Network(List<Node> nodes) {
    this.nodes = nodes;
  }

  List<Node> getNodes() {
    return nodes;
  }

  Node getLeader(int index) {
    // Round robin.
    return nodes.get(index % nodes.size());
  }

  /**
   * The one-way latency, in seconds, taken to deliver a message from {@code source} to
   * {@code destination}.
   */
  abstract double getLatency(Node source, Node destination);

  /** The speed of light through a medium with a given index of refraction, in meters per second. */
  private static double speedOfLight(double refractiveIndex) {
    return SPEED_OF_LIGHT / refractiveIndex;
  }
}

/**
 * A network in which all nodes are directly connected through a fiber optic cable.
 */
class FullyConnectedNetwork extends Network {
  FullyConnectedNetwork(List<Node> nodes) {
    super(nodes);
  }

  public double getLatency(Node source, Node destination) {
    return source.getDistance(destination) / Network.SPEED_OF_FIBER;
  }
}
