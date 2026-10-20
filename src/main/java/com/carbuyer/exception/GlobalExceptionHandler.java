package com.carbuyer.exception;

import com.carbuyer.exception.dto.ErrorResponse;
import com.carbuyer.exception.exceptions.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            EmailAlreadyExistsException.class,
            NameAlreadyExistsException.class,
            UserAlreadyExistsException.class,
            DataIntegrityViolationException.class})
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "Conflict", ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(401, "Unauthorized", ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(401, "Unauthorized", ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad request", message, Instant.now()));
    }

    @ExceptionHandler({
            MissedDataException.class,
            InactiveListingException.class,
            SelfBuyingException.class,
            InsufficientFundsException.class,
            NoEmptySlotsException.class,
            ChangeCarOwnerException.class})
    public ResponseEntity<ErrorResponse> handleMissedData(MissedDataException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad request", ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler({
            NotCarOwnerException.class,
            NotListingOwnerException.class})
    public ResponseEntity<ErrorResponse> handleNotCarOwner(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(403, "Forbidden", ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler({
            UserNotFoundException.class,
            CarNotFoundException.class,
            ListingNotFoundException.class,
            GarageSlotNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleUserNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "Not found", ex.getMessage(), Instant.now()));
    }
}
