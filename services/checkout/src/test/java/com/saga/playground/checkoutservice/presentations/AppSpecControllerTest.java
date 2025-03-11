package com.saga.playground.checkoutservice.presentations;

import com.saga.playground.checkoutservice.application.impl.AppSpecServiceImpl;
import com.saga.playground.checkoutservice.constants.ErrorConstant;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import(AppSpecServiceImpl.class)
@ExtendWith({MockitoExtension.class})
class AppSpecControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    private AppSpecServiceImpl appSpecService;
    
    @Test
    void testHealthCheckEndpoint() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", Matchers.is("OK")))
            .andExpect(jsonPath("$.data.isHealthy", Matchers.is(true)));
    }

    @Test
    void testUnhandledErrorEndpoint_ok() throws Exception {
        Mockito.doReturn("dummyString").when(appSpecService).unhandledError();

        mockMvc.perform(MockMvcRequestBuilders.get("/unhandled-error"))
            .andExpect(status().isOk());
    }

    @Test
    void testUnhandledErrorEndpoint_error() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/unhandled-error"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code", Matchers.is(ErrorConstant.CODE_INTERNAL_SERVER_ERROR)));
    }

}
