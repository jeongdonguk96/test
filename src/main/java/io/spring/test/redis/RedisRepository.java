package io.spring.test.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.test.entity.Dummy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class RedisRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisTemplate<byte[], byte[]> redisTemplate2;
    private final RedisTemplate<String, Dummy> redisTemplate3;
    private final ObjectMapper objectMapper;

    long startTime;
    long endTime;
    long timeDiff;
    double transactionTime;

    // redis에 데이터를 저장한다.
    public void insert(Dummy dummy) throws JsonProcessingException {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        String jsonString = objectMapper.writeValueAsString(dummy);
        valueOperations.set(String.valueOf(dummy.getId()), jsonString);
    }

    // redis에 데이터를 벌크로 저장한다.
    public void insertMany(List<Dummy> dummyList) {
        RedisSerializer keySerializer = redisTemplate2.getStringSerializer();
        RedisSerializer valueSerializer = redisTemplate2.getValueSerializer();

        redisTemplate2.executePipelined((RedisCallback<?>) connection -> {
            for (Dummy dummy : dummyList) {
                String key = String.valueOf(dummy.getId());
                String value = null;
                try {
                    value = objectMapper.writeValueAsString(dummy);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                connection.set(keySerializer.serialize(key), valueSerializer.serialize(value));
            }

            return null;
        });
    }

    // redis에서 데이터를 키로 조회한다.
    public void find(String id) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        startTime = System.currentTimeMillis();
        String result = valueOperations.get(id);
        endTime = System.currentTimeMillis();

        timeDiff = (endTime - startTime);
        transactionTime = timeDiff / 1000.0;
        System.out.println("id = " + id + ", ========== REDIS TRX TIME = { " + transactionTime + "}s ==========");
        System.out.println("redis result = " + result);
        System.out.println();
    }

    // redis에서 전체 데이터를 조회한다.
    public void findAll() {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        ValueOperations<String, Dummy> valueOperations2 = redisTemplate3.opsForValue();
        List<Dummy> dummyList;

        startTime = System.currentTimeMillis();
        Set<String> keys = redisTemplate.keys("*");
        endTime = System.currentTimeMillis();
        timeDiff = (endTime - startTime);
        transactionTime = timeDiff / 1000.0;
        System.out.println("========== REDIS TRX TIME = { " + transactionTime + "}s ==========");
        System.out.println("REDIS SELECT SIZE = " + Objects.requireNonNull(keys).size());
        System.out.println("data = " + valueOperations2.get("1"));;

        startTime = System.currentTimeMillis();
        dummyList = Objects.requireNonNull(keys).stream()
                .map(valueOperations2::get)
                .filter(Objects::nonNull)
                .toList();

        endTime = System.currentTimeMillis();
        timeDiff = (endTime - startTime);
        transactionTime = timeDiff / 1000.0;
        System.out.println("========== MAPPING TRX TIME = { " + transactionTime + "}s ==========");
        System.out.println("List Size = " + dummyList.size());

        System.out.println();
    }

}
