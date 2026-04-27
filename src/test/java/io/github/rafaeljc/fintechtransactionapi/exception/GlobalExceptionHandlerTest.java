package io.github.rafaeljc.fintechtransactionapi.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @RestController
    static class TestController {

        @PostMapping(value = "/test", consumes = "application/json")
        void handle(@Valid @RequestBody TestRequest body) {
        }
    }

    record TestRequest(@NotNull String field) {
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(""));
    }

    @Test
    void nullFieldReturns422() throws Exception {
        mockMvc.perform(post("/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"field\": null}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().string(""));
    }
}
