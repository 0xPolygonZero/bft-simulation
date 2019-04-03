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
  private double nextTimer;

  CorrectTendermintNode(EarthPosition position, double initialTimeout) {
    super(position);
    this.timeout = initialTimeout;
  }

  @Override public void onStart(Simulation simulation) {
    beginProposal(simulation, 0);
  }

  @Override public void onTimerEvent(TimerEvent timerEvent, Simulation simulation) {
    if (hasTerminated()) {
      return;
    }

    double time = timerEvent.getTime();
    if (time != nextTimer) {
      // It's a stale timer; we must have made the relevant state transition based on observed
      // messages rather than a timer. Ignore it.
      return;
    }

    switch (protocolState) {
      case PROPOSAL:
        beginPreVote(simulation, time);
        break;
      case PRE_VOTE:
        beginPreCommit(simulation, time);
        break;
      case PRE_COMMIT:
        ++cycle;
        // Exponential backoff.
        timeout *= 2;
        beginProposal(simulation, time);
        break;
      default:
        throw new AssertionError("Unexpected protocol state");
    }
  }

  @Override public void onMessageEvent(MessageEvent messageEvent, Simulation simulation) {
    if (hasTerminated()) {
      return;
    }

    Message message = messageEvent.getMessage();
    double time = messageEvent.getTime();
    cycleStates.putIfAbsent(message.getCycle(), new CycleState());
    CycleState cycleState = cycleStates.get(message.getCycle());

    if (message instanceof ProposalMessage) {
      cycleState.proposals.add(message.getProposal());
    } else if (message instanceof PreVoteMessage) {
      cycleState.preVoteCounts.merge(message.getProposal(), 1, Integer::sum);
    } else if (message instanceof PreCommitMessage) {
      cycleState.preCommitCounts.merge(message.getProposal(), 1, Integer::sum);
      Set<Proposal> committedProposals = keysWithMinCount(
          cycleState.preCommitCounts, quorumSize(simulation));
      if (!committedProposals.isEmpty()) {
        Proposal committedProposal = committedProposals.iterator().next();
        if (committedProposal != null) {
          terminate(committedProposal, time);
        }
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

  private void beginPreVote(Simulation simulation, double time) {
    protocolState = ProtocolState.PRE_VOTE;
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

  private void beginPreCommit(Simulation simulation, double time) {
    protocolState = ProtocolState.PRE_COMMIT;
    Map<Proposal, Integer> prevoteCounts = getCurrentCycleState().preVoteCounts;
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
    nextTimer = time + timeout;
    simulation.scheduleEvent(new TimerEvent(nextTimer, this));
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
    private Map<Proposal, Integer> preVoteCounts = new HashMap<>();
    private Map<Proposal, Integer> preCommitCounts = new HashMap<>();
  }

  private enum ProtocolState {
    PROPOSAL, PRE_VOTE, PRE_COMMIT
  }
}
