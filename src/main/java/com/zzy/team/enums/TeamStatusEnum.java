package com.zzy.team.enums;

public enum TeamStatusEnum {
    OPEN(0, "公开"),
    CLOSED(1, "私有"),
    PASSWROD(2, "加密");

    int type;
    String text;

    TeamStatusEnum(int type, String text) {
        this.type = type;
        this.text = text;
    }

    public static TeamStatusEnum getEnum(int type) {
        TeamStatusEnum[] values = TeamStatusEnum.values();
        for (TeamStatusEnum value : values) {
            if (value.getType() == type) {
                return value;
            }
        }
        return null;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
