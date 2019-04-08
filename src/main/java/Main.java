import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class Main {
  private static final int RANDOM_SEED = 12345;
  private static final double TIME_LIMIT = 4;
  private static final int SAMPLES = 100;

  public static void main(String[] args) {
    // Print the first row which contains column names.
    System.out.println("initial_timeout, tendermint, algorand, this_work");

    double tendermintBest = Double.MAX_VALUE;
    double algorandBest = Double.MAX_VALUE;
    double mirBest = Double.MAX_VALUE;

    for (double initalTimeout = 0.01; initalTimeout <= 0.4; initalTimeout += 0.01) {
      DoubleSummaryStatistics tendermintOverallStats = new DoubleSummaryStatistics(),
          algorandOverallStats = new DoubleSummaryStatistics(),
          mirOverallStats = new DoubleSummaryStatistics();
      for (int i = 0; i < SAMPLES; ++i) {
        Optional<DoubleSummaryStatistics> tendermintStats =
            runTendermint(initalTimeout, 90, 10);
        Optional<DoubleSummaryStatistics> algorandStats =
            runAlgorand(initalTimeout, 90, 10);
        Optional<DoubleSummaryStatistics> mirStats =
            runMir(initalTimeout, 90, 10);

        tendermintStats.ifPresent(tendermintOverallStats::combine);
        algorandStats.ifPresent(algorandOverallStats::combine);
        mirStats.ifPresent(mirOverallStats::combine);
      }

      if (tendermintOverallStats.getCount() > 0) {
        tendermintBest = Math.min(tendermintBest, tendermintOverallStats.getAverage());
      }
      if (algorandOverallStats.getCount() > 0) {
        algorandBest = Math.min(algorandBest, algorandOverallStats.getAverage());
      }
      if (mirOverallStats.getCount() > 0) {
        mirBest = Math.min(mirBest, mirOverallStats.getAverage());
      }

      System.out.printf("%.2f, %s, %s, %s\n",
          initalTimeout,
          tendermintOverallStats.getCount() > 0 ? tendermintOverallStats.getAverage() : "",
          algorandOverallStats.getCount() > 0 ? algorandOverallStats.getAverage() : "",
          mirOverallStats.getCount() > 0 ? mirOverallStats.getAverage() : "");
    }

    System.out.println();
    System.out.printf("Tendermint best: %.4f\n", tendermintBest);
    System.out.printf("Algorand best: %.4f\n", algorandBest);
    System.out.printf("Mir best: %.4f\n", mirBest);
  }

  private static Optional<DoubleSummaryStatistics> runTendermint(
      double initialTimeout, int correctNodeCount, int failedNodeCount) {
    Random random = new Random();
    List<Node> nodes = new ArrayList<>();
    for (int i = 0; i < correctNodeCount; ++i) {
      EarthPosition position = EarthPosition.randomPosition(random);
      nodes.add(new CorrectTendermintNode(position, initialTimeout));
    }
    for (int i = 0; i < failedNodeCount; ++i) {
      EarthPosition position = EarthPosition.randomPosition(random);
      nodes.add(new FailedNode(position));
    }
    Collections.shuffle(nodes, random);

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
    Random random = new Random();
    List<Node> nodes = new ArrayList<>();
    for (int i = 0; i < correctNodeCount; ++i) {
      EarthPosition position = EarthPosition.randomPosition(random);
      nodes.add(new CorrectAlgorandNode(position, initialTimeout));
    }
    for (int i = 0; i < failedNodeCout; ++i) {
      EarthPosition position = EarthPosition.randomPosition(random);
      nodes.add(new FailedNode(position));
    }
    Collections.shuffle(nodes, random);

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
    Random random = new Random();
    List<Node> nodes = new ArrayList<>();
    for (int i = 0; i < correctNodeCount; ++i) {
      EarthPosition position = EarthPosition.randomPosition(random);
      nodes.add(new CorrectMirNode(position, initialTimeout));
    }
    for (int i = 0; i < failedNodeCount; ++i) {
      EarthPosition position = EarthPosition.randomPosition(random);
      nodes.add(new FailedNode(position));
    }
    Collections.shuffle(nodes, random);

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
