package com.evernight.components.redisComponent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RedisComponent {

    // Lua 脚本，用于获取 BitMap 中所有已设置为 1 的位索引
    // 根本性修复：确保按自然顺序返回索引
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
    RedisProperties redisProperties;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    // value 操作

    /**
     * 生成带项目前缀的key
     */
    private String buildKey(String key) {
        return redisProperties.getBaseProjectName() + key;
    }

    /**
     * 设置缓存
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(buildKey(key), value);
    }

    /**
     * 设置缓存并设置过期时间（原子操作）
     */
    public boolean setEx(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(buildKey(key), value, timeout, unit);
            return true;
        } catch (Exception e) {
            log.error("Redis setEx error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 获取缓存
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(buildKey(key));
    }

    /**
     * 获取并设置新值（原子操作）
     */
    public Object getAndSet(String key, Object value) {
        return redisTemplate.opsForValue().getAndSet(buildKey(key), value);
    }

    /**
     * 删除缓存
     */
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(buildKey(key)));
    }

    /**
     * 批量删除缓存
     */
    public long deleteBatch(Collection<String> keys) {
        List<String> fullKeys = new ArrayList<>();
        for (String key : keys) {
            fullKeys.add(buildKey(key));
        }
        Long count = redisTemplate.delete(fullKeys);
        return count != null ? count : 0;
    }

    /**
     * 设置值（如果不存在）（原子操作）
     */
    public boolean setIfAbsent(String key, Object value) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(buildKey(key), value)
        );
    }

    /**
     * 设置值（如果不存在）并设置过期时间（原子操作）
     */
    public boolean setIfAbsentEx(String key, Object value, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(buildKey(key), value, timeout, unit)
        );
    }

    /**
     * 设置值（如果存在）（原子操作）
     */
    public boolean setIfPresent(String key, Object value) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfPresent(buildKey(key), value)
        );
    }

    /**
     * 设置值（如果存在）并设置过期时间（原子操作）
     */
    public boolean setIfPresentEx(String key, Object value, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfPresent(buildKey(key), value, timeout, unit)
        );
    }

    /**
     * 自增操作（原子操作）
     */
    public long increment(String key, long delta) {
        Long result = redisTemplate.opsForValue().increment(buildKey(key), delta);
        return result != null ? result : 0;
    }

    /**
     * 自减操作（原子操作）
     */
    public long decrement(String key, long delta) {
        Long result = redisTemplate.opsForValue().decrement(buildKey(key), delta);
        return result != null ? result : 0;
    }

    /**
     * 设置过期时间
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(buildKey(key), timeout, unit));
    }

    /**
     * 获取过期时间
     */
    public long getExpire(String key, TimeUnit unit) {
        Long expire = redisTemplate.getExpire(buildKey(key), unit);
        return expire != null ? expire : -1;
    }


    // hash 操作

    /**
     * 判断key是否存在
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(key)));
    }

    /**
     * Hash操作：设置Hash字段值
     */
    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(buildKey(key), field, value);
    }

    /**
     * Hash操作：获取Hash字段值
     */
    public Object hGet(String key, String field) {
        return redisTemplate.opsForHash().get(buildKey(key), field);
    }

    /**
     * Hash操作：获取所有Hash字段和值
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(buildKey(key));
    }

    /**
     * Hash操作：批量设置Hash字段值
     */
    public void hPutAll(String key, Map<String, String> values) {
        redisTemplate.opsForHash().putAll(buildKey(key), values);
    }

    /**
     * Hash操作：删除Hash字段
     */
    public void hDelete(String key, Object... fields) {
        redisTemplate.opsForHash().delete(buildKey(key), fields);
    }

    /**
     * Hash操作：判断Hash字段是否存在
     */
    public boolean hExists(String key, String field) {
        return redisTemplate.opsForHash().hasKey(buildKey(key), field);
    }

    /**
     * Hash操作：获取Hash中所有字段
     */
    public Set<Object> hKeys(String key) {
        return redisTemplate.opsForHash().keys(buildKey(key));
    }


    // BitMap 操作

    /**
     * 标记 BitMap 中的指定索引位为 1
     * @param bitmapKey  BitMap 的键（内部会通过 buildKey 添加前缀）
     * @param index      要标记的位索引（从 0 开始）
     * @return true 表示该位之前为 0（本次标记成功），false 表示该位之前已为 1（重复标记）
     */
    public boolean markBitMap(String bitmapKey, int index) {
        // setBit 返回该位原来的值（true=1, false=0），若 key 不存在则创建且旧值默认为 false
        Boolean oldValue = redisTemplate.opsForValue().setBit(buildKey(bitmapKey), index, true);
        // 若旧值为 null 或 false，说明之前未标记（或 key 不存在），返回 true
        return oldValue == null || !oldValue;
    }

    /**
     * 检查 BitMap 中指定索引位是否为 1
     * @param bitmapKey   BitMap 的键（内部会通过 buildKey 添加前缀）
     * @param chunkIndex  要检查的位索引
     * @return true 表示该位为 1，false 表示该位为 0 或 key 不存在
     */
    public boolean isBitMap(String bitmapKey, int chunkIndex) {
        Boolean bit = redisTemplate.opsForValue().getBit(buildKey(bitmapKey), chunkIndex);
        return bit != null && bit;
    }

    /**
     * 统计 BitMap 中值为 1 的位的个数
     * @param bitmapKey  BitMap 的键（内部会通过 buildKey 添加前缀）
     * @return 1 的位数
     */
    public long countBitMap(String bitmapKey) {
        String finalBitmapKey = buildKey(bitmapKey);
        return redisTemplate.execute((RedisCallback<Long>) connection -> {
            if (connection != null) {
                // 使用 BITCOUNT 命令统计
                return (Long) connection.execute("BITCOUNT", finalBitmapKey.getBytes());
            }
            return 0L;
        });
    }

    /**
     * 获取 BitMap 中所有已标记（值为 1）的位索引
     * @param bitmapKey  BitMap 的键（内部会通过 buildKey 添加前缀）
     * @return 索引列表（从 0 开始），若 key 不存在或没有标记位则返回空列表
     */
    public List<Integer> getAllBitMap(String bitmapKey) {
        String finalBitmapKey = buildKey(bitmapKey);

        // 使用 Lua 脚本批量获取所有为 1 的索引
        List<Long> result = redisTemplate.execute((RedisCallback<List<Long>>) connection -> {
            // 执行 Lua 脚本，参数：脚本字节数组、返回类型（多值）、key 数量、key 的字节数组
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

        // 将 Long 转换为 Integer（注意：若索引可能超过 Integer.MAX_VALUE 会有溢出风险）
        return result.stream()
                .map(Long::intValue)
                .collect(Collectors.toList());
    }

}