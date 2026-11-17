package com.carbuyer.bot.controller;

import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.dto.StopBotResponse;
import com.carbuyer.bot.entity.BotStatus;
import com.carbuyer.containers.MockMvcIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
public class BotControllerIT extends MockMvcIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(value = "bib@g.c", roles = "ADMIN")
    void botsOn_shouldReturn201_whenRequestIsCorrectAndBotsWereCreated() throws Exception {
        String expectedStatus = BotStatus.SUCCESSFULLY_STARTED.toString();

        MvcResult response = mockMvc.perform(post("/api/v1/bot/start/{amount}", 10))
                .andExpect(status().isCreated())
                .andReturn();


        StartBotResponse startBotResponse = objectMapper.readValue(
                response.getResponse().getContentAsString(),
                StartBotResponse.class);
        String result = startBotResponse.status();

        mockMvc.perform(post("/api/v1/bot/stop"));

        assertThat(result).isEqualTo(expectedStatus);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1000", "abc"})
    @WithMockUser(value = "bib@g.c", roles = "ADMIN")
    void startBots_shouldReturn400_whenUriVarInvalid(String invalidAmount) throws Exception {
        mockMvc.perform(post("/api/v1/bot/start/{amount}", invalidAmount))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(value = "bib@g.c", roles = "USER")
    void startBots_shouldReturn403_whenAccessForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/bot/start/{amount}", 10))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(value = "bib@g.c", roles = "ADMIN")
    void stopBots_shouldReturn200_whenBotsCorrectlyStopped() throws Exception {
        String expectedStatus = BotStatus.STOPPED.toString();

        MvcResult result = mockMvc.perform(post("/api/v1/bot/stop"))
                .andExpect(status().isOk())
                .andReturn();

        StopBotResponse resultResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                StopBotResponse.class);

        assertThat(resultResponse.status()).isEqualTo(expectedStatus);
    }

    @Test
    void stopBots_shouldReturn403_whenAccessForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/bot/stop")).andExpect(status().isForbidden());
    }
}
