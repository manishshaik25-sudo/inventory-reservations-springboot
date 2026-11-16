package dev.portfolio.inventory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ReservationService {
 private final InventoryRepository inventory;
 private final ReservationRepository reservations;
 private final IdempotencyRepository keys;
 private final JdbcTemplate jdbc;
 public ReservationService(InventoryRepository inventory, ReservationRepository reservations,IdempotencyRepository keys,JdbcTemplate jdbc){
  this.inventory=inventory;this.reservations=reservations;this.keys=keys;this.jdbc=jdbc;
 }
 @Transactional
 public ApiResult reserve(String key,ReservationRequest request){
  if(key==null||key.isBlank()||key.length()>128) throw new BadRequestException("Idempotency-Key must contain 1-128 characters");
  String sku=request.sku().trim();
  if(sku.isEmpty()||sku.length()>64) throw new BadRequestException("sku must contain 1-64 characters");
  String fingerprint=sha256(sku+"\n"+request.quantity());
  // MySQL's UNIQUE primary key serializes attempts for the same request key.
  // INSERT IGNORE waits for any concurrent uncommitted insert before returning 0.
  int inserted=jdbc.update("INSERT IGNORE INTO idempotency_records(request_key,payload_fingerprint,http_status,outcome,reservation_id) VALUES (?, ?, 0, 'PROCESSING', NULL)",key,fingerprint);
  if(inserted==0){
   IdempotencyRecord stored=keys.findById(key).orElseThrow(()->new IllegalStateException("Missing idempotency record"));
   if(!stored.getPayloadFingerprint().equals(fingerprint)) throw new ConflictException("Idempotency-Key reused with a different payload");
   if(stored.getHttpStatus()==0) throw new ConflictException("Request is still processing; retry later");
   return new ApiResult(stored.getHttpStatus(),responseFor(stored,sku,request.quantity()),true);
  }
  IdempotencyRecord record=keys.findById(key).orElseThrow();
  Inventory item=inventory.findById(sku).orElse(null);
  if(item==null){ record.complete(404,"SKU_NOT_FOUND",null);return new ApiResult(404,responseFor(record,sku,request.quantity()),false); }
  if(item.getAvailableQuantity()<request.quantity()){
   record.complete(409,"INSUFFICIENT_STOCK",null);return new ApiResult(409,responseFor(record,sku,request.quantity()),false);
  }
  int updated=inventory.reserveIfVersionMatches(sku,request.quantity(),item.getVersion());
  if(updated!=1){record.complete(409,"VERSION_CONFLICT",null);return new ApiResult(409,responseFor(record,sku,request.quantity()),false);}
  Reservation reservation=reservations.saveAndFlush(new Reservation(sku,request.quantity()));
  record.complete(201,"RESERVED",reservation.getId());
  return new ApiResult(201,responseFor(record,sku,request.quantity()),false);
 }
 private ReservationResponse responseFor(IdempotencyRecord r,String sku,int quantity){return new ReservationResponse(r.getReservationId(),sku,quantity,r.getOutcome());}
 private static String sha256(String input){
  try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));}
  catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
}
