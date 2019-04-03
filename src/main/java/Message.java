abstract class Message {
  private final int cycle;
  private final Proposal proposal;
  private final Node sender;

  Message(int cycle, Proposal proposal, Node sender) {
    this.cycle = cycle;
    this.proposal = proposal;
    this.sender = sender;
  }

  int getCycle() {
    return cycle;
  }

  Proposal getProposal() {
    return proposal;
  }

  Node getSender() {
    return sender;
  }
}

class ProposalMessage extends Message {
  ProposalMessage(int cycle, Proposal proposal, Node sender) {
    super(cycle, proposal, sender);
  }
}

/** A Tendermint pre-vote message */
class PreVoteMessage extends Message {
  PreVoteMessage(int cycle, Proposal proposal, Node sender) {
    super(cycle, proposal, sender);
  }
}

/** A Tendermint pre-commit message */
class PreCommitMessage extends Message {
  PreCommitMessage(int cycle, Proposal proposal, Node sender) {
    super(cycle, proposal, sender);
  }
}

/** An Algorand soft-vote message */
class SoftVoteMessage extends Message {
  SoftVoteMessage(int cycle, Proposal proposal, Node sender) {
    super(cycle, proposal, sender);
  }
}

/** An Algorand cert-vote message */
class CertVoteMessage extends Message {
  CertVoteMessage(int cycle, Proposal proposal, Node sender) {
    super(cycle, proposal, sender);
  }
}

/** An Algorand next-vote message */
class NextVoteMessage extends Message {
  NextVoteMessage(int cycle, Proposal proposal, Node sender) {
    super(cycle, proposal, sender);
  }
}

abstract class MirVoteMessage extends Message {
  private final int round;

  MirVoteMessage(int cycle, int round, Proposal proposal, Node sender) {
    super(cycle, proposal, sender);
    this.round = round;
  }

  int getRound() {
    return round;
  }
}

/** A Mir prepare-vote message */
class PrepareVoteMessage extends MirVoteMessage {
  PrepareVoteMessage(int cycle, int round, Proposal proposal, Node sender) {
    super(cycle, round, proposal, sender);
  }
}

/** A Mir commit-vote message */
class CommitVoteMessage extends MirVoteMessage {
  CommitVoteMessage(int cycle, int round, Proposal proposal, Node sender) {
    super(cycle, round, proposal, sender);
  }
}
