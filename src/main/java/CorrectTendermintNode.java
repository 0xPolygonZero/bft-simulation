import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class CorrectTendermintNode extends Node {
  private int cycle = 0;
  private Map<Integer, CycleState> cycleStates = new ArrayList<>();
  private ProtocolState protocolState = ProtocolState.PROPOSAL;
  private double timeout;

  CorrectTendermintNode(EarthPosition position, double initialTimeout) {
    super(position);
    this.timeout = initialTimeout;
  }

  @Override public void onStart(Simulation simulation) {
    resetTimeout(simulation);
  }

  @Override public void onTimerEvent(TimerEvent timerEvent, Simulation simulation) {
    cycleStates.putIfAbsent(cycle, new CycleState());
    CycleState cycleState = cycleStates.get(cycle);

    switch (protocolState) {
      case PROPOSAL:
        protocolState = ProtocolState.PREVOTE;
        resetTimeout(simulation);
        break;
      case PREVOTE:
        protocolState = ProtocolState.PRECOMMIT;
        resetTimeout(simulation);
        break;
      case PRECOMMIT:
        ++cycle;
        // Exponential backoff.
        timeout *= 2;
        protocolState = ProtocolState.PROPOSAL;
        resetTimeout(simulation);
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
    } if (message instanceof PreVoteMessage) {
      cycleState.prevoteCounts.merge(message.getProposal(), 1, Integer::sum);
    } else if (message instanceof PreCommitMessage) {
      cycleState.precommitCounts.merge(message.getProposal(), 1, Integer::sum);
    } else {
      throw new AssertionError("Unexpected message: " + message);
    }
  }

  private void resetTimeout(Simulation simulation) {
    simulation.scheduleEvent(new TimerEvent(timeout, this));
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
