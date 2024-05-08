package com.example.ordering_lecture.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RedisService {

    @Qualifier("7")
    private final RedisTemplate<String,Object> redisTemplate7;

    public RedisService(RedisTemplate<String, Object> redisTemplate7) {
        this.redisTemplate7 = redisTemplate7;
    }

    public List<String> getValues(String key) {
        ListOperations<String, Object> values = redisTemplate7.opsForList();
        log.info(key+"의 "+values.size(key)+"개의 저장 된 알람을 불러 왔습니다.");
        return values.range(key, 0, -1).stream().map(Object::toString).collect(Collectors.toList()); // 리스트의 전체 범위를 가져옵니다.
    }
}