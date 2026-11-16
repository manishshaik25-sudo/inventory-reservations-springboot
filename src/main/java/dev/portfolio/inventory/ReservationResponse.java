package dev.portfolio.inventory;
public record ReservationResponse(Long reservationId,String sku,int quantity,String status) {}
