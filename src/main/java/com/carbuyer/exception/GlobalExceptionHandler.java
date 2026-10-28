package com.carbuyer.exception;

import com.carbuyer.exception.dto.ErrorResponse;
import com.carbuyer.exception.exceptions.*;
import jakarta.persistence.PessimisticLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
            ObjectOptimisticLockingFailureException.class,
            PessimisticLockException.class,
            DataIntegrityViolationException.class})
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex) {
        String message = switch (ex) {
            case DataIntegrityViolationException ignored ->
                    "Data conflict, the record may already exist or violates system rules";
            case ObjectOptimisticLockingFailureException ignored ->
                    "Concurrent data modification conflict. Please retry.";
            case PessimisticLockException ignored ->
                    "The record was updated by another request. Please reload and try again.";
            default -> ex.getMessage();
        };
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "Conflict", message, Instant.now()));
    }

    @ExceptionHandler({
            BadCredentialsException.class,
            InvalidTokenException.class})
    public ResponseEntity<ErrorResponse> handleBadCredentials(RuntimeException ex) {
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
            GarageSlotNotFoundException.class,
            ListingUnexistsException.class})
    public ResponseEntity<ErrorResponse> handleUserNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "Not found", ex.getMessage(), Instant.now()));
    }
}
