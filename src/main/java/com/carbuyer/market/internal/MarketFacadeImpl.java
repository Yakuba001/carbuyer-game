package com.carbuyer.market.internal;

import com.carbuyer.market.api.MarketFacade;
import com.carbuyer.market.internal.service.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class MarketFacadeImpl implements MarketFacade {

    private final MarketService marketService;

    @Override
    public void createListings(List<Long> sellerIds, List<UUID> carIds) {
        marketService.createListings(sellerIds, carIds);
    }
}
