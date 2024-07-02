package com.zzy.team.utils;

import java.util.List;

/**
 * 距离编辑算法
 */
public class StringCompareUtil {
    //定义一个similarRates方法获取两个字符串间不同字符的个数并求出两个字符串的相似率
    public static int similarRates(List<String> str1 , List<String> str2){
        //确定二维距离表distance的维度
        int str1Len = str1.size();
        int str2Len = str2.size();
        //如果一个字符串的内容为空就返回另一个字符串的长度
        if (str1Len == 0) return str2Len;
        if (str2Len == 0) return str1Len;
        //定义一张二维距离表distance
        int[][] distance = new int[str1Len + 1][str2Len + 1];

        //给二维数组的第一行第一列赋值
        int maxLen = str1Len > str2Len ? str1Len : str2Len;
        for (int num = 0; num < maxLen + 1; num++){
            if (num<str1Len + 1) distance[num][0] = num;
            if (num<str2Len + 1) distance[0][num] = num;
        }

        /**
         * 补全二维数组除第一行第一列的其他值
         * 行列索引进行对比，相同的话直接取左上方值，不同的话采用最小距离算法
         */
        for (int row = 1; row < str1Len+1; row++){
            String c1 = str1.get(row - 1);
            for (int col = 1; col < str2Len+1; col++){
                String c2 = str2.get(col - 1);
                if (c1.equals(c2)) {
                    distance[row][col] = distance[row - 1][col - 1];
                } else {
                    // 最小距离算法就是，取该元素左上方值、左边值、上边值，找到三个之中的最小值再加1即最终距离
                    distance[row][col] = mostMin(distance[row-1][col], distance[row][col-1], distance[row-1][col-1]) + 1;
                }
            }
        }

        //二维数组中的最后一个元素即是两个字符串间不同字符的个数
        int notSimilarNum = distance[str1Len][str2Len];

        //求出相似率
//        double similarRates = (1- (double)notSimilarNum / maxLen)*100;
//        return (int)similarRates;
        // 在这里我只需要交换次数
        return notSimilarNum;
    }

    //取三个数中的最小值
    public static int mostMin(int up, int left, int upLeft){
        int min = up < left ? up : left;
        min = min < upLeft ? min : upLeft;
        return min;
    }
}
