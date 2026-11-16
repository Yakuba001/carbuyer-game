package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.exception.exceptions.BotCreationException;
import com.carbuyer.market.api.MarketFacade;
import com.carbuyer.user.api.UserFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BotServiceTest {

    @Mock
    private BotCore botCore;
    @Mock
    private UserFacade userFacade;
    @Mock
    private MarketFacade marketFacade;
    @InjectMocks
    private BotService distributeService;


}
