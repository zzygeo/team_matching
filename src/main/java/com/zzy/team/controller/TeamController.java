package com.zzy.team.controller;

import com.zzy.team.common.Result;
import com.zzy.team.constant.ErrorStatus;
import com.zzy.team.exception.BusinessException;
import com.zzy.team.model.domain.Team;
import com.zzy.team.service.TeamService;
import com.zzy.team.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/team")
@CrossOrigin(value = {"http://localhost:5173"}, allowCredentials = "true")
@Slf4j
@Api(tags = {"队伍管理接口"})
public class TeamController {
    @Autowired
    private TeamService teamService;

    @Autowired
    private UserService userService;

    @PostMapping("/add")
    @ApiOperation("添加队伍")
    public Result<Long> addTeam(@RequestBody Team team) {
        if(team == null) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR);
        }
        boolean save = teamService.save(team);
        if (!save) {
            throw new BusinessException(ErrorStatus.SERVICE_ERROR, "插入失败");
        }
        return Result.OK(team.getId());
    }

    @PostMapping("/delete")
    @ApiOperation("删除队伍")
    public Result<Boolean> deleteTeam(Long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR);
        }
        boolean isDelete = teamService.removeById(id);
        if (!isDelete) {
            throw new BusinessException(ErrorStatus.SERVICE_ERROR, "删除失败");
        }
        return Result.OK();
    }

    @PostMapping("/update")
    @ApiOperation("更新队伍")
    public Result<Boolean> updateTeam(@RequestBody Team team) {
        if (team == null || team.getId() <= 0) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR);
        }
        boolean save = teamService.updateById(team);
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
}
