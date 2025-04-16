package com.saga.playground.checkoutservice.presentations;

import com.saga.playground.checkoutservice.application.impl.CheckoutRestServiceImpl;
import com.saga.playground.checkoutservice.constants.ErrorConstant;
import com.saga.playground.checkoutservice.presentations.responses.GetCheckoutStatusResponse;
import com.saga.playground.checkoutservice.utils.http.error.CommonHttpError;
import com.saga.playground.checkoutservice.utils.http.error.HttpException;
import org.hamcrest.Matchers;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CheckoutController.class)
@Import(CheckoutRestServiceImpl.class)
@ExtendWith({MockitoExtension.class})
class CheckoutRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckoutRestServiceImpl checkoutRestService;

    @Test
    void testHealthCheckEndpoint() throws Exception {
        var mockResp = Instancio.of(GetCheckoutStatusResponse.class).create();
        Mockito.when(checkoutRestService.getCheckoutStatus(mockResp.orderId()))
            .thenReturn(mockResp);

        mockMvc.perform(MockMvcRequestBuilders
                .get("/api/v1/checkout/status/%s".formatted(mockResp.orderId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", Matchers.is("OK")))
            .andExpect(jsonPath("$.data.orderId", Matchers.equalTo(mockResp.orderId())))
            .andExpect(jsonPath("$.data.status", Matchers.equalTo(mockResp.status().name())))
            .andExpect(jsonPath("$.data.isTerminated", Matchers.equalTo(mockResp.isTerminated())));
    }

    @Test
    void testHealthCheckEndpoint_NotFound() throws Exception {
        var mockResp = Instancio.of(GetCheckoutStatusResponse.class).create();

        Mockito.when(checkoutRestService.getCheckoutStatus(mockResp.orderId()))
            .thenThrow(new HttpException(CommonHttpError.NOT_FOUND_ERROR));

        mockMvc.perform(MockMvcRequestBuilders
                .get("/api/v1/checkout/status/%s".formatted(mockResp.orderId())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code", Matchers.is(ErrorConstant.CODE_NOT_FOUND_ERROR)));
    }

}
