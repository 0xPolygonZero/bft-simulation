import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {
  private static final int RANDOM_SEED = 123;

  public static void main(String[] args) {
    for (double initalTimeout = 0.01; initalTimeout <= 0.5; initalTimeout += 10.01) {
      System.out.printf("Initial timeout: %f\n", initalTimeout);
      System.out.printf("Tendermint termination time: %f\n",
          runTendermint(initalTimeout, 90, 10));
      System.out.printf("Mir termination time: %f\n",
          runMir(initalTimeout, 90, 10));
      System.out.println();
    }
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

    return nodes.stream()
        .filter(node -> node instanceof CorrectTendermintNode)
        .mapToDouble(Node::getTerminationTime)
        .average()
        .orElseThrow(AssertionError::new);
  }

  private static double runMir(
      double initialTimeout, int correctNodes, int failedNodes) {
    Random random = new Random(RANDOM_SEED);
    List<Node> nodes = new ArrayList<>();
    for (int i = 0; i < correctNodes; ++i) {
      EarthPosition position = EarthPosition.randomPosition(random);
      nodes.add(new CorrectMirNode(position, initialTimeout));
    }
    for (int i = 0; i < failedNodes; ++i) {
      EarthPosition position = EarthPosition.randomPosition(random);
      nodes.add(new FailedNode(position));
    }

    Network network = new FullyConnectedNetwork(nodes, random);
    Simulation simulation = new Simulation(network);
    simulation.run();

    return nodes.stream()
        .filter(node -> node instanceof CorrectMirNode)
        .mapToDouble(Node::getTerminationTime)
        .average()
        .orElseThrow(AssertionError::new);
  }
}
