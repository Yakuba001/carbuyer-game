package com.carbuyer.market.internal.controller;

import com.carbuyer.market.api.dto.CreateListingRequestDto;
import com.carbuyer.market.api.dto.MarketListingResponseDto;
import com.carbuyer.market.internal.service.MarketService;
import com.carbuyer.user.api.UserFacade;
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
public class MarketController {

    private final MarketService service;
    private final UserFacade userFacade;

    @PostMapping("/listings")
    public ResponseEntity<MarketListingResponseDto> createListing(@Valid @RequestBody CreateListingRequestDto request,
                                                                  @AuthenticationPrincipal UserDetails userDetails) {
        Long sellerId = getUserId(userDetails);
        MarketListingResponseDto response = service.createListing(sellerId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/listings/{id}/cancel")
    public ResponseEntity<Void> cancelListing(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        Long sellerId = getUserId(userDetails);
        service.cancelListing(sellerId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/listings")
    public ResponseEntity<Page<MarketListingResponseDto>> getListings(Pageable pageable) {
        Page<MarketListingResponseDto> listingsPage = service.findAllByStatus(pageable);
        return ResponseEntity.ok(listingsPage);
    }

    @PostMapping("/listings/{id}/buy")
    public ResponseEntity<Void> buyListing(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        Long buyerId = getUserId(userDetails);
        service.buyCar(buyerId, id);
        return ResponseEntity.noContent().build();
    }

    private Long getUserId(UserDetails userDetails) {
        return userFacade.findIdByUsername(userDetails.getUsername());
    }
}
