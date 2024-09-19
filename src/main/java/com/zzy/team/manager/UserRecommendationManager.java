package com.zzy.team.manager;

import cn.hutool.bloomfilter.BitMapBloomFilter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @Author zzy
 * @Description 用户推荐管理，单机版本
 */

public class UserRecommendationManager {
    private final static int KEY = 10;

    private final static Map<Long, BitMapBloomFilter> maps = new HashMap<Long, BitMapBloomFilter>();

    public final static boolean contain(long userId) {
        return maps.containsKey(userId);
    }

    /**
     * 获取某个用户的过滤器
     * @param userId
     * @return
     */
    public final static BitMapBloomFilter get(long userId) {
        return maps.get(userId);
    }

    public final static BitMapBloomFilter create(long userId) {
        BitMapBloomFilter bitMapBloomFilter = new BitMapBloomFilter(KEY);
        maps.put(userId, bitMapBloomFilter);
        return bitMapBloomFilter;
    }
}
