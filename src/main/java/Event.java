abstract class Event implements Comparable<Event> {
  /** The time of the event, in seconds. */
  private final double time;

  /** The subject of the event. */
  private final Node subject;

  Event(double time, Node subject) {
    this.time = time;
    this.subject = subject;
  }

  double getTime() {
    return time;
  }

  Node getSubject() {
    return subject;
  }

  @Override public int compareTo(Event that) {
    double delta = this.time - that.time;
    if (delta == 0 && !this.equals(that)) {
      // We shouldn't return 0, since the messages aren't equal. identityHashCode gives us an
      // arbitrary but consistent ordering, although this isn't 100% safe since it assumes no
      // collisions (which are rare).
      return System.identityHashCode(this) - System.identityHashCode(that);
    } else {
      return (int) Math.signum(delta);
    }
  }
}

class TimerEvent extends Event {
  TimerEvent(double time, Node subject) {
    super(time, subject);
  }
}

class MessageEvent extends Event {
  private final Message message;

  MessageEvent(double time, Node subject, Message message) {
    super(time, subject);
    this.message = message;
  }

  Message getMessage() {
    return message;
  }
}
