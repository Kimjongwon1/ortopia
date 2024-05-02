package com.example.ordering_lecture.review.controller;

import com.example.ordering_lecture.common.OrTopiaResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ortopia-order-service", url ="http://ortopia-order-service.default.svc.cluster.local")
public interface OrderServiceClient {
    @GetMapping("check/review/{orderDetailId}")
    OrTopiaResponse checkReview(@PathVariable(name = "orderDetailId") Long orderDetailId);

}
