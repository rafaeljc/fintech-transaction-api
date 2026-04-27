package io.github.rafaeljc.fintechtransactionapi.controller;

import io.github.rafaeljc.fintechtransactionapi.dto.StatisticsResponse;
import io.github.rafaeljc.fintechtransactionapi.service.StatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StatisticsControllerTest {

    @Mock
    private StatisticsService statisticsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new StatisticsController(statisticsService))
            .build();
    }

    @Test
    void getReturns200WithStatisticsBody() throws Exception {
        var response = new StatisticsResponse(
            3L,
            new BigDecimal("150"),
            new BigDecimal("50"),
            new BigDecimal("10"),
            new BigDecimal("100"));
        when(statisticsService.get()).thenReturn(response);

        mockMvc.perform(get("/statistics"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.count").value(3))
            .andExpect(jsonPath("$.sum").value(150))
            .andExpect(jsonPath("$.avg").value(50))
            .andExpect(jsonPath("$.min").value(10))
            .andExpect(jsonPath("$.max").value(100));

        verify(statisticsService).get();
    }

    @Test
    void getReturnsEmptyStatisticsWhenNoTransactions() throws Exception {
        when(statisticsService.get()).thenReturn(StatisticsResponse.empty());

        mockMvc.perform(get("/statistics"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.count").value(0))
            .andExpect(jsonPath("$.sum").value(0))
            .andExpect(jsonPath("$.avg").value(0))
            .andExpect(jsonPath("$.min").value(0))
            .andExpect(jsonPath("$.max").value(0));

        verify(statisticsService).get();
    }
}
