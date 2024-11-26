package io.gleecy.foi.paypal;

import java.util.Map;
import java.util.Set;
public enum Operation {
    CREATE_ORDER(Set.of("PmntProposed"), "PgoCreateOrder"),
    AUTHORIZE(Set.of("PmntPromised"), "PgoAuthorize"),
    REAUTHORIZE(Set.of("PmntAuthorized"), "PgoAuthorize"),
    CAPTURE(Set.of("PmntAuthorized", "PmntConfirmed"), "PgoCapture"),
    VOID(Set.of("PmntAuthorized", "PmntConfirmed"), "PgoRelease"),
    REFUND(Set.of("PmntAuthorized", "PmntDelivered", "PmntConfirmed"), "PgoRefund"),
    ;
    public final String id;
    public final Set<String> fromStatuses;
    private Operation(Set<String> fromStatuses, String id) {
        this.fromStatuses = fromStatuses;
        this.id = id;
    }
    private static Map<String, Operation> _valMap = Map.of(
            "PgoCreateOrder", CREATE_ORDER,
            "PgoAuthorize", AUTHORIZE,
            "PgoCapture", CAPTURE,
            "PgoRelease", VOID,
            "PgoRefund", REFUND);
    public static Operation fromString(String value) {
        if(value == null) return null;
        value = value.trim();
        Operation op = _valMap.get(value);
        if(op == null) {
            try {
                op = Operation.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        return op;
    }
}
