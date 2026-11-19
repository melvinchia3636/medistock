package medistock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Append-only audit log. Writes are serialised on a single background thread
 * so a slow store never blocks the transfer threads.
 */
public class AuditLog {
  private final List<String> entries = new ArrayList<>(); // guarded by entries
  private final ExecutorService writer = Executors.newSingleThreadExecutor();

  /** Queues an entry; returns immediately (what vs when). */
  public void record(String entry) {
    if (entry == null) {
      throw new IllegalArgumentException("entry must not be null");
    }
    writer.submit(() -> {
      synchronized (entries) {
        entries.add(entry);
      }
    });
  }

  /** Stops accepting work and waits for the queued entries to be written. */
  public void close() throws InterruptedException {
    writer.shutdown();
    if (!writer.awaitTermination(10, TimeUnit.SECONDS)) {
      writer.shutdownNow();
    }
  }

  /** Returns a snapshot, safe to read while writes continue. */
  public List<String> entries() {
    synchronized (entries) {
      return List.copyOf(entries);
    }
  }
}
