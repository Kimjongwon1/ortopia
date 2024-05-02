package com.example.ordering_lecture.item.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.example.ordering_lecture.common.ErrorCode;
import com.example.ordering_lecture.common.OrTopiaException;
import com.example.ordering_lecture.item.controller.MemberServiceClient;
import com.example.ordering_lecture.item.dto.*;
import com.example.ordering_lecture.item.entity.*;
import com.example.ordering_lecture.item.repository.*;
import com.example.ordering_lecture.redis.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ItemService {
    private final ItemRepository itemRepository;
    private final RedisService redisService;
    private final AmazonS3Client amazonS3Client;
    private final MemberServiceClient memberServiceClient;
    private final ItemOptionDetailRepository itemOptionDetailRepository;
    private final ItemOptionRepository itemOptionRepository;
    private final ItemOptionQuantityRepository itemOptionQuantityRepository;
    private final LoveItemRepository loveItemRepository;
    public ItemService(ItemRepository itemRepository, RedisService redisService, AmazonS3Client amazonS3Client, MemberServiceClient memberServiceClient, ItemOptionDetailRepository itemOptionDetailRepository, ItemOptionRepository itemOptionRepository, ItemOptionQuantityRepository itemOptionQuantityRepository, LoveItemRepository loveItemRepository) {
        this.itemRepository = itemRepository;
        this.redisService = redisService;
        this.amazonS3Client = amazonS3Client;
        this.memberServiceClient = memberServiceClient;
        this.itemOptionDetailRepository = itemOptionDetailRepository;
        this.itemOptionRepository = itemOptionRepository;
        this.itemOptionQuantityRepository = itemOptionQuantityRepository;
        this.loveItemRepository = loveItemRepository;
    }
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public ItemResponseDto createItem(ItemRequestDto itemRequestDto,List<OptionRequestDto> optionRequestDtos ,String email) throws OrTopiaException {
        String fileName = itemRequestDto.getName() + System.currentTimeMillis();
        String fileUrl = null;
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(itemRequestDto.getImagePath().getContentType());
            metadata.setContentLength(itemRequestDto.getImagePath().getSize());
            amazonS3Client.putObject(bucket, fileName, itemRequestDto.getImagePath().getInputStream(), metadata);
            fileUrl = amazonS3Client.getUrl(bucket, fileName).toString();
            Long sellerId = memberServiceClient.searchIdByEmail(email);
            log.info("fein client result " + sellerId.toString());
            log.info("file url " + fileUrl);
            Item item = itemRequestDto.toEntity(fileUrl, sellerId);
            itemRepository.save(item);
            // 옵션이 존재하는 경우
            if(!optionRequestDtos.isEmpty()){
                List<List<String>> allOptionValue = new ArrayList<>();
                int index = 0;
                for(OptionRequestDto optionRequestDto :optionRequestDtos){
                    allOptionValue.add(new ArrayList<>());
                    ItemOption itemOption = ItemOption.builder()
                            .name(optionRequestDto.getOptionName())
                            .item(item)
                            .build();
                    itemOptionRepository.save(itemOption);
                    for(String value:optionRequestDto.getDetails()){
                        ItemOptionDetail itemOptionDetail = ItemOptionDetail.builder()
                                .itemOption(itemOption)
                                // 옵션의 이름
                                .value(value)
                                .build();
                        allOptionValue.get(index).add(value);
                        itemOptionDetailRepository.save(itemOptionDetail);
                    }
                    index++;
                }
                fillAllOption(item,allOptionValue,new ArrayList<>(),allOptionValue.size(),0);
            }else{
                ItemOptionQuantityDto itemOptionQuantityDto = new ItemOptionQuantityDto();
                ItemOptionQuantity itemOptionQuantity = itemOptionQuantityDto.toEntity(item);
                itemOptionQuantityRepository.save(itemOptionQuantity);
                //재고를 레디스에 저장
                redisService.setItemQuantity(itemOptionQuantity.getId(),item.getStock());
            }
            return ItemResponseDto.toDto(item);
        } catch (Exception e) {
            e.printStackTrace();
            throw new OrTopiaException(ErrorCode.S3_SERVER_ERROR);
        }
    }

    // 받아온 옵션의 모든 경우의 수를 계산하여 DB에 저장.
    public void fillAllOption(Item item, List<List<String>> allOptionValue,List<String> nowOption,int maxDepth,int nowDepth){
        if(maxDepth == nowDepth){
            ItemOptionQuantityDto itemOptionQuantityDto = new ItemOptionQuantityDto();
            for(int i=0;i<nowDepth;i++){
                itemOptionQuantityDto.setValue(i,nowOption.get(i));
            }
            ItemOptionQuantity itemOptionQuantity = itemOptionQuantityDto.toEntity(item);
            itemOptionQuantityRepository.save(itemOptionQuantity);
            redisService.setItemQuantity(itemOptionQuantity.getId(),item.getStock());
            return;
        }
        for(String option : allOptionValue.get(nowDepth)){
            nowOption.add(option);
            fillAllOption(item,allOptionValue,nowOption,maxDepth,nowDepth+1);
            nowOption.remove(nowOption.size()-1);
        }
    }

    public List<ItemResponseDto> showAllItem(String email){
        if(email.equals("noLogin")){
            return itemRepository.findAll().stream()
                    .map(ItemResponseDto::toDto)
                    .collect(Collectors.toList());
        }
        List<Item> items = itemRepository.findAll();
        List<ItemResponseDto> itemResponseDtos = new ArrayList<>();
        for(Item item : items){
            ItemResponseDto itemResponseDto = ItemResponseDto.toDto(item);
            if(loveItemRepository.findByItemIdAndEmail(item.getId(),email).isPresent()){
                itemResponseDto.setLove(true);
            }
            itemResponseDtos.add(itemResponseDto);
        }
        return itemResponseDtos;
    }

    @Transactional
    public ItemResponseDto updateItem(Long id, ItemUpdateDto itemUpdateDto) {
        Item item = itemRepository.findById(id).orElseThrow(()->new OrTopiaException(ErrorCode.NOT_FOUND_ITEM));
        if(!itemUpdateDto.getImagePath().isEmpty()){
            String fileUrl = item.getImagePath();
            String splitStr = ".com/";
            amazonS3Client.deleteObject(
                    new DeleteObjectRequest(bucket,fileUrl.substring(fileUrl.lastIndexOf(splitStr)+splitStr.length())));
            String fileName = itemUpdateDto.getName()+System.currentTimeMillis();
            fileUrl = null;
            try {
                ObjectMetadata metadata= new ObjectMetadata();
                metadata.setContentType(itemUpdateDto.getImagePath().getContentType());
                metadata.setContentLength(itemUpdateDto.getImagePath().getSize());
                amazonS3Client.putObject(bucket,fileName,itemUpdateDto.getImagePath().getInputStream(),metadata);
                fileUrl = amazonS3Client.getUrl(bucket,fileName).toString();
            } catch (Exception e) {
                throw new OrTopiaException(ErrorCode.S3_SERVER_ERROR);
            }
            item = itemUpdateDto.toUpdate(item,fileUrl);
        }else{
            item = itemUpdateDto.toUpdate(item,item.getImagePath());
        }
        return ItemResponseDto.toDto(item);
    }

    public void deleteItem(Long id) {
        Item item = itemRepository.findById(id).orElseThrow(
                ()-> new OrTopiaException(ErrorCode.NOT_FOUND_ITEM)
                );
        itemRepository.deleteById(id);
        String fileUrl = item.getImagePath();
        String splitStr = ".com/";
        amazonS3Client.deleteObject(
                new DeleteObjectRequest(bucket,fileUrl.substring(fileUrl.lastIndexOf(splitStr)+splitStr.length())));
//        Item item = itemRepository.findById(id).orElseThrow();
//        item.deleteItem();
    }

    public List<ItemResponseDto> banItem(Long sellerId)throws OrTopiaException{
        List<Item> items = itemRepository.findAllBySellerId(sellerId);
        if(items.isEmpty()){
            throw new OrTopiaException(ErrorCode.EMPTY_ITEMS);
        }
        for(Item item : items){
            item.banItem();
        }
        return items.stream()
                .map(ItemResponseDto::toDto)
                .collect(Collectors.toList());
    }

    public List<ItemResponseDto>  releaseBanItem(Long sellerId)throws OrTopiaException{
        List<Item> items = itemRepository.findAllBySellerId(sellerId);
        if(items.isEmpty()){
            throw new OrTopiaException(ErrorCode.EMPTY_ITEMS);
        }
        for(Item item : items){
            item.releaseBanItem();
        }
        return items.stream()
                .map(ItemResponseDto::toDto)
                .collect(Collectors.toList());
    }

    public List<ItemResponseDto> findItemByEmail(String email)throws OrTopiaException {
        Long sellerId = memberServiceClient.searchIdByEmail(email);
        List<Item> items = itemRepository.findAllBySellerId(sellerId);
        if(items.isEmpty()){
            throw new OrTopiaException(ErrorCode.EMPTY_ITEMS);
        }
        return items.stream()
                .map(ItemResponseDto::toDto)
                .collect(Collectors.toList());
    }

    public ItemResponseDto readItem(Long id, String email) {
        Item item = itemRepository.findById(id).orElseThrow(
                ()->new OrTopiaException(ErrorCode.NOT_FOUND_ITEM)
                );
        ItemResponseDto itemResponseDto = ItemResponseDto.toDto(item);
        // 최근본 상품을 저장하기 위함
        if(!email.equals("noLogin")) {
            redisService.setValues(email,itemResponseDto);
        }
        // 옵션을 불러옴
        List<ItemOption> itemOptions = itemOptionRepository.findAllByItemId(id);
        if(!itemOptions.isEmpty()){
            for(ItemOption itemOption : itemOptions){
                ItemOptionResponseDto itemOptionResponseDto = new ItemOptionResponseDto();
                itemOptionResponseDto.setName(itemOption.getName());
                List<ItemOptionDetail> itemOptionDetails = itemOptionDetailRepository.findAllByItemOptionId(itemOption.getId());
                for(ItemOptionDetail itemOptionDetail : itemOptionDetails){
                    itemOptionResponseDto.getValue().add(itemOptionDetail.getValue());
                }
                itemResponseDto.getItemOptionResponseDtoList().add(itemOptionResponseDto);
            }
        }
        // 멤버가 좋아하는지 확인
        if(loveItemRepository.findByItemIdAndEmail(id,email).isPresent()){
            itemResponseDto.setLove(true);
        }
        return itemResponseDto;
    }

    public List<ItemResponseDto> readRecentItems(String email) {
        Set<String> set = redisService.getValues(email);
        List<ItemResponseDto> itemResponseDtos = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        for(String str: set){
            ItemResponseDto  itemResponseDto = null;
            try {
                itemResponseDto = objectMapper.readValue(str, ItemResponseDto.class);
            } catch (JsonProcessingException e) {
                throw new OrTopiaException(ErrorCode.JSON_PARSE_ERROR);
            }
            itemResponseDtos.add(itemResponseDto);
        }
        return itemResponseDtos;
    }

    public String getImagePath(Long itemId) {
        Item item = itemRepository.findImagePathById(itemId).orElseThrow(()->new OrTopiaException(ErrorCode.NOT_FOUND_ITEM));
        return item.getImagePath();
    }

    public List<RecommendationRedisData> findRecommendItem(String email) {
        Long id = memberServiceClient.searchIdByEmail(email);
        List<String> list = redisService.getValues2(id);
        List<RecommendationRedisData> recommendationRedisDatas = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        for(String str: list){
            RecommendationRedisData  recommendationRedisData = null;
            try {
                recommendationRedisData = objectMapper.readValue(str, RecommendationRedisData.class);
            } catch (JsonProcessingException e) {
                throw new OrTopiaException(ErrorCode.JSON_PARSE_ERROR);
            }
            recommendationRedisDatas.add(recommendationRedisData);
        }
        return recommendationRedisDatas;
    }

    public ItemResponseDto readItemForMyPage(Long itemId, String email) {
        Item item = itemRepository.findImagePathById(itemId).orElseThrow(() -> new OrTopiaException(ErrorCode.NOT_FOUND_ITEM));
        ItemResponseDto itemResponseDto = ItemResponseDto.toDto(item);
        return itemResponseDto;
    }

    public Long searchIdByOptionDetail(Long itemId, List<String> values) {
        String value1 = "NONE";
        String value2 = "NONE";
        String value3 = "NONE";
        if(values.size()==1){
            value1 = values.get(0);
        }
        if(values.size()==2){
            value1 = values.get(0);
            value2 = values.get(1);
        }
        if(values.size()==3){
            value1 = values.get(0);
            value2 = values.get(1);
            value3 = values.get(2);
        }
        ItemOptionQuantity itemOptionQuantity = itemOptionQuantityRepository.findItemOptionQuantity(itemId,value1,value2,value3).orElseThrow(
                () -> new OrTopiaException(ErrorCode.NOT_FOUND_OPTION)
        );
        return itemOptionQuantity.getId();
    }

    public List<ItemResponseForSellerDto> findMyAllItem(String email) {
        Long sellerId = memberServiceClient.searchIdByEmail(email);
        List<Item> items = itemRepository.findAllBySellerId(sellerId);
        if(items.isEmpty()){
            throw new OrTopiaException(ErrorCode.NOT_FOUND_ITEM);
        }
        List<ItemResponseForSellerDto> itemResponseDtos = new ArrayList<>();
        for(Item item:items){
            ItemResponseForSellerDto itemResponseDto = ItemResponseForSellerDto.toDto(item);
            List<ItemOptionQuantity> itemOptionQuantities = itemOptionQuantityRepository.findAllByItemId(item.getId());
            for(ItemOptionQuantity itemOptionQuantity : itemOptionQuantities){
                itemResponseDto.getItemOptionQuantityResponseDtos().add(ItemOptionQuantityResponseDto.toDto(itemOptionQuantity));
            }
            List<ItemOption> itemOptions = itemOptionRepository.findAllByItemId(item.getId());
            for(ItemOption itemOption :itemOptions){
                itemResponseDto.getOptionName().add(itemOption.getName());
            }
            itemResponseDtos.add(itemResponseDto);
        }

        return itemResponseDtos;
    }

    @Transactional
    public void updateQuantity(ItemOptionQuantityDto itemOptionQuantityDto) {
        ItemOptionQuantity itemOptionQuantity = itemOptionQuantityRepository.findById(itemOptionQuantityDto.getId()).orElseThrow(
                () -> new OrTopiaException(ErrorCode.NOT_FOUND_OPTION)
        );
        //DB 내 아이템 조정
        itemOptionQuantity.updateQuantity(itemOptionQuantityDto.getQuantity());
        // redis 내 아이템 수량 조정
        redisService.setItemQuantity(itemOptionQuantity.getId(),itemOptionQuantityDto.getQuantity());
    }

    public String loveAndDisLoveItem(String email, Long itemId) {
        Optional<LoveItem> optionalLoveItem =  loveItemRepository.findByItemIdAndEmail(itemId,email);
        if(optionalLoveItem.isEmpty()){
            Item item = itemRepository.findById(itemId).orElseThrow(
                    ()-> new OrTopiaException(ErrorCode.NOT_FOUND_ITEM)
            );
            LoveItem loveItem = LoveItem.builder()
                    .email(email)
                    .item(item)
                    .build();
            loveItemRepository.save(loveItem);
            return "save success";
        }
        loveItemRepository.deleteById(optionalLoveItem.get().getId());
        return "delete success";
    }

    public String getItemName(Long itemId) {
        Item item = itemRepository.findImagePathById(itemId).orElseThrow(()->new OrTopiaException(ErrorCode.NOT_FOUND_ITEM));
        return item.getName();
    }

    public List<SellerGraphStockData> getEachItemStockData(Long sellerId) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusWeeks(2);
        List<Object[]> ageAndCountData = itemOptionQuantityRepository.findItemStockBySellerId(startDate, endDate, sellerId);
        List<SellerGraphStockData> result = new ArrayList<>();
        for(Object[] data : ageAndCountData) {
            System.out.println("data[0] = " + data[0]);
            System.out.println("data[1] = " + data[1]);
            String itemName = itemRepository.findById((Long) data[0]).orElseThrow(() -> new OrTopiaException(ErrorCode.NOT_FOUND_ITEM)).getName();
            // age, count 저장
            SellerGraphStockData sellerGraphStockData = new SellerGraphStockData(itemName, (Long) data[1]);
            result.add(sellerGraphStockData);
        }
        return result;
    }
}
