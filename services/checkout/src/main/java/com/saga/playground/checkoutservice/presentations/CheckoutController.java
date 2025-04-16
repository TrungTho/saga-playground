package com.saga.playground.checkoutservice.presentations;

import com.saga.playground.checkoutservice.application.CheckoutRestService;
import com.saga.playground.checkoutservice.presentations.responses.GetCheckoutStatusResponse;
import com.saga.playground.checkoutservice.utils.http.model.HttpResponseModel;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/checkout")
public class CheckoutController {

    private final CheckoutRestService checkoutRestService;

    /**
     * Health of across-domain services
     *
     * @return Health Check
     */
    @GetMapping("/status/{orderId}")
    public final HttpResponseModel<GetCheckoutStatusResponse> getCheckoutStatus(
        @PathVariable String orderId) {
        return HttpResponseModel.success(checkoutRestService.getCheckoutStatus(orderId));
    }

}
