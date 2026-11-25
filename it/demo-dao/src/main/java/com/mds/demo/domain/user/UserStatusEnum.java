package com.mds.demo.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** UserStatusEnum（手写枚举：成员与文案由业务决定，插件只出骨架）。 */
@Getter
@RequiredArgsConstructor
public enum UserStatusEnum {

    /** 正常 */
    NORMAL((short) 1, "正常"),
    /** 停用 */
    DISABLED((short) 2, "停用"),
    /** 锁定 */
    LOCKED((short) 3, "锁定");

    private final short value;
    private final String label;

    public static UserStatusEnum fromValue(short value) {
        for (UserStatusEnum item : values()) {
            if (item.value == value) {
                return item;
            }
        }
        throw new IllegalArgumentException("未知的 UserStatusEnum 落库值: " + value);
    }
}