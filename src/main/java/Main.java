import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class Main {
  private static final int RANDOM_SEED = 1234;
  private static final double TIME_LIMIT = 10;

  public static void main(String[] args) {
    double tendermintBest = Double.MAX_VALUE;
    double algorandBest = Double.MAX_VALUE;
    double mirBest = Double.MAX_VALUE;
    for (double initalTimeout = 0.01; initalTimeout <= 0.3; initalTimeout += 0.01) {
      Optional<DoubleSummaryStatistics> tendermintStats =
          runTendermint(initalTimeout, 90, 10);
      Optional<DoubleSummaryStatistics> algorandStats =
          runAlgorand(initalTimeout, 90, 10);
      Optional<DoubleSummaryStatistics> mirStats =
          runMir(initalTimeout, 90, 10);

      if (tendermintStats.isPresent()) {
        tendermintBest = Math.min(tendermintBest, tendermintStats.get().getAverage());
      }
      if (algorandStats.isPresent()) {
        algorandBest = Math.min(algorandBest, algorandStats.get().getAverage());
      }
      if (mirStats.isPresent()) {
        mirBest = Math.min(mirBest, mirStats.get().getAverage());
      }

      System.out.println();
      System.out.printf("Initial timeout: %f\n", initalTimeout);
      System.out.printf("Tendermint termination time: %s\n",
          tendermintStats.map(Main::statisticsToCompactString).orElse("FAILED"));
      System.out.printf("Algorand termination time: %s\n",
          algorandStats.map(Main::statisticsToCompactString).orElse("FAILED"));
      System.out.printf("Mir termination time: %s\n",
          mirStats.map(Main::statisticsToCompactString).orElse("FAILED"));
    }

    System.out.println();
    System.out.printf("Tendermint best: %.2f\n", tendermintBest);
    System.out.printf("Algorand best: %.2f\n", algorandBest);
    System.out.printf("Mir best: %.2f\n", mirBest);
  }

  private static Optional<DoubleSummaryStatistics> runTendermint(
      double initialTimeout, int correctNodeCount, int failedNodeCount) {
    Random random = new Random(RANDOM_SEED);
    List<Node> nodes = new ArrayList<>();
    for (int i = 0; i < correctNodeCount; ++i) {
      EarthPosition position = EarthPosition.randomPosition(random);
      nodes.add(new CorrectTendermintNode(position, initialTimeout));
    }
    for (int i = 0; i < failedNodeCount; ++i) {
      EarthPosition position = EarthPosition.randomPosition(random);
      nodes.add(new FailedNode(position));
    }

    Network network = new FullyConnectedNetwork(nodes, random);
    Simulation simulation = new Simulation(network);
    if (!simulation.run(TIME_LIMIT)) {
      return Optional.empty();
    }

    List<Node> correctNodes = nodes.stream()
        .filter(n -> n instanceof CorrectTendermintNode)
        .collect(Collectors.toList());
    if (!correctNodes.stream().allMatch(Node::hasTerminated)) {
      System.out.println("WARNING: Not all Tendermint nodes terminated.");
      return Optional.empty();
    }

    return Optional.of(correctNodes.stream()
        .mapToDouble(Node::getTerminationTime)
        .summaryStatistics());
  }

  private static Optional<DoubleSummaryStatistics> runAlgorand(
      double initialTimeout, int correctNodeCount, int failedNodeCout) {
    Random random = new Random(RANDOM_SEED);
    List<Node> nodes = new ArrayList<>();
    for (int i = 0; i < correctNodeCount; ++i) {
      EarthPosition position = EarthPosition.randomPosition(random);
      nodes.add(new CorrectAlgorandNode(position, initialTimeout));
    }
    for (int i = 0; i < failedNodeCout; ++i) {
      EarthPosition position = EarthPosition.randomPosition(random);
      nodes.add(new FailedNode(position));
    }

    Network network = new FullyConnectedNetwork(nodes, random);
    Simulation simulation = new Simulation(network);
    if (!simulation.run(TIME_LIMIT)) {
      return Optional.empty();
    }

    List<Node> correctNodes = nodes.stream()
        .filter(n -> n instanceof CorrectAlgorandNode)
        .collect(Collectors.toList());
    if (!correctNodes.stream().allMatch(Node::hasTerminated)) {
      System.out.println("WARNING: Not all Algorand nodes terminated.");
      return Optional.empty();
    }

    //System.out.println("Algorand times: " + correctNodes.stream().mapToDouble(Node::getTerminationTime).sorted().boxed().collect(Collectors.toList()));
    return Optional.of(nodes.stream()
        .mapToDouble(Node::getTerminationTime)
        .summaryStatistics());
  }

  private static Optional<DoubleSummaryStatistics> runMir(
      double initialTimeout, int correctNodeCount, int failedNodeCount) {
    Random random = new Random(RANDOM_SEED);
    List<Node> nodes = new ArrayList<>();
    for (int i = 0; i < correctNodeCount; ++i) {
      EarthPosition position = EarthPosition.randomPosition(random);
      nodes.add(new CorrectMirNode(position, initialTimeout));
    }
    for (int i = 0; i < failedNodeCount; ++i) {
      EarthPosition position = EarthPosition.randomPosition(random);
      nodes.add(new FailedNode(position));
    }

    Network network = new FullyConnectedNetwork(nodes, random);
    Simulation simulation = new Simulation(network);
    if (!simulation.run(TIME_LIMIT)) {
      return Optional.empty();
    }

    List<Node> correctNodes = nodes.stream()
        .filter(n -> n instanceof CorrectMirNode)
        .collect(Collectors.toList());
    if (!correctNodes.stream().allMatch(Node::hasTerminated)) {
      System.out.println("WARNING: Not all Mir nodes terminated.");
      return Optional.empty();
    }

    //System.out.println("Mir times: " + correctNodes.stream().mapToDouble(Node::getTerminationTime).sorted().boxed().collect(Collectors.toList()));
    return Optional.of(nodes.stream()
        .mapToDouble(Node::getTerminationTime)
        .summaryStatistics());
  }

  private static String statisticsToCompactString(DoubleSummaryStatistics statistics) {
    return String.format("min=%.2f, max=%.2f, average=%.2f",
        statistics.getMin(), statistics.getMax(), statistics.getAverage());
  }
}
