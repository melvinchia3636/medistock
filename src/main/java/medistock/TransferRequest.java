package medistock;

public record TransferRequest(WardStock from, WardStock to, int units) {
}
