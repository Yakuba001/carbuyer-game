package com.carbuyer.market.internal.controller;

import com.carbuyer.exception.dto.ErrorResponse;
import com.carbuyer.market.api.dto.CreateListingRequestDto;
import com.carbuyer.market.api.dto.MarketListingResponseDto;
import com.carbuyer.market.internal.service.MarketService;
import com.carbuyer.user.api.UserFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
@Tag(name = "Market", description = "Endpoints for listings management")
public class MarketController {

    private final MarketService service;
    private final UserFacade userFacade;

    @PostMapping("/listings")
    @Operation(
            summary = "Create a new listing",
            description = "Create a new listing and return response dto."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Listing successfully created",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MarketListingResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing, invalid or expired",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    ))
    })
    public ResponseEntity<MarketListingResponseDto> createListing(@Valid @RequestBody CreateListingRequestDto request,
                                                                  @AuthenticationPrincipal UserDetails userDetails) {
        Long sellerId = getUserId(userDetails);
        MarketListingResponseDto response = service.createListing(sellerId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/listings/{id}/cancel")
    @Operation(
            summary = "Cancel exists listing",
            description = "Switch from ACTIVE to CANCEL listing status"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Listing successfully canceled"),
            @ApiResponse(responseCode = "400", description = "Invalid request or business error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Listing not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> cancelListing(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        Long sellerId = getUserId(userDetails);
        service.cancelListing(sellerId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/listings")
    @Operation(
            summary = "Get all active market listings",
            description = "Fetches a paginated list of all currently active car listings in the market"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated list of listings successfully retrieved",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Page.class)
            )),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters supplied",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)
            ))
    })
    public ResponseEntity<Page<MarketListingResponseDto>> getListings(Pageable pageable) {
        Page<MarketListingResponseDto> listingsPage = service.findAllByStatus(pageable);
        return ResponseEntity.ok(listingsPage);
    }

    @PostMapping("/listings/{id}/buy")
    @Operation(
            summary = "Buy a car listing",
            description = "Processes the purchase of a car listing."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Car successfully purchased"),
            @ApiResponse(responseCode = "400", description = "Business rule violation",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Listing not found",
                    content = @Content(
                            mediaType = "application/json", schema =
                    @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> buyListing(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        Long buyerId = getUserId(userDetails);
        service.buyCar(buyerId, id);
        return ResponseEntity.noContent().build();
    }

    private Long getUserId(UserDetails userDetails) {
        return userFacade.findUserIdByEmail(userDetails.getUsername());
    }
}
