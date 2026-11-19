package medistock;

public class StockTransferV1 {
  static class WardStock {
    final String id;
    int units;

    WardStock(String id, int units) {
      this.id = id;
      this.units = units;
    }
  }

  static int requests = 0;

  static void transfer(WardStock from, WardStock to, int n) {
    synchronized (from) {
      synchronized (to) {
        if (n >= 1 && n <= from.units) {
          from.units -= n;
          to.units += n;
        }
      }
    }

    requests++;
  }

  public static void main(String[] args) throws InterruptedException {
    WardStock icu = new WardStock("ICU", 500), er = new WardStock("ER", 500);
    WardStock ward = new WardStock("WARD", 500), lab = new WardStock("LAB", 500);

    int N = 100_000;
    Thread t1 = new Thread(() -> { for (int i = 0; i < N; i++) transfer(icu, er, 1); });
    Thread t2 = new Thread(() -> { for (int i = 0; i < N; i++) transfer(ward, lab, 1); });

    t1.start();
    t2.start();
    t1.join();
    t2.join();

    System.out.println("requests=" + requests); // expected 200000
  }
}
