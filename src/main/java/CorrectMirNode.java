import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class CorrectMirNode extends Node {
  private int cycle = 0;
  private Map<Integer, CycleState> cycleStates = new HashMap<>();
  private ProtocolState protocolState;
  private double timeout;
  private double nextTimer;

  CorrectMirNode(EarthPosition position, double initialTimeout) {
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

    if (timerEvent.getTime() != nextTimer) {
      // It's a stale timer; we must have made the relevant state transition based on observed
      // messages rather than a timer. Ignore it.
      return;
    }

    switch (protocolState) {
      case PROPOSAL:
        beginPrepareVote(simulation, timerEvent.getTime());
        break;
      case VOTE:
        beginVote(simulation, timerEvent.getTime());
        ++cycle;
        // Exponential backoff.
        // TODO
        //timeout *= 2;
        beginProposal(simulation, timerEvent.getTime());
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
    boolean currentCycle = message.getCycle() == cycle;
    cycleStates.putIfAbsent(message.getCycle(), new CycleState());
    CycleState cycleState = cycleStates.get(message.getCycle());

    if (message instanceof ProposalMessage) {
      cycleState.proposals.add(message.getProposal());
      if (currentCycle && protocolState == ProtocolState.PROPOSAL) {
        beginPrepareVote(simulation, time);
      }
    } else if (message instanceof PrepareVoteMessage) {
      cycleState.prepareVoteCounts.merge(message.getProposal(), 1, Integer::sum);
      if (currentCycle && protocolState != ProtocolState.COMMIT_VOTE) {
        Set<Proposal> preparedProposals =
            keysWithMinCount(cycleState.prepareVoteCounts, quorumSize(simulation));
        if (!preparedProposals.isEmpty()) {
          beginCommitVote(simulation, time);
        }
      }
    } else if (message instanceof CommitVoteMessage) {
      cycleState.commitVoteCounts.merge(message.getProposal(), 1, Integer::sum);
      Set<Proposal> committedProposals = keysWithMinCount(
          cycleState.commitVoteCounts, quorumSize(simulation));
      if (!committedProposals.isEmpty()) {
        Proposal committedProposal = committedProposals.iterator().next();
        if (committedProposal != null) {
          terminate(committedProposal, time);
          // TODO temp
          if (equals(simulation.getNetwork().getNodes().get(0)))
            System.out.printf("%s Tendermint terminated in cycle %d with timeout %f; %b\n",
                this, message.getCycle(), timeout, hasTerminated());
        } else if (currentCycle) {
          // Nil was committed in this cycle. Move on to the next cycle.
          ++cycle;
          beginProposal(simulation, time);
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

  private void beginPrepareVote(Simulation simulation, double time) {
    protocolState = ProtocolState.PREPARE_VOTE;
    Set<Proposal> proposals = getCurrentCycleState().proposals;
    Message message;
    if (proposals.size() == 1) {
      Proposal proposal = proposals.iterator().next();
      message = new PrepareVoteMessage(cycle, proposal, this);
    } else {
      // No proposals received, or more than one received. Either way, vote for null.
      message = new PrepareVoteMessage(cycle, null, this);
    }
    simulation.broadcast(this, message, time);
    resetTimeout(simulation, time);
  }

  private void beginCommitVote(Simulation simulation, double time) {
    protocolState = ProtocolState.COMMIT_VOTE;
    Map<Proposal, Integer> mergedVoteCounts = mergeCounts(
        getCurrentCycleState().prepareVoteCounts, getCurrentCycleState().commitVoteCounts);
    Set<Proposal> preparedProposals = keysWithMinCount(mergedVoteCounts, quorumSize(simulation));
    Message message;
    if (preparedProposals.isEmpty()) {
      message = new CommitVoteMessage(cycle, null, this);
    } else if (preparedProposals.size() == 1) {
      Proposal proposal = preparedProposals.iterator().next();
      message = new CommitVoteMessage(cycle, proposal, this);
    } else {
      throw new AssertionError("Safety violation");
    }
    simulation.broadcast(this, message, time);
    resetTimeout(simulation, time);
  }

  private static <K> Map<K, Integer> mergeCounts(Map<K, Integer> a, Map<K, Integer> b) {
    Set<K> allKeys = Stream.concat(a.keySet().stream(), b.keySet().stream())
        .collect(Collectors.toSet());
    return allKeys.stream().collect(Collectors.toMap(
        Function.identity(),
        k -> a.getOrDefault(k, 0) + b.getOrDefault(k, 0)));
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
    private Map<Proposal, Integer> prepareVoteCounts = new HashMap<>();
    private Map<Proposal, Integer> commitVoteCounts = new HashMap<>();
  }

  private enum ProtocolState {
    PROPOSAL, VOTE
  }
}
