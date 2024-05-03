package com.example.ordering_lecture.orderdetail.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class BuyerGraphPriceData {
    private Date createdTime;
    private Long price;
}