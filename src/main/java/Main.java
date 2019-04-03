import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Random;

public class Main {
  private static final int RANDOM_SEED = 1234;

  public static void main(String[] args) {
    double tendermintBest = Double.MAX_VALUE;
    double mirBest = Double.MAX_VALUE;
    for (double initalTimeout = 0.01; initalTimeout <= 0.5; initalTimeout += 0.01) {
      DoubleSummaryStatistics tendermintStats = runTendermint(initalTimeout, 90, 10);
      DoubleSummaryStatistics mirStats = runMir(initalTimeout, 90, 10);

      tendermintBest = Math.min(tendermintBest, tendermintStats.getAverage());
      mirBest = Math.min(mirBest, mirStats.getAverage());

      System.out.println();
      System.out.printf("Initial timeout: %f\n", initalTimeout);
      System.out.printf("Tendermint termination time: %s\n",
          statisticsToCompactString(tendermintStats));
      System.out.printf("Mir termination time: %s\n",
          statisticsToCompactString(mirStats));
    }

    System.out.println();
    System.out.printf("Tendermint best: %.2f\n", tendermintBest);
    System.out.printf("Mir best: %.2f\n", mirBest);
  }

  private static DoubleSummaryStatistics runTendermint(
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
        .summaryStatistics();
  }

  private static DoubleSummaryStatistics runMir(
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
        .summaryStatistics();
  }

  private static String statisticsToCompactString(DoubleSummaryStatistics statistics) {
    return String.format("min=%.2f, max=%.2f, average=%.2f",
        statistics.getMin(), statistics.getMax(), statistics.getAverage());
  }
}
