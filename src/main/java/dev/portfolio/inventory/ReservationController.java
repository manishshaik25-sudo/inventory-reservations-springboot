package dev.portfolio.inventory;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/reservations")
public class ReservationController {
 private final ReservationService service;
 public ReservationController(ReservationService service){this.service=service;}
 @PostMapping
 public ResponseEntity<ReservationResponse> create(@RequestHeader(value="Idempotency-Key",required=false) String key,@Valid @RequestBody ReservationRequest request){
  ApiResult result=service.reserve(key,request);
  return ResponseEntity.status(result.status()).header("Idempotency-Replayed",Boolean.toString(result.replayed())).body(result.body());
 }
}
