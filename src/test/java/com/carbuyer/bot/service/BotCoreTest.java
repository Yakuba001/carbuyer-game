package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.entity.BotStatus;
import com.carbuyer.car.api.CarFacade;
import com.carbuyer.garage.api.GarageFacade;
import com.carbuyer.market.api.MarketFacade;
import com.carbuyer.user.api.UserFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BotCoreTest {

    @Mock
    private UserFacade userFacade;
    @Mock
    private CarFacade carFacade;
    @Mock
    private GarageFacade garageFacade;
    @Mock
    private MarketFacade marketFacade;
    @InjectMocks
    private BotCore botCore;


}
