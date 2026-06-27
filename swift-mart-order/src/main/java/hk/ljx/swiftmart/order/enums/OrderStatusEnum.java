package hk.ljx.swiftmart.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    PENDING_PAYMENT(0, "待支付"),
    PENDING_SHIPMENT(1, "待发货"),
    SHIPPED(2, "已发货"),
    RECEIVED(3, "已收货"),
    REFUNDED(4, "已退款"),
    CANCELLED(5, "已取消"),
    CLOSED(6, "已关闭");

    /**
     * 状态值
     */
    private final Integer status;

    /**
     * 状态描述
     */
    private final String description;
}

