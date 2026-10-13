package com.carbuyer.market.internal.controller;

import com.carbuyer.market.api.dto.CreateListingRequestDto;
import com.carbuyer.market.api.dto.MarketListingResponseDto;
import com.carbuyer.market.internal.service.MarketService;
import com.carbuyer.user.api.UserFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService service;
    private final UserFacade userFacade;

    @PostMapping("/listings")
    public ResponseEntity<MarketListingResponseDto> createListing(@Valid @RequestBody CreateListingRequestDto request,
                                                                  @AuthenticationPrincipal UserDetails userDetails) {
        Long sellerId = userFacade.findIdByUsername(userDetails.getUsername());
        MarketListingResponseDto response = service.createListing(sellerId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/listings/{id}/cancel")
    public ResponseEntity<Void> cancelListing(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        Long sellerId = userFacade.findIdByUsername(userDetails.getUsername());
        service.cancelListing(sellerId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/listings")
    public ResponseEntity<Page<MarketListingResponseDto>> getListings(
            @PageableDefault(size = 50, page = 0) Pageable pageable) {
        Page<MarketListingResponseDto> listingsPage = service.findAllByStatus(pageable);
        return ResponseEntity.ok(listingsPage);
    }
}
