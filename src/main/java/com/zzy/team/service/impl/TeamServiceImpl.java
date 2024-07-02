package com.zzy.team.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzy.team.constant.ErrorStatus;
import com.zzy.team.constant.TeamConstant;
import com.zzy.team.enums.TeamStatusEnum;
import com.zzy.team.exception.BusinessException;
import com.zzy.team.model.domain.Team;
import com.zzy.team.model.domain.User;
import com.zzy.team.model.domain.UserTeam;
import com.zzy.team.model.request.TeamJoinRequest;
import com.zzy.team.model.request.TeamListRequest;
import com.zzy.team.model.request.TeamPageRequest;
import com.zzy.team.model.request.TeamUpdateRequest;
import com.zzy.team.model.vo.TeamUser;
import com.zzy.team.model.vo.UserVo;
import com.zzy.team.service.TeamService;
import com.zzy.team.service.UserService;
import com.zzy.team.service.UserTeamService;
import com.zzy.team.service.mapper.TeamMapper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author zzy
 * @description 针对表【team(队伍表)】的数据库操作Service实现
 * @createDate 2024-06-21 10:39:26
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    @Autowired
    private UserTeamService userTeamService;
    @Autowired
    private UserService userService;
    @Resource
    private RedissonClient redissonClient;

    @Override
    // 表示事务失败时，应该抛出什么样的异常
    @Transactional(rollbackFor = Exception.class)
    public Long addTeam(Team team, User loginUser) {
        if (team == null) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR);
        }
        if (loginUser == null) {
            throw new BusinessException(ErrorStatus.UNAUTHORIZED_ERROR);
        }

        int maxNums = Optional.ofNullable(team.getMaxNums()).orElse(0);
        if (maxNums < 1 || maxNums > 20) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "队伍人数必须在1-20之间");
        }

        if (StringUtils.isBlank(team.getTeamName()) || team.getTeamName().length() > 20) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "队伍名称参数错误");
        }

        if (StringUtils.isNotEmpty(team.getDescription()) && team.getDescription().length() > 256) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "队伍描述不能超过256个字符");
        }
        int teamStatus = Optional.ofNullable(team.getTeamStatus()).orElse(0);
        TeamStatusEnum anEnum = TeamStatusEnum.getEnum(teamStatus);
        if (anEnum == null) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "队伍状态参数错误");
        }
        if (TeamStatusEnum.PASSWROD.equals(anEnum) && (StringUtils.isEmpty(team.getTeamPassword()) || team.getTeamPassword().length() > 32)) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "密码参数错误");
        }
        if (team.getExpireTime() == null) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "过期时间不能为空");
        }
        if (team.getExpireTime().getTime() < System.currentTimeMillis()) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "过期时间不能早于当前时间");
        }
        // 判断创建的队伍数是否小于5, todo 这里判断大于5是有bug的
        LambdaQueryWrapper<Team> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Team::getUserId, loginUser.getId());
        long count = this.count(wrapper);
        if (count >= 5) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "创建的队伍数不能超过5个");
        }
        team.setUserId(loginUser.getId());
        boolean save = this.save(team);
        if (!save) {
            throw new BusinessException(ErrorStatus.SERVICE_ERROR, "创建队伍失败");
        }
        // 插入用户队伍关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(loginUser.getId());
        userTeam.setTeamId(team.getId());
        userTeam.setJoinTime(new Date());
        save = userTeamService.save(userTeam);
        if (!save) {
            throw new BusinessException(ErrorStatus.SERVICE_ERROR, "创建队伍失败");
        }
        return team.getId();
    }

    @Override
    public List<TeamUser> listTeams(TeamListRequest teamListRequest, boolean isAdmin, User loginUser) {
        //1. 用户名不为空的话，就作为查询条件。
        //2. 不展示过期的队伍，即根据队伍的过期时间对队伍进行筛选。
        //3. 查询时查出房主的信息
        //4. 查询时插入已经加入队伍的人数
        //4. 分页查询。
        LambdaQueryWrapper<Team> wrapper = new LambdaQueryWrapper<>();
        if (teamListRequest != null) {
            String teamName = teamListRequest.getTeamName();
            Long id = teamListRequest.getId();
            String description = teamListRequest.getDescription();
            Integer teamStatus = teamListRequest.getTeamStatus();
            Long userId = teamListRequest.getUserId();
            String searchText = teamListRequest.getSearchText();
            List<Long> teamIds = teamListRequest.getTeamIds();

            wrapper.like(StringUtils.isNotBlank(teamName), Team::getTeamName, teamName);
            wrapper.eq(id != null && id > 0, Team::getId, id);
            wrapper.like(StringUtils.isNotBlank(description), Team::getDescription, description);
            wrapper.in(!CollectionUtils.isEmpty(teamIds), Team::getId, teamIds);
            if (teamStatus != null) {
                // 判断是不是在范围之内，如果不是则设置为公开
                TeamStatusEnum anEnum = TeamStatusEnum.getEnum(teamStatus);
                if (anEnum == null) {
                    teamStatus = TeamStatusEnum.OPEN.getType();
                }
                if (!isAdmin && !TeamStatusEnum.OPEN.equals(anEnum)) {
                    throw new BusinessException(ErrorStatus.FORBIDDEN_ERROR, "没有权限");
                }
                wrapper.eq(Team::getTeamStatus, teamStatus);
            }
            wrapper.eq(userId != null && userId > 0, Team::getUserId, userId);
            wrapper.and(wp -> wp.eq(Team::getExpireTime, null).or().ge(Team::getExpireTime, new Date()));
            if (StringUtils.isNotBlank(searchText)) {
                wrapper.and(wp -> wp.like(Team::getTeamName, searchText).or().like(Team::getDescription, searchText));
            }
        }
        List<Team> teams = this.list(wrapper);
        if (teams == null) {
            return new ArrayList<>();
        }
        List<TeamUser> teamUsers = new ArrayList<>();
        // 查询的时候查出队伍的队长信息
        for (Team team : teams) {
            Long userId = team.getUserId();
            if (userId == null) {
                continue;
            }
            TeamUser teamUser = new TeamUser();
            UserVo userVo = new UserVo();
            User user = userService.getById(userId);
            if (user != null) {
                BeanUtils.copyProperties(user, userVo);
            }
            BeanUtils.copyProperties(team, teamUser);
            teamUser.setUsers(List.of(userVo));
            teamUsers.add(teamUser);
        }
        // 设置用户是否加入队伍的状态
        LambdaQueryWrapper<UserTeam> userTeamWrapper = new LambdaQueryWrapper<>();
        userTeamWrapper.eq(UserTeam::getUserId, loginUser.getId());
        List<UserTeam> userTeams = userTeamService.list(userTeamWrapper);
        if (!CollectionUtils.isEmpty(userTeams)) {
            List<Long> teamIds = userTeams.stream().map(UserTeam::getTeamId).collect(Collectors.toList());
            teamUsers.forEach(teamUser -> {
                if (teamIds.contains(teamUser.getId())) {
                    teamUser.setJoinStatus(true);
                } else {
                    teamUser.setJoinStatus(false);
                }
            });
        }
        teamUsers = teamUsers.stream().map(teamUser -> {
            Long teamId = teamUser.getId();
            LambdaQueryWrapper<UserTeam> teamUserWrapper = new LambdaQueryWrapper<>();
            teamUserWrapper.eq(UserTeam::getTeamId, teamId);
            long count = userTeamService.count(teamUserWrapper);
            teamUser.setJoinNums((int) count);
            return teamUser;
        }).collect(Collectors.toList());
        return teamUsers;
    }

    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR);
        }
        Long id = teamUpdateRequest.getId();
        Team team = getTeam(id);
        if (userService.isAdmin(loginUser) || team.getUserId() == loginUser.getId()) {
            Integer teamStatus = teamUpdateRequest.getTeamStatus();
            if (teamStatus != null) {
                TeamStatusEnum anEnum = TeamStatusEnum.getEnum(teamStatus);
                if (anEnum == null) {
                    throw new BusinessException(ErrorStatus.PARAMS_ERROR, "队伍状态参数错误");
                }
                if (TeamStatusEnum.PASSWROD.equals(anEnum)) {
                    String password = teamUpdateRequest.getTeamPassword();
                    if (StringUtils.isBlank(password)) {
                        throw new BusinessException(ErrorStatus.PARAMS_ERROR, "加密房间密码不能为空");
                    }
                }
            }
            Team tempTeam = new Team();
            BeanUtils.copyProperties(teamUpdateRequest, tempTeam);
            return this.updateById(tempTeam);
        } else {
            throw new BusinessException(ErrorStatus.FORBIDDEN_ERROR, "没有权限");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        if (teamJoinRequest == null || teamJoinRequest.getTeamId() <= 0) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR);
        }
        Team team = this.getById(teamJoinRequest.getTeamId());
        if (team == null || team.getExpireTime().before(new Date())) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "队伍不存在或者已过期");
        }
        Integer teamStatus = team.getTeamStatus();
        TeamStatusEnum anEnum = TeamStatusEnum.getEnum(teamStatus);
        if (TeamStatusEnum.CLOSED.equals(anEnum)) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "不能加入私有的队伍");
        }
        if (TeamStatusEnum.PASSWROD.equals(anEnum)) {
            String password = teamJoinRequest.getPassword();
            if (StringUtils.isBlank(password) || !password.equals(team.getTeamPassword())) {
                throw new BusinessException(ErrorStatus.PARAMS_ERROR, "加密队伍密码不能为空或者密码错误");
            }
        }
        LambdaQueryWrapper<UserTeam> wrapper = new LambdaQueryWrapper<>();
        // 这里对队伍人数不能超过5个对判断需要加锁
        RLock lock = redissonClient.getLock(TeamConstant.ADD_TEAM_USERID_LOCK + loginUser.getId());
        try {
            // 没抢到锁就一直抢
            while (true) {
                if (lock.tryLock(0, 10l, TimeUnit.SECONDS)) {
                    System.out.println("获取到锁了: " + Thread.currentThread().getId());
                    wrapper.eq(UserTeam::getUserId, loginUser.getId());
                    long count = userTeamService.count(wrapper);
                    if (count >= 5) {
                        throw new BusinessException(ErrorStatus.PARAMS_ERROR, "一个用户最多只能加入5个队伍");
                    }

                    wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(UserTeam::getTeamId, team.getId());
                    wrapper.eq(UserTeam::getUserId, loginUser.getId());
                    count = userTeamService.count(wrapper);
                    if (count > 0) {
                        throw new BusinessException(ErrorStatus.PARAMS_ERROR, "不能重复加入队伍");
                    }
                    // 加入的人不能超过队伍的最大人数
                    wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(UserTeam::getTeamId, team.getId());
                    count = userTeamService.count(wrapper);
                    if (count >= team.getMaxNums()) {
                        throw new BusinessException(ErrorStatus.PARAMS_ERROR, "队伍已满");
                    }
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(loginUser.getId());
                    userTeam.setTeamId(teamJoinRequest.getTeamId());
                    userTeam.setJoinTime(new Date());
                    return userTeamService.save(userTeam);
                }
            }
        } catch (InterruptedException e) {
            log.error("get userId lock failed, ", e);
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(Long teamId, User loginUser) {
        Team team = getTeam(teamId);
        LambdaQueryWrapper<UserTeam> userTeamWrapper = new LambdaQueryWrapper<>();
        userTeamWrapper.eq(UserTeam::getTeamId, teamId);
        userTeamWrapper.eq(UserTeam::getUserId, loginUser.getId());
        long count = userTeamService.count(userTeamWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "你不在该队伍里");
        }
        LambdaQueryWrapper<UserTeam> teamWrapper = new LambdaQueryWrapper<>();
        teamWrapper.eq(UserTeam::getTeamId, teamId);
        count = userTeamService.count(teamWrapper);
        if (count == 1) {
            this.removeById(teamId);
            return userTeamService.remove(teamWrapper);
        } else {
            // 判断是不是队长，如果是队长则顺延
            if (team.getUserId().equals(loginUser.getId())) {
                // 顺延只用取两条
                teamWrapper.last("order by id asc limit 2");
                List<UserTeam> userTeamList = userTeamService.list(teamWrapper);
                if (userTeamList == null || userTeamList.size() < 2) {
                    throw new BusinessException(ErrorStatus.SERVICE_ERROR);
                }
                team.setUserId(userTeamList.get(1).getUserId());
                boolean save = this.updateById(team);
                if (!save) {
                    throw new BusinessException(ErrorStatus.SERVICE_ERROR, "更新队长失败");
                }
            }
            return userTeamService.remove(userTeamWrapper);
        }
    }

    private @NotNull Team getTeam(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "队伍不存在");
        }
        return team;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTeam(Long teamId, User loginUser) {
        Team team = getTeam(teamId);
        if (!team.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorStatus.FORBIDDEN_ERROR, "你不是队长，无法解散队伍");
        }
        LambdaQueryWrapper<UserTeam> userTeamWrapper = new LambdaQueryWrapper<>();
        userTeamWrapper.eq(UserTeam::getTeamId, teamId);
        boolean remove = userTeamService.remove(userTeamWrapper);
        if (!remove) {
            throw new BusinessException(ErrorStatus.SERVICE_ERROR, "解除队伍关联关系失败");
        }
        return this.removeById(teamId);
    }

    @Override
    public Page<TeamUser> pageTeams(TeamPageRequest teamPageRequest, boolean isAdmin, User loginUser) {
        //1. 用户名不为空的话，就作为查询条件。
        //2. 不展示过期的队伍，即根据队伍的过期时间对队伍进行筛选。
        //3. 查询时查出房主的信息
        //4. 查询时插入已经加入队伍的人数
        //4. 分页查询。
        Integer pageNum = teamPageRequest.getPageNum();
        Integer pageSize = teamPageRequest.getPageSize();
        if (teamPageRequest == null || pageNum == null || pageSize == null) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR);
        }
        if (pageNum <= 0 || pageSize <= 0) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "分页参数错误");
        }
        LambdaQueryWrapper<Team> wrapper = new LambdaQueryWrapper<>();
        String teamName = teamPageRequest.getTeamName();
        Long id = teamPageRequest.getId();
        String description = teamPageRequest.getDescription();
        Integer teamStatus = teamPageRequest.getTeamStatus();
        Long userId = teamPageRequest.getUserId();
        String searchText = teamPageRequest.getSearchText();

        wrapper.like(StringUtils.isNotBlank(teamName), Team::getTeamName, teamName);
        wrapper.eq(id != null && id > 0, Team::getId, id);
        wrapper.like(StringUtils.isNotBlank(description), Team::getDescription, description);
        if (teamStatus != null) {
            // 判断是不是在范围之内，如果不是则设置为公开
            TeamStatusEnum anEnum = TeamStatusEnum.getEnum(teamStatus);
            if (anEnum == null) {
                teamStatus = TeamStatusEnum.OPEN.getType();
            }
            if (!isAdmin && !TeamStatusEnum.OPEN.equals(anEnum)) {
                throw new BusinessException(ErrorStatus.FORBIDDEN_ERROR, "没有权限");
            }
            wrapper.eq(Team::getTeamStatus, teamStatus);
        }
        wrapper.eq(userId != null && userId > 0, Team::getUserId, userId);
        wrapper.and(wp -> wp.eq(Team::getExpireTime, null).or().ge(Team::getExpireTime, new Date()));
        if (StringUtils.isNotBlank(searchText)) {
            wrapper.and(wp -> wp.like(Team::getTeamName, searchText).or().like(Team::getDescription, searchText));
        }
        Page<Team> teamPage = new Page<>(pageNum, pageSize);
        Page<Team> teamPageRes = this.page(teamPage, wrapper);
        if (teamPageRes == null) {
            return null;
        }
        List<TeamUser> teamUsers = new ArrayList<>();
        List<Team> teams = teamPageRes.getRecords();
        // 查询的时候查出队伍的队长信息
        for (Team team : teams) {
            userId = team.getUserId();
            if (userId == null) {
                continue;
            }
            TeamUser teamUser = new TeamUser();
            UserVo userVo = new UserVo();
            User user = userService.getById(userId);
            if (user != null) {
                BeanUtils.copyProperties(user, userVo);
            }
            BeanUtils.copyProperties(team, teamUser);
            teamUser.setUsers(List.of(userVo));
            teamUsers.add(teamUser);
        }
        // 设置用户是否加入队伍的状态
        LambdaQueryWrapper<UserTeam> userTeamWrapper = new LambdaQueryWrapper<>();
        userTeamWrapper.eq(UserTeam::getUserId, loginUser.getId());
        List<UserTeam> userTeams = userTeamService.list(userTeamWrapper);
        if (!CollectionUtils.isEmpty(userTeams)) {
            List<Long> teamIds = userTeams.stream().map(UserTeam::getTeamId).collect(Collectors.toList());
            teamUsers.forEach(teamUser -> {
                if (teamIds.contains(teamUser.getId())) {
                    teamUser.setJoinStatus(true);
                } else {
                    teamUser.setJoinStatus(false);
                }
            });
        }
        teamUsers = teamUsers.stream().map(teamUser -> {
            Long teamId = teamUser.getId();
            LambdaQueryWrapper<UserTeam> teamUserWrapper = new LambdaQueryWrapper<>();
            teamUserWrapper.eq(UserTeam::getTeamId, teamId);
            long count = userTeamService.count(teamUserWrapper);
            teamUser.setJoinNums((int) count);
            return teamUser;
        }).collect(Collectors.toList());
        Page<TeamUser> teamUserPage = new Page<>();
        BeanUtils.copyProperties(teamPageRes, teamUserPage);
        teamUserPage.setRecords(teamUsers);
        return teamUserPage;
    }
}




