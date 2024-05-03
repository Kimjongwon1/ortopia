package com.example.ordering_lecture.order.dto;

import com.example.ordering_lecture.order.entity.Ordering;
import com.example.ordering_lecture.orderdetail.dto.OrderDetailResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderResponseDto {
    private Long id;
    private int totalPrice;
    private Long addressId;
    private String email;
    private String paymentMethod;
    private String recipientName;
    private LocalDateTime createdTime;
    private List<OrderDetailResponseDto> orderDetailResponseDtoList;

    public static OrderResponseDto toDto(Ordering ordering){
        return OrderResponseDto.builder()
                .email(ordering.getEmail())
                .recipientName(ordering.getRecipientName())
                .createdTime(ordering.getCreatedTime())
                .id(ordering.getId())
                .addressId(ordering.getAddressId())
                .totalPrice(ordering.getTotalPrice())
                .paymentMethod(ordering.getPaymentMethod().toString())
                .build();
    }
}
