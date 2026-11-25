package com.mds.demo.domain.device;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** DeviceKindEnum（手写枚举：成员与文案由业务决定，插件只出骨架）。 */
@Getter
@RequiredArgsConstructor
public enum DeviceKindEnum {

    /** 真机 */
    REAL((short) 1, "真机"),
    /** 模拟器 */
    EMULATOR((short) 2, "模拟器"),
    /** 云机 */
    CLOUD((short) 3, "云机");

    private final short value;
    private final String label;

    public static DeviceKindEnum fromValue(short value) {
        for (DeviceKindEnum item : values()) {
            if (item.value == value) {
                return item;
            }
        }
        throw new IllegalArgumentException("未知的 DeviceKindEnum 落库值: " + value);
    }
}