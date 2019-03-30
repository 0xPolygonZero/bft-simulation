import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {
  private static final int RANDOM_SEED = 123;

  public static void main(String[] args) {
    double terminationTime = runTendermint(1, 3, 1);
    System.out.printf("Tendermint termination time: %f\n", terminationTime);
  }

  private static double runTendermint(
      double initialTimeout, int correctNodes, int failedNodes) {
    Random random = new Random(RANDOM_SEED);
    List<Node> nodes = new ArrayList<>();
    for (int i = 0; i < correctNodes; ++i) {
      EarthPosition position = EarthPosition.randomPosition(random);
      nodes.add(new CorrectTendermintNode(position, initialTimeout));
    }
    for (int i = 0; i < failedNodes; ++i) {
      EarthPosition position = EarthPosition.randomPosition(random);
      nodes.add(new FailedNode(position));
    }

    Network network = new FullyConnectedNetwork(nodes, random);
    Simulation simulation = new Simulation(network);
    simulation.run();

    // TODO temp
    for (Node node : nodes) {
      System.out.printf("Node %s terminated at %f\n", node, node.getTerminationTime());
    }

    return nodes.stream()
        .filter(node -> node instanceof CorrectTendermintNode)
        .mapToDouble(Node::getTerminationTime)
        .average()
        .orElseThrow(AssertionError::new);
  }
}
