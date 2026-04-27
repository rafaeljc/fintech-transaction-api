package io.github.rafaeljc.fintechtransactionapi.controller;

import io.github.rafaeljc.fintechtransactionapi.dto.TransactionRequest;
import io.github.rafaeljc.fintechtransactionapi.exception.GlobalExceptionHandler;
import io.github.rafaeljc.fintechtransactionapi.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new TransactionController(transactionService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void postWithValidBodyReturns201() throws Exception {
        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":\"42.00\",\"dateTime\":\"2024-01-15T10:00:00Z\"}"))
            .andExpect(status().isCreated())
            .andExpect(content().string(""));

        verify(transactionService).add(any(TransactionRequest.class));
    }

    @Test
    void postWithNullAmountReturns422() throws Exception {
        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":null,\"dateTime\":\"2024-01-15T10:00:00Z\"}"))
            .andExpect(status().isUnprocessableEntity());

        verify(transactionService, never()).add(any());
    }

    @Test
    void postWithNullDateTimeReturns422() throws Exception {
        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":\"42.00\",\"dateTime\":null}"))
            .andExpect(status().isUnprocessableEntity());

        verify(transactionService, never()).add(any());
    }

    @Test
    void postWithMalformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReturns200() throws Exception {
        mockMvc.perform(delete("/transactions"))
            .andExpect(status().isOk())
            .andExpect(content().string(""));

        verify(transactionService).clear();
    }
}
