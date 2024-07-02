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

## 队伍表

字段：

id bigint 自增，缺点是爬虫问题

name varchar 队伍名称

description varchar 描述

userId bigint 拥有者

expireTime timestamp 过期时间

maxNum integer 队伍最大上限

createTime timestamp 创建时间

updateTime timestamp 更新时间

isDelete tinyint 逻辑删除

status 0公开 1私有 2加密

password 密码

```sql
create table team (
    id bigint auto_increment comment 'id' primary key ,
    team_name varchar(20) not null comment '队名',
    description varchar(256) default '' comment '描述',
    user_id bigint not null comment '创建者id',
    expire_time timestamp comment '过期时间',
    max_nums int not null comment '最大人数',
    create_time timestamp not null default current_timestamp comment '创建时间',
    update_time timestamp not null default current_timestamp on update current_timestamp comment '更新时间',
    is_delete tinyint not null default 0 comment '是否删除',
    team_status tinyint not null default 0 comment '状态 0表示公开 1表示私有 2表示加密',
    team_password varchar(100) default '' comment '队伍密码'
) comment '队伍表';
```



## 用户-队伍表

id 主键

userId 用户id

teamId 队伍id

joinTime 加入时间

createTime 创建时间

updateTime 更新时间

isDelete 是否删除

```sql
create table user_team (
    id bigint auto_increment comment 'id' primary key ,
    user_id bigint not null comment '用户id',
    team_id bigint not null comment '队伍id',
    join_time timestamp comment '加入时间',
    create_time timestamp not null default current_timestamp comment '创建时间',
    update_time timestamp not null default current_timestamp on update current_timestamp comment '更新时间',
    is_delete tinyint not null default 0 comment '是否删除'
) comment '用户队伍表';
```

# 需求分析

## 标签管理

### 清单

1. 添加标签。
2. 删除标签。
3. 树状查询标签。
4. 修改标签信息。

## 用户管理

### 清单

1. 添加用户。
2. 修改用户信息。
3. 注册用户。
4. 删除用户。
5. 根据标签搜索用户。
6. 用户推荐。

------

### 设计

**查询标签：**

可以用**sql拼接多个模糊查询**，也可以**先把所有的用户查询出来在代码里过滤**，要**结合数据量**去实践谁快，必要时可以根据用户输出的的tag**参数的多少去选择**查询方式，在内存和资源充足的情况下，甚至可以**同时进行**，谁先快返回哪个结果，也可以通过sql**先筛选出一部分数据**以后再到内存中去计算。

**查看用户信息**

如果不是好友，那么只能查看基础信息，如果是好友，能查看联系方式等信息。

## 组队功能

### 清单

1. 用户可以创建一个队伍，设置队伍的人数，标题，描述，过期时间。
2. 队伍信息可以修改，队伍可以设置密码。
3. 队伍里有队长的概念，用户可以加入未满的队伍，但是加入的队伍有上限。
4. 队员可以退出队伍，如果是队长退出顺位给下一个用户。
5. 队长可以解散队伍。
6. 分享队伍、邀请人加入队伍。
7. 队伍人满以后发送消息通知。
8. 一个用户最多创建5个队伍。

### 设计

**创建队伍**

1. 参数不能为空。
2. 用户需要登陆。
3. 队伍名称不能为空，最大人数不能小于1且不能超过20，描述信息不能超过256，队伍名称不能超过20，队伍类型为int,如果密码有的话小于32，超时时间必须大于当前时间，用户创建的队伍数是否小于5。
4. 设置userId。

------

**查询队伍列表**

1. 用户名不为空的话，就作为查询条件。
2. 不展示过期的队伍，即根据队伍的过期时间对队伍进行筛选。
3. 查询队伍列表时，展示队长的消息。
4. 分页查询。
5. 只有管理员才能查到非公开和加密的房间。

------

**修改队伍信息**

1. 队伍不能为空，队伍id不能为空。
2. 只有管理员或者队伍的拥有者才能修改队伍信息。
3. 如果要修改的队伍不存在，则直接返回。
4. 如果传过来的队伍信息和数据库里的数据信息一致，则不修改，节省数据库的性能。
5. 如果修改队伍的状态为加密，则必须设置队伍密码

------

**加入队伍**

1. 最多加入5个队伍。
2. 加入的队伍必须是未过期且存在的。
3. 不允许加入私有的队伍。
4. 不能加入重复的队伍（幂等性）。
5. 如果加入的是加密类型的队伍，则密码要匹配。
6. 添加用户队伍的关联关系。

------

**退出队伍**

1. 参数校验。
2. 如果队伍只剩一人，则删除队伍。
3. 如果是队长退出队伍，则把队长交给第二个加入队伍的人。
4. 如果是成员退出，则直接退出。

------

**解散队伍**

1. 校验请求参数。
2. 判断队伍是否存在。
3. 判断是不是队长。
4. 移除该队伍的关联关系。
5. 退出队伍。

------

**获取当前用户已加入的队伍**

1. 参数校验。
2. 复用获取用户列表的接口

**获取当前用户创建的队伍**

1. 参数校验。
2. 先查询队伍用户关系表，拿到teamIds。
3. 复用获取用户列表的接口，并新增teamIds的查询条件并。

## 随机匹配

目的是为了用户更快的发现和自己兴趣相同的队伍。

简化为找到有相似标签的用户。

### 设计

1. 找到有共同标签最多的用户。
2. 相似标签越多，分数越高，优先级越高。
3. 如果没有找到相似用户，则随机匹配。
4. 匹配设置上限，范围在1-20。

------

**怎么对所有用户匹配，取分数最高。**

对于直接取出所有用户的操作，不建议，以下是些优化点：

1. 切忌不要在数据量大的时候循环输出日志。
2. map存了所有的分数信息，**占用内存**，解决方案是按分数降序维护一个固定长度的集合。
3. 减少用户的查询量: **过滤**掉没有标签的用户，**根据部分标签取用户**（热点标签），只查需要的数据（no select *)。
4. 提前把所有用户缓存起来：比如redis的布隆过滤器，提前运算出结果，缓存。

### 算法推荐

简单匹配单词相似度，采用[编辑距离算法](https://labuladong.online/algo/dynamic-programming/edit-distance/)。

带权重的匹配，采用[余弦相似度算法](https://blog.csdn.net/qq_36488175/article/details/109787805)。

# 待办

1. 组队聊天室功能，队伍里的人可以在队伍聊天室里聊天。p2
2. 查看用户信息功能优化，在一个队伍里人能够查看联系方式等信息，否则只能查看基础信息。p0
3. 邀请用户加入队伍功能。p1
4. 标签功能优化，固定标签，用户在创建时只能选择固定标签里的一部分（校验）在搜索用户的时候，标签树从固定的标签表返回。p0
5. 登陆页面添加用户注册按钮。p0

# 知识点

## 分布式锁

### 引入分布式锁

假如现在在项目里指定了每天0点执行的定时任务，由于项目部署了多个后端服务，那么在0点时执行一样的任务，会出现以下的问题：

1. 资源占用，多台机器执行了一样的任务，浪费资源。
2. 出现脏数据，假如是插入数据的情况，可能会造成重复插入，产生脏数据。

------

解决方案：

1. 将定时任务和接口代码分开，保证只有一套机器运行定时任务，这样做的缺点是要拆成两套代码，不方便。
2. 写死配置，直接在代码里写死配置，耦合性太高。
3. 动态配置，比如在定时任务的执行代码里增加ip的判断逻辑，只有特定ip的机器才可以执行定时任务，这个ip最好是配置在外部，比如数据库、redis、配置中心，这样可以在**不重启服务**的情况下动态修改配置，这是一种非常常用的操作，但是当**服务过多以后，ip可能不可控**。
4. **分布式锁**，只有抢到锁的服务器才能执行业务逻辑。坏处：增加成本，好处：不用手动的修改配置，多少个服务器都一样。

#### 锁

在有限资源的情况下，控制同一时间（段），只有某些线程（用户/服务器）能访问到资源。

java实现锁的方式：synchronized关键字、并发包的类。

存在的问题：只对单个的jvm有效。

#### 分布式锁

#### 为什么需要分布式锁

1. 在有限资源的情况下，控制同一时间段只有某些线程（用户/服务器）能访问到资源，比如定时任务的插入数据情景。
2. 为什么需要分布式，因为单个锁只对单个jvm有效。

#### 分布式锁实现的关键

##### 抢锁机制：

怎么保证同一时间只有一个服务器能抢到锁？

核心思想：先来的人把数据改成自己的标识（比如服务器ip）后来的人发现标识已存在，就抢锁失败，继续等待。等先来的人执行方法结束，把标识清空，其他人继续抢锁。

#### 实现方法

##### mysql数据库行级锁

select for update（最简单的实现方式），保证查询的原子性。

##### mysql乐观锁

##### redis实现

redis的setnx操作是原子性的，可以编写lua脚本执行，并且redis是内存数据库，性能高，比较推荐这种方式。

------

**注意事项**：

1. 加完锁以后一定要记得**释放锁**，把位置给腾出来。
2. 一定记得**加过期时间**，即使锁没有正常释放也不会一直占有锁。
3. 如果方法的执行时间过长，**锁提前过期了**，这样还是会存在**多个方法同时执行**的情况，并且可能出现**连锁效应**（即锁过期，b任务进来执行，同时加了一把锁，a任务这时执行完毕把b的锁给释放了，c任务又进来了）。

------

**怎么解决连锁效应**：

在加锁的时候加上任务的标识符，在释放锁的时候判断是不是锁的标识符是不是本任务的。

只用以上的判断还是**可能会出现连锁效应**，比如以下的情景：

```java
// a 执行
if（getLock == a) {
	// 在判断完以后，正好lock过期了，lock设置为b 
	// set lock = b;
	// 这里删除了b添加的锁，触发连锁效应
	del lock;
}
```

因此有必要将上面的整个代码提升为原子操作，需要利用redis + lua脚本实现。

------

**怎么解决锁的过期问题：**

在另一个任务进来执行的时候，先判断任务是否执行完了，如果没有执行完，则对值进行**续期**，这个判断依据可以**设置一个共享的状态值**。

#### redisson工具

上面的redis实现分布式锁，需要自己setnx键值，过期时间，续期，结合lua脚本判断删除锁逻辑，redisson就可以实现这样的功能。

##### redisson的定义

redisson是一款**java操作redis的客户端**，提供了大量**分布式**数据集来简化对redis的操作，让开发者**像使用本地集合一样使用redis**，甚至完全感知不到redis的存在。

##### 注意点

redisson释放锁的机制一定要放在finnal代码块里，确保代码执行异常以后锁还是会被释放掉。

##### 看门狗机制

如果过期时间设置为大于0，先设置30s过期的一个锁。

```java
 CompletionStage<Boolean> acquiredFuture;
        if (leaseTime > 0) {
            acquiredFuture = tryLockInnerAsync(waitTime, leaseTime, unit, threadId, RedisCommands.EVAL_NULL_BOOLEAN);
        } else {
            acquiredFuture = tryLockInnerAsync(waitTime, internalLockLeaseTime,
                    TimeUnit.MILLISECONDS, threadId, RedisCommands.EVAL_NULL_BOOLEAN);
        }
```

如果如果过期时间设置为大于0，则不启用看门狗机制。

```java
CompletionStage<Boolean> f = acquiredFuture.thenApply(acquired -> {
            // lock acquired
            if (acquired) {
                if (leaseTime > 0) {
                    internalLockLeaseTime = unit.toMillis(leaseTime);
                } else {
                    scheduleExpirationRenewal(threadId);
                }
            }
            return acquired;
        });
```

如果线程是debug的中断状态则取消刷新策略，否则采用刷新策略。

```java
protected void scheduleExpirationRenewal(long threadId) {
        ExpirationEntry entry = new ExpirationEntry();
        ExpirationEntry oldEntry = EXPIRATION_RENEWAL_MAP.putIfAbsent(getEntryName(), entry);
        if (oldEntry != null) {
            oldEntry.addThreadId(threadId);
        } else {
            entry.addThreadId(threadId);
            try {
                renewExpiration();
            } finally {
                if (Thread.currentThread().isInterrupted()) {
                    cancelExpirationRenewal(threadId, null);
                }
            }
        }
    }
```

递归执行刷新任务，直到这个线程不存在了，可以看到是设置了过期时间的1/3的时间进行刷新，这个默认的过期时间30s可以在Config配置里修改。

```java
Timeout task = getServiceManager().newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                ExpirationEntry ent = EXPIRATION_RENEWAL_MAP.get(getEntryName());
                if (ent == null) {
                    return;
                }
                Long threadId = ent.getFirstThreadId();
                if (threadId == null) {
                    return;
                }
                
                CompletionStage<Boolean> future = renewExpirationAsync(threadId);
                future.whenComplete((res, e) -> {
                    if (e != null) {
                        log.error("Can't update lock {} expiration", getRawName(), e);
                        EXPIRATION_RENEWAL_MAP.remove(getEntryName());
                        return;
                    }
                    
                    if (res) {
                        // reschedule itself
                        renewExpiration();
                    } else {
                        cancelExpirationRenewal(null, null);
                    }
                });
            }
        }, internalLockLeaseTime / 3, TimeUnit.MILLISECONDS);
```

##### 分布式锁的数据不一致问题

假如我们的redis服务也是集群模式的，虽然设置了redis的主从模式，但是也不能保证数据完全一致，假如第一次设置锁到了redis a上，另外一个线程去获取锁的时候如果是从redis b上获取的，这两数据不一致，导致两个线程都拿到锁执行。

可以采用redis的**红锁机制**，redisson判断redis是集群模式的话，自动拿到的就是红锁。

