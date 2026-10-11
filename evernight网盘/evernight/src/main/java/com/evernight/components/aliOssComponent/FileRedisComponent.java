package com.evernight.components.aliOssComponent;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 文件模块Redis操作组件
 * <br>
 * 使用独立的 RedisTemplate（支持 LocalDateTime 序列化）
 */
@Slf4j
@Component
public class FileRedisComponent {

    // Lua 脚本，用于获取 BitMap 中所有已设置为 1 的位索引
    private static final String SCRIPT =
            "local str = redis.call('GET', KEYS[1]) " +
                    "if not str then return {} end " +
                    "local result = {} " +
                    "local byte_count = string.len(str) " +
                    "for byte_idx = 0, byte_count - 1 do " +
                    "    local byte_val = string.byte(str, byte_idx + 1) " +
                    "    for bit_pos = 0, 7 do " +
                    "        if bit.band(byte_val, bit.lshift(1, 7 - bit_pos)) ~= 0 then " +
                    "            table.insert(result, byte_idx * 8 + bit_pos) " +
                    "        end " +
                    "    end " +
                    "end " +
                    "return result";

    @Autowired
    private FileRedisProperties fileRedisProperties;

    @Autowired
    @Qualifier("fileRedisTemplate")
    private RedisTemplate<String, Object> fileRedisTemplate;

    @Autowired
    @Qualifier("fileObjectMapper")
    private ObjectMapper fileObjectMapper;

    // value 操作

    /**
     * 生成带项目前缀的key
     */
    private String buildKey(String key) {
        return fileRedisProperties.getBaseProjectName() + key;
    }

    /**
     * 设置缓存
     */
    public void set(String key, Object value) {
        fileRedisTemplate.opsForValue().set(buildKey(key), value);
    }

    /**
     * 设置缓存并设置过期时间（原子操作）
     */
    public boolean setEx(String key, Object value, long timeout, TimeUnit unit) {
        try {
            fileRedisTemplate.opsForValue().set(buildKey(key), value, timeout, unit);
            return true;
        } catch (Exception e) {
            log.error("FileRedis setEx error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 获取缓存
     */
    public Object get(String key) {
        return fileRedisTemplate.opsForValue().get(buildKey(key));
    }

    /**
     * 获取缓存并安全转换为目标类型
     * <br>
     * 当Redis中的数据缺少@class类型信息时，自动通过ObjectMapper.convertValue进行转换
     */
    public <T> T get(String key, Class<T> type) {
        Object value = get(key);
        if (value == null) return null;
        if (type.isInstance(value)) return type.cast(value);
        return fileObjectMapper.convertValue(value, type);
    }

    /**
     * 获取缓存列表并安全转换元素类型
     * <br>
     * 当Redis中的列表元素为LinkedHashMap时，自动转换为指定的元素类型
     */
    public <T> List<T> getList(String key, Class<T> elementType) {
        Object value = get(key);
        if (value == null) return null;
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(item -> {
                        if (elementType.isInstance(item)) return elementType.cast(item);
                        return fileObjectMapper.convertValue(item, elementType);
                    })
                    .collect(Collectors.toList());
        }
        return null;
    }

    /**
     * 删除缓存
     */
    public boolean delete(String key) {
        return Boolean.TRUE.equals(fileRedisTemplate.delete(buildKey(key)));
    }

    /**
     * 自增操作（原子操作）
     */
    public long increment(String key, long delta) {
        Long result = fileRedisTemplate.opsForValue().increment(buildKey(key), delta);
        return result != null ? result : 0;
    }

    /**
     * 设置过期时间
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(fileRedisTemplate.expire(buildKey(key), timeout, unit));
    }

    /**
     * 判断key是否存在
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(fileRedisTemplate.hasKey(buildKey(key)));
    }

    // hash 操作

    /**
     * Hash操作：设置Hash字段值
     */
    public void hSet(String key, String field, Object value) {
        fileRedisTemplate.opsForHash().put(buildKey(key), field, value);
    }

    /**
     * Hash操作：获取所有Hash字段和值
     */
    public Map<Object, Object> hGetAll(String key) {
        return fileRedisTemplate.opsForHash().entries(buildKey(key));
    }

    // BitMap 操作

    /**
     * 标记 BitMap 中的指定索引位为 1
     */
    public boolean markBitMap(String bitmapKey, int index) {
        Boolean oldValue = fileRedisTemplate.opsForValue().setBit(buildKey(bitmapKey), index, true);
        return oldValue == null || !oldValue;
    }

    /**
     * 统计 BitMap 中值为 1 的位的个数
     */
    public long countBitMap(String bitmapKey) {
        String finalBitmapKey = buildKey(bitmapKey);
        return fileRedisTemplate.execute((RedisCallback<Long>) connection -> {
            if (connection != null) {
                return (Long) connection.execute("BITCOUNT", finalBitmapKey.getBytes());
            }
            return 0L;
        });
    }

    /**
     * 获取 BitMap 中所有已标记（值为 1）的位索引
     */
    public List<Integer> getAllBitMap(String bitmapKey) {
        String finalBitmapKey = buildKey(bitmapKey);

        List<Long> result = fileRedisTemplate.execute((RedisCallback<List<Long>>) connection -> {
            Object evalResult = connection.scriptingCommands().eval(
                    SCRIPT.getBytes(),
                    ReturnType.MULTI,
                    1,
                    finalBitmapKey.getBytes()
            );

            if (evalResult instanceof List<?> resultList) {
                List<Long> longResult = new ArrayList<>();
                for (Object obj : resultList) {
                    if (obj instanceof Number) {
                        longResult.add(((Number) obj).longValue());
                    }
                }
                return longResult;
            }
            return Collections.emptyList();
        });

        if (result == null || result.isEmpty()) {
            return Collections.emptyList();
        }

        return result.stream()
                .map(Long::intValue)
                .collect(Collectors.toList());
    }
}
