package dev.portfolio.inventory;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
@Testcontainers
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationIntegrationTest {
 @Container static final MySQLContainer<?> mysql=new MySQLContainer<>("mysql:8.4")
  .withDatabaseName("reservations").withUsername("test").withPassword("test");
 @DynamicPropertySource static void db(DynamicPropertyRegistry registry){
  registry.add("spring.datasource.url",mysql::getJdbcUrl);
  registry.add("spring.datasource.username",mysql::getUsername);
  registry.add("spring.datasource.password",mysql::getPassword);
 }
 @Autowired TestRestTemplate http;
 @Autowired JdbcTemplate jdbc;
 private String sku;
 @BeforeEach void seed(){
  sku="TEST-"+UUID.randomUUID();
  jdbc.update("INSERT INTO inventory(sku,available_quantity,version) VALUES (?,1,0)",sku);
 }
 private ResponseEntity<ReservationResponse> post(String key,int quantity){
  HttpHeaders h=new HttpHeaders();h.set("Idempotency-Key",key);
  return http.exchange("/reservations",HttpMethod.POST,new HttpEntity<>(new ReservationRequest(sku,quantity),h),ReservationResponse.class);
 }
 @Test void identicalRetryReturnsOriginalResponseAndDoesNotReserveTwice(){
  String key=UUID.randomUUID().toString();
  var first=post(key,1);var again=post(key,1);
  assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  assertThat(first.getBody()).isEqualTo(again.getBody());
  assertThat(again.getHeaders().getFirst("Idempotency-Replayed")).isEqualTo("true");
  assertThat(jdbc.queryForObject("select available_quantity from inventory where sku=?",Integer.class,sku)).isZero();
  assertThat(jdbc.queryForObject("select count(*) from reservations where sku=?",Integer.class,sku)).isEqualTo(1);
 }
 @Test void concurrentRequestsForLastUnitCannotOversell() throws Exception {
  ExecutorService pool=Executors.newFixedThreadPool(2);
  CountDownLatch gate=new CountDownLatch(1);
  try {
   Callable<ResponseEntity<ReservationResponse>> a=()->{gate.await();return post(UUID.randomUUID().toString(),1);};
   Future<ResponseEntity<ReservationResponse>> f1=pool.submit(a),f2=pool.submit(a);
   gate.countDown();
   var results=List.of(f1.get(20,TimeUnit.SECONDS),f2.get(20,TimeUnit.SECONDS));
   assertThat(results.stream().filter(r->r.getStatusCode()==HttpStatus.CREATED).count()).isEqualTo(1);
   assertThat(results.stream().filter(r->r.getStatusCode()==HttpStatus.CONFLICT).count()).isEqualTo(1);
   assertThat(jdbc.queryForObject("select available_quantity from inventory where sku=?",Integer.class,sku)).isZero();
   assertThat(jdbc.queryForObject("select count(*) from reservations where sku=?",Integer.class,sku)).isEqualTo(1);
  } finally {pool.shutdownNow();}
 }
 @Test void staleVersionCannotUpdateInventory(){
  long staleVersion=jdbc.queryForObject("select version from inventory where sku=?",Long.class,sku);
  int first=jdbc.update("update inventory set available_quantity=available_quantity-1,version=version+1 where sku=? and version=? and available_quantity>=1",sku,staleVersion);
  int second=jdbc.update("update inventory set available_quantity=available_quantity-1,version=version+1 where sku=? and version=? and available_quantity>=1",sku,staleVersion);
  assertThat(first).isEqualTo(1);assertThat(second).isZero();
  assertThat(jdbc.queryForObject("select available_quantity from inventory where sku=?",Integer.class,sku)).isZero();
 }
 @Test void sameKeyDifferentPayloadRejected(){
  String key=UUID.randomUUID().toString();
  assertThat(post(key,1).getStatusCode()).isEqualTo(HttpStatus.CREATED);
  assertThat(post(key,2).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
 }
}
