package com.carbuyer.bot.controller;

import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.containers.MockMvcIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
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

    // TODO: Rebuild this
    @Test
    @WithMockUser(value = "bib@g.c", roles = "ADMIN")
    void botsOn_shouldReturn201_whenRequestIsCorrectAndBotsWereCreated() throws Exception {
        String expectedStatus = "Bots added";

        MvcResult response = mockMvc.perform(post("/api/v1/bot/add"))
                .andExpect(status().isCreated())
                .andReturn();

        StartBotResponse startBotResponse = objectMapper.readValue(
                response.getResponse().getContentAsString(),
                StartBotResponse.class);
        String result = startBotResponse.status();

        assertThat(result).isEqualTo(expectedStatus);
    }

    @Test
    @WithMockUser(value = "bib@g.c", roles = "USER")
    void addBots_shouldReturn403_whenAccessForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/bot/add"))
                .andExpect(status().isForbidden());
    }
}
