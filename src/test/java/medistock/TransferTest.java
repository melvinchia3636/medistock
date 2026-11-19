package medistock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TransferTest {
  private final WardStock icu = new WardStock("ICU", 500);
  private final WardStock er = new WardStock("ER", 500);
  private final TransferService service = new TransferService(4);
  private static final int N = 100_000;

  @AfterEach
  void stop() throws InterruptedException {
    service.shutdownAndWait();
  }

  @Test // row 1
  void transfer_nominal_movesUnits() {
    assertTrue(service.transfer(icu, er, 100));
    assertEquals(400, icu.units());
    assertEquals(600, er.units());
  }

  @Test // row 2
  void transfer_exactBalance_drainsSource() {
    assertTrue(service.transfer(icu, er, 500));
    assertEquals(0, icu.units());
    assertEquals(1000, er.units());
  }

  @Test // row 3
  void transfer_moreThanBalance_rejected() {
    assertFalse(service.transfer(icu, er, 501));
    assertEquals(500, icu.units());
    assertEquals(500, er.units());
  }

  @Test // row 4
  void transfer_zeroUnits_rejected() {
    assertFalse(service.transfer(icu, er, 0));
    assertEquals(500, icu.units());
    assertEquals(500, er.units());
  }

  @Test // row 5
  void wardStock_negativeUnits_throws() {
    assertThrows(IllegalArgumentException.class, () -> new WardStock("LAB", -1));
  }

  @Test // row 6
  void transfer_sameStock_rejected() {
    assertFalse(service.transfer(icu, icu, 10));
    assertEquals(500, icu.units());
  }

  @Test // row 7
  void transfer_concurrentPairs_countsAllRequests() {
    WardStock ward = new WardStock("WARD", 500), lab = new WardStock("LAB", 500);

    assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
      Thread t1 = new Thread(() -> repeat(N, () -> service.transfer(icu, er, 1)));
      Thread t2 = new Thread(() -> repeat(N, () -> service.transfer(ward, lab, 1)));
      t1.start();
      t2.start();
      t1.join();
      t2.join();
    });

    assertEquals(200_000, service.requests());
    assertEquals(1000, icu.units() + er.units());
    assertEquals(1000, ward.units() + lab.units());
  }

  @Test // row 8 (deadlock)
  void transfer_oppositeDirections_noDeadlock() {
    assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
      Thread t1 = new Thread(() -> repeat(N, () -> service.transfer(icu, er, 1)));
      Thread t2 = new Thread(() -> repeat(N, () -> service.transfer(er, icu, 1)));
      t1.start();
      t2.start();
      t1.join();
      t2.join();
    });
    assertEquals(1000, icu.units() + er.units());
  }

  @Test // row 9
  void submit_thousandRequests_allAccepted() throws InterruptedException {
    WardStock pharmacy = new WardStock("PHARMACY", 1000);

    for (int i = 0; i < 1000; i++) {
      service.submit(new TransferRequest(pharmacy, er, 1));
    }
    service.shutdownAndWait();

    assertEquals(1000, service.accepted());
    assertEquals(1500, er.units());
  }

  @Test // row 10
  void transfer_stress_totalConserved() {
    WardStock[] wards = {
        new WardStock("WARD-1", 500),
        new WardStock("WARD-2", 500),
        new WardStock("WARD-3", 500),
        new WardStock("WARD-4", 500)
    };

    assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
      Thread[] threads = new Thread[8];
      for (int i = 0; i < threads.length; i++) {
        WardStock from = wards[i % wards.length];
        WardStock to = wards[(i + 1) % wards.length];
        threads[i] = new Thread(() -> repeat(N / 2, () -> service.transfer(from, to, 1)));
      }
      for (Thread t : threads) t.start();
      for (Thread t : threads) t.join();
    });

    long total = 0;
    for (WardStock w : wards) total += w.units();
    assertEquals(2000, total);
  }

  @Test // row 11
  void audit_acceptedTransfers_areRecorded() throws InterruptedException {
    AuditLog audit = new AuditLog();
    TransferService audited = new TransferService(4, audit);
    try {
      for (int i = 0; i < 1000; i++) {
        audited.transfer(icu, er, 1);
      }
    } finally {
      audited.shutdownAndWait();
    }

    assertEquals(500, audited.accepted());
    assertEquals(500, audit.entries().size());
  }

  private static void repeat(int times, Runnable action) {
    for (int i = 0; i < times; i++) action.run();
  }
}
