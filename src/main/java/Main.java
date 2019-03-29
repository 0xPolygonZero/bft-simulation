import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Main {
  private static final int RANDOM_SEED = 123;

  public static void main(String[] args) {
    Random random = new Random(RANDOM_SEED);
    Set<Node> nodes = new HashSet<>();
    for (int i = 0; i < 111; ++i) {
      EarthPosition position = EarthPosition.randomPosition(random);
      nodes.add(new CorrectTendermintNode(position));
    }

    Network network = new FullyConnectedNetwork(nodes);
    Simulation simulation = new Simulation(network);
    simulation.run();

    for (Node node : nodes) {
      System.out.printf("Node terminated at %f\n", node.getTerminationTime());
    }
  }
}
