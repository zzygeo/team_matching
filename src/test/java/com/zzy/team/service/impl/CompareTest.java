package com.zzy.team.service.impl;

import com.zzy.team.utils.StringCompareUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

import java.util.List;

@SpringBootTest
public class CompareTest {

    @Test
    public void test() {
        List<String> str1 = List.of("java", "大一", "男");
        List<String> str2 = List.of("java", "大二", "男");
        List<String> str3 = List.of("java", "大二", "女");

        int i = StringCompareUtil.similarRates(str1, str2);
        int i2 = StringCompareUtil.similarRates(str1, str3);
        Assert.isTrue(i == 1);
        Assert.isTrue(i2 == 2);
    }
 }
