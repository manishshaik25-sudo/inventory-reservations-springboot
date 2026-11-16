package dev.portfolio.inventory;
import jakarta.validation.constraints.*;
public record ReservationRequest(@NotBlank String sku,@Min(1) int quantity) {}
