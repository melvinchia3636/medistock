package medistock;

public class StockTransferV2 {
  public static void main(String[] args) throws InterruptedException {
    WardStock icu = new WardStock("ICU", 500), er = new WardStock("ER", 500);
    TransferService service = new TransferService(4);

    service.submit(new TransferRequest(icu, er, 100));
    service.submit(new TransferRequest(er, icu, 30));
    service.submit(new TransferRequest(icu, er, 900));
    service.shutdownAndWait();

    System.out.println("ICU=" + icu.units() + " ER=" + er.units()
        + " requests=" + service.requests());
  }
}
