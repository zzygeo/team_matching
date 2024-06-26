package com.zzy.team.model.request;

import lombok.Data;

@Data
public class TeamJoinRequest {
    private Long teamId;
    private String password;
}
