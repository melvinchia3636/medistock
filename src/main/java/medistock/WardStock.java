package medistock;

public class WardStock {
  private final String id;
  private int units; // guarded by this

  public WardStock(String id, int units) {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("id must not be blank");
    }
    if (units < 0) {
      throw new IllegalArgumentException("units must be >= 0: " + units);
    }
    this.id = id;
    this.units = units;
  }

  public String id() {
    return id;
  }

  public synchronized int units() {
    return units;
  }

  public static boolean transfer(WardStock from, WardStock to, int n) {
    if (from == to) {
      return false;
    }

    // Always take the locks in the same global order (smaller id first) so two
    // concurrent transfers in opposite directions cannot deadlock.
    WardStock first = from.before(to) ? from : to;
    WardStock second = (first == from) ? to : from;

    synchronized (first) {
      synchronized (second) {
        if (n < 1 || n > from.units) {
          return false;
        }
        from.units -= n;
        to.units += n;
        return true;
      }
    }
  }

  private boolean before(WardStock other) {
    int byId = id.compareTo(other.id);
    return byId != 0 ? byId < 0 : System.identityHashCode(this) < System.identityHashCode(other);
  }
}
