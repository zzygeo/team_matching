package com.zzy.team.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzy.team.common.Result;
import com.zzy.team.constant.ErrorStatus;
import com.zzy.team.exception.BusinessException;
import com.zzy.team.model.domain.Team;
import com.zzy.team.model.domain.User;
import com.zzy.team.model.domain.UserTeam;
import com.zzy.team.model.request.*;
import com.zzy.team.model.vo.TeamUser;
import com.zzy.team.service.TeamService;
import com.zzy.team.service.UserService;
import com.zzy.team.service.UserTeamService;
import com.zzy.team.utils.ServletUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/team")
@Slf4j
@Api(tags = {"队伍管理接口"})
public class TeamController {
    @Autowired
    private TeamService teamService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserTeamService userTeamService;

    @PostMapping("/add")
    @ApiOperation("添加队伍")
    public Result<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR);
        }
        User loginUser = ServletUtils.getLoginUser(request);
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);
        Long teamId = teamService.addTeam(team, loginUser);
        return Result.OK(teamId);
    }

    @PostMapping("/update")
    @ApiOperation("更新队伍")
    public Result<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        if (teamUpdateRequest == null || teamUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR);
        }
        User loginUser = ServletUtils.getLoginUser(request);
        boolean save = teamService.updateTeam(teamUpdateRequest, loginUser);
        if (!save) {
            throw new BusinessException(ErrorStatus.SERVICE_ERROR, "更新失败");
        }
        return Result.OK();
    }

    @GetMapping("/get")
    @ApiOperation("获取队伍")
    public Result<Team> getTeam(@RequestParam long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR);
        }
        Team team = teamService.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorStatus.SERVICE_ERROR, "队伍不存在");
        }
        return Result.OK(team);
    }

    @GetMapping("/list")
    @ApiOperation("获取队伍列表")
    public Result<List<TeamUser>> getTeamList(TeamListRequest teamListRequest,
                                              HttpServletRequest request) {
        if (teamListRequest == null) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR);
        }
        User loginUser = ServletUtils.getLoginUser(request);
        boolean admin = userService.isAdmin(loginUser);
        List<TeamUser> teamUsers = teamService.listTeams(teamListRequest, admin,loginUser);
        return Result.OK(teamUsers);
    }

    @GetMapping("/page")
    @ApiOperation("分页队伍列表")
    public Result<Page<TeamUser>> getTeamPage(TeamPageRequest teamPageRequest, HttpServletRequest request) {
        if (teamPageRequest == null) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR);
        }
        User loginUser = ServletUtils.getLoginUser(request);
        boolean admin = userService.isAdmin(loginUser);
        Page<TeamUser> teamUsers = teamService.pageTeams(teamPageRequest, admin,loginUser);
        return Result.OK(teamUsers);
    }

    @PostMapping("/join")
    @ApiOperation("加入队伍")
    public Result<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest == null || teamJoinRequest.getTeamId() <= 0) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR);
        }
        User loginUser = ServletUtils.getLoginUser(request);
        boolean save = teamService.joinTeam(teamJoinRequest, loginUser);
        if (!save) {
            throw new BusinessException(ErrorStatus.SERVICE_ERROR, "加入失败");
        }
        return Result.OK();
    }

    @PostMapping("/quitTeam")
    @ApiOperation("退出队伍")
    public Result<Boolean> quitTeam(Long teamId, HttpServletRequest request) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR);
        }
        User loginUser = ServletUtils.getLoginUser(request);
        boolean save = teamService.quitTeam(teamId, loginUser);
        if (!save) {
            throw new BusinessException(ErrorStatus.SERVICE_ERROR, "退出失败");
        }
        return Result.OK();
    }

    @PostMapping("/deleteTeam")
    @ApiOperation("解散队伍")
    public Result<Boolean> deleteTeam(Long teamId, HttpServletRequest request) {
        User loginUser = ServletUtils.getLoginUser(request);
        boolean save = teamService.deleteTeam(teamId, loginUser);
        if (!save) {
            throw new BusinessException(ErrorStatus.SERVICE_ERROR, "退出失败");
        }
        return Result.OK();
    }

    @GetMapping("/getEnterTeam")
    @ApiOperation("获取加入的队伍")
    public Result<List<TeamUser>> getEnterTeam(HttpServletRequest request) {
        User loginUser = ServletUtils.getLoginUser(request);
        // 先获取关联关系，再去查询team
        LambdaQueryWrapper<UserTeam> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTeam::getUserId, loginUser.getId());
        List<UserTeam> list = userTeamService.list(wrapper);
        if (!CollectionUtils.isEmpty(list)) {
            Map<Long, List<UserTeam>> userTeamGroupsByTeamId = list.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
            Set<Long> teamIds = userTeamGroupsByTeamId.keySet();
            ArrayList<Long> ids = new ArrayList<>(teamIds);
            TeamListRequest teamListRequest = new TeamListRequest();
            teamListRequest.setTeamIds(ids);
            List<TeamUser> teamUsers = teamService.listTeams(teamListRequest, true, loginUser);
            return Result.OK(teamUsers);
        }
        return Result.OK(null);
    }

    @GetMapping("/getCreateTeam")
    @ApiOperation("获取创建的队伍")
    Result<List<TeamUser>> getCreateTeam(HttpServletRequest request) {
        User loginUser = ServletUtils.getLoginUser(request);
        TeamListRequest teamListRequest = new TeamListRequest();
        teamListRequest.setUserId(loginUser.getId());
        List<TeamUser> teamUsers = teamService.listTeams(teamListRequest, true, loginUser);
        return Result.OK(teamUsers);
    }
}
