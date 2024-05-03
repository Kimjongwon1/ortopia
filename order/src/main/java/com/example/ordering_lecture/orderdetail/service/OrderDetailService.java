package com.example.ordering_lecture.orderdetail.service;

import com.example.ordering_lecture.common.ErrorCode;
import com.example.ordering_lecture.common.OrTopiaException;
import com.example.ordering_lecture.feign.FeignClient;
import com.example.ordering_lecture.orderdetail.dto.*;
import com.example.ordering_lecture.orderdetail.entity.OrderDetail;
import com.example.ordering_lecture.orderdetail.repository.OrderDetailRepository;
import com.example.ordering_lecture.payment.controller.ItemServiceClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderDetailService {
    private final OrderDetailRepository orderDetailRepository;
    private final ItemServiceClient itemServiceClient;
    private final FeignClient feignClient;

    public OrderDetailService(OrderDetailRepository orderDetailRepository, ItemServiceClient itemServiceClient, FeignClient feignClient) {
        this.orderDetailRepository = orderDetailRepository;
        this.itemServiceClient = itemServiceClient;
        this.feignClient = feignClient;
    }
    // 일별 구매 금액을 위한 데이터
    public List<BuyerGraphPriceData> getBuyerGraphPriceData(String email) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusWeeks(2);
        System.out.println("startDate = " + startDate);
        System.out.println("endDate = " + endDate);
        return orderDetailRepository.findSumPriceByDateBetweenAndStatueAndEmail(startDate, endDate, email);
    }
    // 일별 구매 건수를 위한 데이터
    public List<BuyerGraphCountData> getBuyerGraphCountData(String email) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusWeeks(2);
        return orderDetailRepository.findCompletedOrdersByEmailAndDateRange(startDate, endDate, email);
    }
    // 일별 판매 금액을 위한 데이터
    public List<SellerGraphPriceData> getSellerGraphPriceData(Long sellerId) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusWeeks(2);
        return orderDetailRepository.findSalesData(startDate, endDate, sellerId);
    }
    // 일별 판매 건수를 위한 데이터
    public List<SellerGraphCountData> getSellerGraphCountData(Long sellerId) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusWeeks(2);
        return orderDetailRepository.findSalesDataBySellerIdAndDateRange(startDate, endDate, sellerId);
    }
    // 아이템 별 판매 금액을 위한 데이터
    public List<SellerGraphItemPriceData> getSellerEachItemPriceData(Long sellerId) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusWeeks(2);

        List<Object[]> itemPriceData = orderDetailRepository.getItemPriceDataBySellerId(startDate, endDate, sellerId);
        List<SellerGraphItemPriceData> sellerGraphItemPriceDatas = new ArrayList<>();
        for(Object[] data : itemPriceData) {
            Long itemId = (Long) data[0];
            String itemName = itemServiceClient.findNameById(itemId);
            Long price = (Long) data[1];
            sellerGraphItemPriceDatas.add(new SellerGraphItemPriceData(itemName, price));
        }

        return sellerGraphItemPriceDatas;
    }
    public Map<String, Long> getGenderData(Long sellerId) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusWeeks(2);
        List<Object[]> genderAndCountData = orderDetailRepository.getGenderCountDataBySellerId(startDate, endDate, sellerId);
        Map<String, Long> result = new HashMap<>();
        for(Object[] data : genderAndCountData) {
            String gender = feignClient.searchGenderByEmail(data[0].toString());
            // gender, count 저장
            result.put(gender, result.getOrDefault(gender, 0L)+(Long)data[1]);
        }
        return result;
    }
    public Map<Byte, Long> getAgeData(Long sellerId) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusWeeks(2);
        List<Object[]> ageAndCountData = orderDetailRepository.getAgeCountDataBySellerId(startDate, endDate, sellerId);
        Map<Byte, Long> result = new HashMap<>();
        for(Object[] data : ageAndCountData) {
            byte age = feignClient.searchAgeByEmail(data[0].toString());
            // age, count 저장
            result.put(age, result.getOrDefault(age, 0L)+(Long)data[1]);
        }
        return result;
    }

    @Transactional
    public void updateReview(Long orderDetailId){
        OrderDetail orderDetail = orderDetailRepository.findById(orderDetailId).orElseThrow(
                ()-> new OrTopiaException(ErrorCode.NOT_FOUND_ORDERDETAIL)
        );
        System.out.println(orderDetailId);
        orderDetail.updateReview();
    }
    @Transactional
    public OrderDetailResponseDto updateOrder(OrderDetailUpdateDto orderDetailUpdateDto) {
        OrderDetail orderDetail = orderDetailRepository.findById(orderDetailUpdateDto.getId()).orElseThrow();
        orderDetail.updateStatue(orderDetailUpdateDto.getStatue());
        return OrderDetailResponseDto.toDto(orderDetail);
    }
}
