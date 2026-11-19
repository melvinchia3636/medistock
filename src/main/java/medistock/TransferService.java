package medistock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TransferService {
  private final AtomicLong requests = new AtomicLong(), accepted = new AtomicLong();
  private final ExecutorService pool;
  private final AuditLog audit;

  public TransferService(int workers) {
    this(workers, new AuditLog());
  }

  public TransferService(int workers, AuditLog audit) {
    if (workers < 1) {
      throw new IllegalArgumentException("workers must be >= 1: " + workers);
    }
    if (audit == null) {
      throw new IllegalArgumentException("audit must not be null");
    }
    this.audit = audit;
    pool = Executors.newFixedThreadPool(workers);
  }

  public boolean transfer(WardStock from, WardStock to, int n) {
    requests.incrementAndGet();
    boolean moved = WardStock.transfer(from, to, n);
    if (moved) {
      accepted.incrementAndGet();
      audit.record(from.id() + " -> " + to.id() + " x" + n);
    }
    return moved;
  }

  public void submit(TransferRequest r) {
    pool.submit(() -> transfer(r.from(), r.to(), r.units()));
  }

  public void shutdownAndWait() throws InterruptedException {
    pool.shutdown();
    if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
      pool.shutdownNow();
    }
    audit.close();
  }

  public long requests() {
    return requests.get();
  }

  public long accepted() {
    return accepted.get();
  }
}
