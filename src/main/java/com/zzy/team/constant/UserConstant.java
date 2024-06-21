package com.zzy.team.constant;

public interface UserConstant {
    /**
     * 用户登陆session凭证
     */
    String SING_KEY = "123456";

    int DEFAULT_ROLE = 0;

    int ADMIN_ROLE = 1;
    /**
     * 用户推荐前缀字符
     */
    String RECOMMEND_USER = "team_recommend_user:";

    /**
     * 密码加盐凭证
     */
    String PASSWORD_SALT = "1a2b3c4d5e";

    String PRECACHE_JOB_LOCK = "team:precachejob:lock";
}
