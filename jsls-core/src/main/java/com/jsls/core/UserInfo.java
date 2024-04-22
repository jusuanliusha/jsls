package com.jsls.core;

import lombok.Data;

@Data
public class UserInfo {
    public static final String SESSION_USER_ID="userId";
    public static final String SESSION_USERNAME="username";
    public static final String SESSION_NICK_NAME="nickName";
    private Long userId;
    private String username;
    private String nickName;
}
