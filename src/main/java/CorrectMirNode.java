import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class CorrectMirNode extends Node {
  /** The current cycle number. */
  private int cycle = 0;
  /** The observed state of each cycle. */
  private final Map<Integer, CycleState> cycleStates = new HashMap<>();
  /** The current round number within the current cycle. Round 0 is the proposal step. */
  private int round = 0;

  private double initialTimeout;
  private double nextTimer;

  CorrectMirNode(EarthPosition position, double initialTimeout) {
    super(position);
    this.initialTimeout = initialTimeout;
  }

  @Override public void onStart(Simulation simulation) {
    vote(simulation, 0);
    resetTimeout(simulation, 0);
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

    ++round;
    vote(simulation, time);
    resetTimeout(simulation, time);
  }

  @Override public void onMessageEvent(MessageEvent messageEvent, Simulation simulation) {
    if (hasTerminated()) {
      return;
    }

    Message message = messageEvent.getMessage();
    int messageCycle = message.getCycle();
    boolean currentCycle = message.getCycle() == cycle;
    CycleState messageCycleState = getCycleState(messageCycle);
    Proposal proposal = message.getProposal();
    double time = messageEvent.getTime();

    if (message instanceof ProposalMessage) {
      // A proposal was received. If it's for the current cycle, move to the next round and vote to
      // prepare it.
      messageCycleState.proposals.add(proposal);
      if (currentCycle && round == 0) {
        ++round;
        Message prepareVote = new PrepareVoteMessage(cycle, round, proposal, this);
        simulation.broadcast(this, prepareVote, time);
        resetTimeout(simulation, time);
      }
    } else {
      MirVoteMessage voteMessage = (MirVoteMessage) message;
      int messageRound = voteMessage.getRound();
      boolean currentRound = messageRound == round;
      messageCycleState.roundStates.putIfAbsent(messageRound, new RoundState());
      RoundState roundState = messageCycleState.roundStates.get(messageRound);

      if (voteMessage instanceof PrepareVoteMessage) {
        messageCycleState.addPrepareVote((PrepareVoteMessage) voteMessage);
        Set<Proposal> preparedProposals = roundState.getPreparedProposals(simulation);
        if (currentCycle && currentRound && !preparedProposals.isEmpty()) {
          // A proposal was prepared. Move to the next round and vote to commit it.
          ++round;
          vote(simulation, time);
          resetTimeout(simulation, time);
        }
      } else {
        messageCycleState.addCommitVote((CommitVoteMessage) voteMessage);
        Set<Proposal> preparedProposals = roundState.getPreparedProposals(simulation);
        Set<Proposal> committedProposals = roundState.getCommittedProposals(simulation);
        if (currentCycle && !committedProposals.isEmpty()) {
          Proposal committedProposal = committedProposals.iterator().next();
          if (committedProposal != null) {
            terminate(committedProposal, time);
          } else {
            // Nil was committed. Transition to the next cycle.
            round = 0;
            while (getCurrentCycleState().hasCommit(simulation)) {
              ++cycle;
            }
            vote(simulation, time);
            resetTimeout(simulation, time);
          }
        } else if (currentCycle && currentRound && !preparedProposals.isEmpty()) {
          // A proposal was prepared. Move to the next round and vote to commit it.
          ++round;
          vote(simulation, time);
          resetTimeout(simulation, time);
        }
      }
    }
  }

  private void vote(Simulation simulation, double time) {
    Message vote = getVote(simulation);
    if (vote != null) {
      simulation.broadcast(this, vote, time);
    }
  }

  private Message getVote(Simulation simulation) {
    if (round == 0) {
      // Proposal step.
      if (equals(simulation.getLeader(cycle))) {
        Proposal proposal = new Proposal();
        return new ProposalMessage(cycle, proposal);
      } else {
        return null;
      }
    } else {
      // Search for the latest proposal that was prepared, if any.
      for (int prevRound = round - 1; prevRound > 0; --prevRound) {
        RoundState prevRoundState = getCurrentCycleState().roundStates
            .getOrDefault(prevRound, new RoundState());
        Set<Proposal> prevPreparedProposals = prevRoundState.getPreparedProposals(simulation);
        if (!prevPreparedProposals.isEmpty()) {
          Proposal preparedProposal = prevPreparedProposals.iterator().next();
          if (prevRound == round - 1) {
            return new CommitVoteMessage(cycle, round, preparedProposal, this);
          } else {
            return new PrepareVoteMessage(cycle, round, preparedProposal, this);
          }
        }
      }

      // No proposal has been prepared. Fall back to whatever proposal we've observed if there was
      // exactly one, else nil.
      Proposal fallbackProposal;
      if (getCurrentCycleState().proposals.size() == 1) {
        fallbackProposal = getCurrentCycleState().proposals.iterator().next();
      } else {
        fallbackProposal = null;
      }
      return new PrepareVoteMessage(cycle, round, fallbackProposal, this);
    }
  }

  private void resetTimeout(Simulation simulation, double time) {
    nextTimer = time + getCurrentTimeout();
    simulation.scheduleEvent(new TimerEvent(nextTimer, this));
  }

  private double getCurrentTimeout() {
    int numIncreases = cycle;
    if (round >= 3) {
      numIncreases += (round - 1) / 2;
    }

    if (numIncreases > 30) {
      System.out.println("WARNING: Surpassed max timeout.");
      numIncreases = 30;
    }

    double multiplier = 1 << numIncreases;
    return initialTimeout * multiplier;
  }

  private static int quorumSize(Simulation simulation) {
    int nodes = simulation.getNetwork().getNodes().size();
    return nodes * 2 / 3 + 1;
  }

  private CycleState getCurrentCycleState() {
    return getCycleState(cycle);
  }

  private CycleState getCycleState(int c) {
    cycleStates.putIfAbsent(c, new CycleState());
    return cycleStates.get(c);
  }

  private static class CycleState {
    /** Proposals received within this cycle. */
    final Set<Proposal> proposals = new HashSet<>();

    /** The state of each round within this cycle. */
    final Map<Integer, RoundState> roundStates = new HashMap<>();

    void addPrepareVote(PrepareVoteMessage prepareVote) {
      roundStates.putIfAbsent(prepareVote.getRound(), new RoundState());
      roundStates.get(prepareVote.getRound()).prepareVoteCounts.merge(
          prepareVote.getProposal(), 1, Integer::sum);
    }

    void addCommitVote(CommitVoteMessage commitVote) {
      roundStates.putIfAbsent(commitVote.getRound(), new RoundState());
      roundStates.get(commitVote.getRound()).commitVoteCounts.merge(
          commitVote.getProposal(), 1, Integer::sum);
    }

    boolean hasCommit(Simulation simulation) {
      return roundStates.values().stream()
          .anyMatch(roundState -> roundState.hasCommit(simulation));
    }
  }

  private static class RoundState {
    final Map<Proposal, Integer> prepareVoteCounts = new HashMap<>();
    final Map<Proposal, Integer> commitVoteCounts = new HashMap<>();

    Map<Proposal, Integer> getCombinedVoteCounts() {
      Map<Proposal, Integer> result = new HashMap<>(prepareVoteCounts);
      commitVoteCounts.forEach((k, v) -> result.merge(k, v, Integer::sum));
      return result;
    }

    Set<Proposal> getPreparedProposals(Simulation simulation) {
      return Util.keysWithMinCount(getCombinedVoteCounts(), quorumSize(simulation));
    }

    Set<Proposal> getCommittedProposals(Simulation simulation) {
      return Util.keysWithMinCount(commitVoteCounts, quorumSize(simulation));
    }

    boolean hasCommit(Simulation simulation) {
      return !getCommittedProposals(simulation).isEmpty();
    }
  }
}
