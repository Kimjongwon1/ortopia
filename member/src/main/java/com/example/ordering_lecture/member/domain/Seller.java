package com.example.ordering_lecture.member.domain;

import com.example.ordering_lecture.member.dto.Seller.SellerUpdateDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Seller {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String businnessNumber;

    @Column(nullable = false)
    private String companyName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusinnessType businnessType;

    @Column
    @Builder.Default
    private Long totalScore = 0L;

    @Column(nullable = false)
    @Builder.Default
    private boolean delYN = false;

    @OneToOne
    @JoinColumn(nullable = false) //DB에 member_id
    private Member member;

    @OneToOne(mappedBy = "seller", cascade = CascadeType.PERSIST)
    private BannedSeller bannedSeller;

    @OneToMany(mappedBy = "seller", cascade = CascadeType.PERSIST)
    private List<LikedSeller> likedSellers;

    public void updateSeller(SellerUpdateDto sellerUpdateDto){
        if(sellerUpdateDto.getBusinnessNumber() != null){
            this.businnessNumber = sellerUpdateDto.getBusinnessNumber();
        }
        if(sellerUpdateDto.getCompanyName()!= null){
            this.companyName = sellerUpdateDto.getCompanyName();
        }
        if(sellerUpdateDto.getBusinnessType()!= null){
            this.businnessType = sellerUpdateDto.getBusinnessType();
        }
    }
    public void updateTotalScore(Long score){
        this.totalScore += score;
    }
    public void deleteSeller(){
        this.delYN = true;
    }
}