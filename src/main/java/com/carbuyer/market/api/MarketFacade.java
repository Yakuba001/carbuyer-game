package com.carbuyer.market.api;

import java.util.List;
import java.util.UUID;

public interface MarketFacade {

    void createListings(List<Long> sellerIds, List<UUID> carIds);
}
