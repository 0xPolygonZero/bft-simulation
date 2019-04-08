abstract class Message {
  private final int cycle;
  private final Proposal proposal;

  Message(int cycle, Proposal proposal) {
    this.cycle = cycle;
    this.proposal = proposal;
  }

  int getCycle() {
    return cycle;
  }

  Proposal getProposal() {
    return proposal;
  }
}

class ProposalMessage extends Message {
  ProposalMessage(int cycle, Proposal proposal) {
    super(cycle, proposal);
  }

  @Override public String toString() {
    return String.format("ProposalMessage[cycle=%d, proposal=%s]", getCycle(), getProposal());
  }
}

/** A Tendermint pre-vote message */
class PreVoteMessage extends Message {
  PreVoteMessage(int cycle, Proposal proposal) {
    super(cycle, proposal);
  }

  @Override public String toString() {
    return String.format("PreVoteMessage[cycle=%d, proposal=%s]", getCycle(), getProposal());
  }
}

/** A Tendermint pre-commit message */
class PreCommitMessage extends Message {
  PreCommitMessage(int cycle, Proposal proposal) {
    super(cycle, proposal);
  }

  @Override public String toString() {
    return String.format("PreCommitMessage[cycle=%d, proposal=%s]", getCycle(), getProposal());
  }
}

/** An Algorand soft-vote message */
class SoftVoteMessage extends Message {
  SoftVoteMessage(int cycle, Proposal proposal) {
    super(cycle, proposal);
  }
}

/** An Algorand cert-vote message */
class CertVoteMessage extends Message {
  CertVoteMessage(int cycle, Proposal proposal) {
    super(cycle, proposal);
  }
}

/** An Algorand next-vote message */
class NextVoteMessage extends Message {
  NextVoteMessage(int cycle, Proposal proposal) {
    super(cycle, proposal);
  }
}

abstract class MirVoteMessage extends Message {
  private final int round;

  MirVoteMessage(int cycle, int round, Proposal proposal) {
    super(cycle, proposal);
    this.round = round;
  }

  int getRound() {
    return round;
  }
}

/** A Mir prepare-vote message */
class PrepareVoteMessage extends MirVoteMessage {
  PrepareVoteMessage(int cycle, int round, Proposal proposal, Node sender) {
    super(cycle, round, proposal);
  }
}

/** A Mir commit-vote message */
class CommitVoteMessage extends MirVoteMessage {
  CommitVoteMessage(int cycle, int round, Proposal proposal, Node sender) {
    super(cycle, round, proposal);
  }
}
