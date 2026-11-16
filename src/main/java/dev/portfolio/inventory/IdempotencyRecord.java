package dev.portfolio.inventory;
import jakarta.persistence.*;
@Entity @Table(name="idempotency_records")
public class IdempotencyRecord {
 @Id @Column(name="request_key",length=128) private String requestKey;
 @Column(name="payload_fingerprint",length=64,nullable=false) private String payloadFingerprint;
 @Column(name="http_status",nullable=false) private int httpStatus;
 @Column(name="outcome",nullable=false,length=40) private String outcome;
 @Column(name="reservation_id") private Long reservationId;
 protected IdempotencyRecord(){}
 public String getPayloadFingerprint(){return payloadFingerprint;}
 public int getHttpStatus(){return httpStatus;}
 public String getOutcome(){return outcome;}
 public Long getReservationId(){return reservationId;}
 public void complete(int status,String outcome,Long id){this.httpStatus=status;this.outcome=outcome;this.reservationId=id;}
}
