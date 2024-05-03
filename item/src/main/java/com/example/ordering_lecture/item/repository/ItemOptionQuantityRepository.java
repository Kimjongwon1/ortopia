package com.example.ordering_lecture.item.repository;

import com.example.ordering_lecture.item.dto.SellerGraphStockData;
import com.example.ordering_lecture.item.entity.ItemOptionQuantity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ItemOptionQuantityRepository extends JpaRepository<ItemOptionQuantity, Long> {
    @Query("SELECT ioq FROM ItemOptionQuantity ioq WHERE ioq.item.id = :itemId AND ioq.value1 = :value1 AND ioq.value2 = :value2 AND ioq.value3 = :value3")
    Optional<ItemOptionQuantity> findItemOptionQuantity(
            @Param("itemId") Long itemId,
            @Param("value1") String value1,
            @Param("value2") String value2,
            @Param("value3") String value3
    );
    @Query("SELECT i.id, SUM(ioq.quantity) as totalQuantity FROM ItemOptionQuantity ioq JOIN ioq.item i WHERE i.sellerId = :sellerId AND i.createdTime BETWEEN :startDate AND :endDate GROUP BY i.id")
    List<Object[]> findItemStockBySellerId(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("sellerId") Long sellerId);

    List<ItemOptionQuantity> findAllByItemId(Long id);
}
