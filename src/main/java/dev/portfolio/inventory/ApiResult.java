package dev.portfolio.inventory;
public record ApiResult(int status,ReservationResponse body,boolean replayed) {}
