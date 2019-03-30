import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class CorrectTendermintNode extends Node {
  private int cycle = 0;
  private Map<Integer, CycleState> cycleStates = new HashMap<>();
  private ProtocolState protocolState;
  private double timeout;

  CorrectTendermintNode(EarthPosition position, double initialTimeout) {
    super(position);
    this.timeout = initialTimeout;
  }

  @Override public void onStart(Simulation simulation) {
    beginProposal(simulation, 0);
  }

  @Override public void onTimerEvent(TimerEvent timerEvent, Simulation simulation) {
    switch (protocolState) {
      case PROPOSAL:
        beginPrevote(simulation, timerEvent.getTime());
        break;
      case PREVOTE:
        beginPrecommit(simulation, timerEvent.getTime());
        break;
      case PRECOMMIT:
        ++cycle;
        // Exponential backoff.
        timeout *= 2;
        beginProposal(simulation, timerEvent.getTime());
        break;
      case COMMIT:
        // Already terminated, nothing left to do.
        break;
      default:
        throw new AssertionError("Unexpected protocol state");
    }
  }

  @Override public void onMessageEvent(MessageEvent messageEvent, Simulation simulation) {
    Message message = messageEvent.getMessage();
    cycleStates.putIfAbsent(message.getCycle(), new CycleState());
    CycleState cycleState = cycleStates.get(message.getCycle());

    if (message instanceof ProposalMessage) {
      cycleState.proposals.add(message.getProposal());
    } else if (message instanceof PreVoteMessage) {
      cycleState.prevoteCounts.merge(message.getProposal(), 1, Integer::sum);
    } else if (message instanceof PreCommitMessage) {
      cycleState.precommitCounts.merge(message.getProposal(), 1, Integer::sum);
      Set<Proposal> committedProposals = keysWithMinCount(
          cycleState.precommitCounts, quorumSize(simulation));
      if (committedProposals.size() > 1) {
        throw new AssertionError("Safety violation");
      } else if (committedProposals.size() == 1) {
        output(committedProposals.iterator().next(), messageEvent.getTime());
        protocolState = ProtocolState.COMMIT;
      }
    } else {
      throw new AssertionError("Unexpected message: " + message);
    }
  }

  private void beginProposal(Simulation simulation, double time) {
    protocolState = ProtocolState.PROPOSAL;
    if (equals(simulation.getLeader(cycle))) {
      Proposal proposal = new Proposal();
      Message message = new ProposalMessage(cycle, proposal, this);
      simulation.broadcast(this, message, time);
    }
    resetTimeout(simulation, time);
  }

  private void beginPrevote(Simulation simulation, double time) {
    protocolState = ProtocolState.PREVOTE;
    Set<Proposal> proposals = getCurrentCycleState().proposals;
    Message message;
    if (proposals.size() == 1) {
      Proposal proposal = proposals.iterator().next();
      message = new PreVoteMessage(cycle, proposal, this);
    } else {
      // No proposals received, or more than one received. Either way, vote for null.
      message = new PreVoteMessage(cycle, null, this);
    }
    simulation.broadcast(this, message, time);
    resetTimeout(simulation, time);
  }

  private void beginPrecommit(Simulation simulation, double time) {
    protocolState = ProtocolState.PRECOMMIT;
    Map<Proposal, Integer> prevoteCounts = getCurrentCycleState().prevoteCounts;
    Set<Proposal> preVotedProposals = keysWithMinCount(prevoteCounts, quorumSize(simulation));
    Message message;
    if (preVotedProposals.isEmpty()) {
      message = new PreCommitMessage(cycle, null, this);
    } else if (preVotedProposals.size() == 1) {
      Proposal proposal = preVotedProposals.iterator().next();
      message = new PreCommitMessage(cycle, proposal, this);
    } else {
      throw new AssertionError("Safety violation");
    }
    simulation.broadcast(this, message, time);
    resetTimeout(simulation, time);
  }

  private static <K> Set<K> keysWithMinCount(Map<K, Integer> counts, int min) {
    return counts.keySet().stream()
        .filter(k -> counts.get(k) >= min)
        .collect(Collectors.toSet());
  }

  private void resetTimeout(Simulation simulation, double time) {
    simulation.scheduleEvent(new TimerEvent(time + timeout, this));
  }

  private CycleState getCurrentCycleState() {
    cycleStates.putIfAbsent(cycle, new CycleState());
    return cycleStates.get(cycle);
  }

  private int quorumSize(Simulation simulation) {
    int nodes = simulation.getNetwork().getNodes().size();
    return nodes * 2 / 3 + 1;
  }

  private class CycleState {
    private Set<Proposal> proposals = new HashSet<>();
    private Map<Proposal, Integer> prevoteCounts = new HashMap<>();
    private Map<Proposal, Integer> precommitCounts = new HashMap<>();
  }

  private enum ProtocolState {
    PROPOSAL, PREVOTE, PRECOMMIT, COMMIT
  }
}
