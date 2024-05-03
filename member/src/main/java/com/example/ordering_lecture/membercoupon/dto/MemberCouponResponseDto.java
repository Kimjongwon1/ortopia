package com.example.ordering_lecture.membercoupon.dto;

import com.example.ordering_lecture.membercoupon.domain.MemberCoupon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberCouponResponseDto {
    private Long id;
    private Long memberId;
    private Long couponId;
    private String couponDetail;

    public static MemberCouponResponseDto toDto(MemberCoupon memberCoupon) {
        return MemberCouponResponseDto.builder()
                .id(memberCoupon.getId())
                .memberId(memberCoupon.getMemberId())
                .couponId(memberCoupon.getCoupon().getId())
                .couponDetail(memberCoupon.getCoupon().getCouponDetail().getName())
                .build();
    }
}