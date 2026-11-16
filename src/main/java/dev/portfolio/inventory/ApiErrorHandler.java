package dev.portfolio.inventory;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
@RestControllerAdvice public class ApiErrorHandler {
 @ExceptionHandler(BadRequestException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
 public Map<String,String> bad(BadRequestException e){return Map.of("error",e.getMessage());}
 @ExceptionHandler(ConflictException.class) @ResponseStatus(HttpStatus.CONFLICT)
 public Map<String,String> conflict(ConflictException e){return Map.of("error",e.getMessage());}
 @ExceptionHandler(MethodArgumentNotValidException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
 public Map<String,String> invalid(MethodArgumentNotValidException e){return Map.of("error","Invalid reservation payload");}
}
