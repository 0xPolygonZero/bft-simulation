import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class CorrectAlgorandNode extends Node {
  private final double timeout;
  private final Map<Integer, CycleState> cycleStates = new HashMap<>();
  private int cycle = 0;
  private double nextTimer;
  private Phase phase;

  CorrectAlgorandNode(EarthPosition position, double timeout) {
    super(position);
    this.timeout = timeout;
  }

  @Override void onStart(Simulation simulation) {
    startProposal(simulation, 0);
  }

  @Override void onTimerEvent(TimerEvent timerEvent, Simulation simulation) {
    if (hasTerminated()) {
      return;
    }

    double time = timerEvent.getTime();
    if (time != nextTimer) {
      // It's a stale timer; we must have made the relevant state transition based on observed
      // messages rather than a timer. Ignore it.
      return;
    }

    switch (phase) {
      case PROPOSAL:
        doFiltering(simulation, time);
        break;
      case CERTIFYING:
        doFirstFinishing(simulation, time);
        break;
      case SECOND_FINISHING:
        throw new AssertionError("Eh? Shouldn't receive a timeout here.");
    }
  }

  @Override void onMessageEvent(MessageEvent messageEvent, Simulation simulation) {
    if (hasTerminated()) {
      return;
    }

    Message message = messageEvent.getMessage();
    double time = messageEvent.getTime();

    if (message instanceof ProposalMessage) {
      handleProposalMessage((ProposalMessage) message);
    } else if (message instanceof SoftVoteMessage) {
      handleSoftVoteMessage(simulation, time, (SoftVoteMessage) message);
    } else if (message instanceof CertVoteMessage) {
      handleCertVoteMessage(simulation, time, (CertVoteMessage) message);
    } else if (message instanceof NextVoteMessage) {
      handleNextVoteMessage(simulation, time, (NextVoteMessage) message);
    }
  }

  private void handleProposalMessage(ProposalMessage proposalMessage) {
    getCycleState(proposalMessage.getCycle()).proposals.add(proposalMessage.getProposal());
  }

  private void handleSoftVoteMessage(Simulation simulation, double time,
      SoftVoteMessage softVoteMessage) {
    getCycleState(softVoteMessage.getCycle()).addSoftVote(softVoteMessage);
    boolean currentCycle = cycle == softVoteMessage.getCycle();
    if (currentCycle && phase == Phase.CERTIFYING
        && getCurrentCycleState().myCertifiedProposal == null) {
      Set<Proposal> softVotedProposals = getCurrentCycleState().getSoftVotedProposals(simulation);
      if (!softVotedProposals.isEmpty()) {
        Proposal proposalToCertify = softVotedProposals.iterator().next();
        Message certVote = new CertVoteMessage(cycle, proposalToCertify, this);
        simulation.broadcast(this, certVote, time);
        getCurrentCycleState().myCertifiedProposal = proposalToCertify;
      }
    }
  }

  private void handleCertVoteMessage(Simulation simulation, double time,
      CertVoteMessage certVoteMessage) {
    CycleState messageCycleState = getCycleState(certVoteMessage.getCycle());
    messageCycleState.addCertVote(certVoteMessage);
    Set<Proposal> certifiedProposals = messageCycleState.getCertifiedProposals(simulation);

    if (!certifiedProposals.isEmpty()) {
      Proposal certifiedProposal = certifiedProposals.iterator().next();
      if (certifiedProposal == null) {
        throw new AssertionError("Shouldn't have cert-votes for nil?");
      }
      terminate(certifiedProposal, time);
    }
  }

  private void handleNextVoteMessage(Simulation simulation, double time,
      NextVoteMessage nextVoteMessage) {
    CycleState messageCycleState = getCycleState(nextVoteMessage.getCycle());
    messageCycleState.addNextVote(nextVoteMessage);
    boolean currentCycle = cycle == nextVoteMessage.getCycle();

    if (currentCycle && messageCycleState.hasNextVotedProposal(simulation)) {
      while (getCurrentCycleState().hasNextVotedProposal(simulation)) {
        Proposal nextVotedProposal = getCurrentCycleState()
            .getNextVotedProposals(simulation).iterator().next();
        ++cycle;
        getCurrentCycleState().startingValue = nextVotedProposal;
      }
      startProposal(simulation, time);
    }
  }

  private void startProposal(Simulation simulation, double time) {
    phase = Phase.PROPOSAL;
    if (equals(simulation.getLeader(cycle))) {
      Proposal proposal = new Proposal();
      Message message = new ProposalMessage(cycle, proposal, this);
      simulation.broadcast(this, message, time);
    }
    resetTimeout(simulation, time);
  }

  private void doFiltering(Simulation simulation, double time) {
    Set<Proposal> nextVotedProposals;
    if (cycle > 0) {
      nextVotedProposals = getCycleState(cycle - 1).getNextVotedProposals(simulation);
    } else {
      nextVotedProposals = new HashSet<>();
    }

    Proposal proposalToSoftVote = null;
    if (cycle == 0 || cycle > 0 && nextVotedProposals.contains(null)) {
      if (!getCurrentCycleState().proposals.isEmpty()) {
        proposalToSoftVote = getCurrentCycleState().proposals.iterator().next();
      }
    } else if (cycle > 0 && !nextVotedProposals.isEmpty()) {
      proposalToSoftVote = nextVotedProposals.iterator().next();
    }

    if (proposalToSoftVote != null) {
      Message softVote = new SoftVoteMessage(cycle, proposalToSoftVote, this);
      simulation.broadcast(this, softVote, time);
    }

    phase = Phase.CERTIFYING;
    resetTimeout(simulation, time);
  }

  private void doFirstFinishing(Simulation simulation, double time) {
    Proposal proposalToNextVote;
    if (getCurrentCycleState().myCertifiedProposal != null) {
      proposalToNextVote = getCurrentCycleState().myCertifiedProposal;
    } else if (cycle > 0
        && getCycleState(cycle - 1).getNextVotedProposals(simulation).contains(null)) {
      proposalToNextVote = null;
    } else {
      proposalToNextVote = getCurrentCycleState().startingValue;
    }

    NextVoteMessage nextVote = new NextVoteMessage(cycle, proposalToNextVote, this);
    simulation.broadcast(this, nextVote, time);

    phase = Phase.SECOND_FINISHING;
  }

  private void resetTimeout(Simulation simulation, double time) {
    nextTimer = time + timeout;
    simulation.scheduleEvent(new TimerEvent(nextTimer, this));
  }

  private CycleState getCurrentCycleState() {
    return getCycleState(cycle);
  }

  private CycleState getCycleState(int c) {
    cycleStates.putIfAbsent(c, new CycleState());
    return cycleStates.get(c);
  }

  private class CycleState {
    private Proposal startingValue = null;
    private Set<Proposal> proposals = new HashSet<>();
    private Proposal myCertifiedProposal = null;
    private Map<Proposal, Integer> softVoteCounts = new HashMap<>();
    private Map<Proposal, Integer> certVoteCounts = new HashMap<>();
    private Map<Proposal, Integer> nextVoteCounts = new HashMap<>();

    void addSoftVote(SoftVoteMessage softVote) {
      softVoteCounts.merge(softVote.getProposal(), 1, Integer::sum);
    }

    void addCertVote(CertVoteMessage certVote) {
      certVoteCounts.merge(certVote.getProposal(), 1, Integer::sum);
    }

    void addNextVote(NextVoteMessage nextVote) {
      nextVoteCounts.merge(nextVote.getProposal(), 1, Integer::sum);
    }

    Set<Proposal> getSoftVotedProposals(Simulation simulation) {
      return Util.keysWithMinCount(softVoteCounts, quorumSize(simulation));
    }

    Set<Proposal> getCertifiedProposals(Simulation simulation) {
      return Util.keysWithMinCount(certVoteCounts, quorumSize(simulation));
    }

    Set<Proposal> getNextVotedProposals(Simulation simulation) {
      return Util.keysWithMinCount(nextVoteCounts, quorumSize(simulation));
    }

    boolean hasNextVotedProposal(Simulation simulation) {
      return !getNextVotedProposals(simulation).isEmpty();
    }
  }

  private int quorumSize(Simulation simulation) {
    int nodes = simulation.getNetwork().getNodes().size();
    return nodes * 2 / 3 + 1;
  }

  private enum Phase {
    PROPOSAL, CERTIFYING, SECOND_FINISHING
  }
}
