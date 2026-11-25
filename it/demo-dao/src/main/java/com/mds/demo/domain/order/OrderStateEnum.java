package com.mds.demo.domain.order;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** OrderStateEnum（手写枚举：成员与文案由业务决定，插件只出骨架）。 */
@Getter
@RequiredArgsConstructor
public enum OrderStateEnum {

    /** 新建 */
    CREATED((short) 1, "新建"),
    /** 已支付 */
    PAID((short) 2, "已支付"),
    /** 已取消 */
    CANCELLED((short) 3, "已取消");

    private final short value;
    private final String label;

    public static OrderStateEnum fromValue(short value) {
        for (OrderStateEnum item : values()) {
            if (item.value == value) {
                return item;
            }
        }
        throw new IllegalArgumentException("未知的 OrderStateEnum 落库值: " + value);
    }
}