package com.zzy.team.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 8332889461315970193L;

    private String userAccount;

    private String userPassword;
}
