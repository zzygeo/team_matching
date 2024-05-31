# 项目介绍



伙伴匹配系统，用户可自己创建标签，通过标签组合搜索想要的队友。提供组队功能。

# 表设计

## 标签表

```sql
-- auto-generated definition
create table tag
(
    id          bigint auto_increment comment 'id'
        primary key,
    tag_name    varchar(256)                       null comment '标签名称',
    user_id     bigint                             null comment '用户id',
    parent_id   bigint                             null comment '父标签',
    is_parent   tinyint                            null comment '0表示是父标签 1表示非父标签',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete   tinyint  default 0                 null comment '0表示未删除，1表示删除',
    constraint tag_name_index
        unique (tag_name)
)
    comment '标签表';

create index user_id_index
    on tag (user_id);


```

暂定两层标签结构，可通过父id查询某标签下的所有标签，user_id是一个常用查询的字段，并且重复性不高，设置普通索引，标签名字不能重复，设置unique索引。

## 用户表

```sql
-- auto-generated definition
create table user
(
    username      varchar(256)                         null comment '用户昵称',
    id            bigint auto_increment comment '用户id'
        primary key,
    user_account  varchar(256)                         null comment '用户登录账号',
    avatar_url    varchar(1024)                        null comment '用户头像',
    user_password varchar(512)                         null comment '用户密码',
    phone         varchar(128)                         null comment '用户手机号',
    email         varchar(512)                         null comment '用户邮箱',
    user_status   tinyint(1) default 0                 not null comment '用户状态 0 正常 1 禁用',
    create_time   datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time   datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete     tinyint    default 0                 null comment '0表示未删除，1表示删除',
    user_role     tinyint(1) default 0                 not null comment '用户状态 0 普通用户 1 管理员',
    planet_code   varchar(512)                         null comment '星球编号',
    tags          varchar(1024)                        null comment '用户标签',
    gender        tinyint(1) default 0                 not null comment '性别 0-未知 1-男 2-女'
)
    comment '用户表';


```

常见的用户表结构，不同点是设置了tags字段，可认为tags是用户的一个固有属性，这样做的好处是：

1. 减少了一张关联表，增删实现功能方便。
2. 减少连表操作，通过查询的标签去查询标签表，在内存中实现查询功能，在数据量大的时候可以提高性能。

# 需求分析

## 标签部分

### 添加、修改、删除标签

### 查询满足多个tag的用户。

可以用**sql拼接多个模糊查询**，也可以**先把所有的用户查询出来在代码里过滤**，要**结合数据量**去实践谁快，必要时可以根据用户输出的的tag**参数的多少去选择**查询方式，在内存和资源充足的情况下，甚至可以**同时进行**，谁先快返回哪个结果，也可以通过sql**先筛选出一部分数据**以后再到内存中去计算。

### 查询满足任何一个tag的用户。