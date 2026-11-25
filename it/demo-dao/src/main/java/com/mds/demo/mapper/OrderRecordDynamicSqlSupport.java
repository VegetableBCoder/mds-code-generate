package com.mds.demo.mapper;

import jakarta.annotation.Generated;
import java.math.BigDecimal;
import java.sql.JDBCType;
import java.time.LocalDateTime;
import org.mybatis.dynamic.sql.AliasableSqlTable;
import org.mybatis.dynamic.sql.SqlColumn;

public final class OrderRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: order_record")
    public static final OrderRecord orderRecord = new OrderRecord();

    /**
     * Database Column Remarks:
     *   业务单号（主键，由调用方给，不由插件生成）
     */
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source field: order_record.order_no")
    public static final SqlColumn<String> orderNo = orderRecord.orderNo;

    /**
     * Database Column Remarks:
     *   下单账号
     */
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source field: order_record.user_id")
    public static final SqlColumn<Long> userId = orderRecord.userId;

    /**
     * Database Column Remarks:
     *   金额
     */
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source field: order_record.amount")
    public static final SqlColumn<BigDecimal> amount = orderRecord.amount;

    /**
     * Database Column Remarks:
     *   订单状态：1 新建 / 2 已支付 / 3 已取消
     */
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source field: order_record.state")
    public static final SqlColumn<Short> state = orderRecord.state;

    /**
     * Database Column Remarks:
     *   渠道
     */
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source field: order_record.channel")
    public static final SqlColumn<String> channel = orderRecord.channel;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source field: order_record.created_at")
    public static final SqlColumn<LocalDateTime> createdAt = orderRecord.createdAt;

    /**
     * Database Column Remarks:
     *   下单快照（JSON 文本）
     */
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source field: order_record.payload")
    public static final SqlColumn<String> payload = orderRecord.payload;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: order_record")
    public static final class OrderRecord extends AliasableSqlTable<OrderRecord> {
        public final SqlColumn<String> orderNo = column("order_no", JDBCType.VARCHAR);

        public final SqlColumn<Long> userId = column("user_id", JDBCType.BIGINT);

        public final SqlColumn<BigDecimal> amount = column("amount", JDBCType.DECIMAL);

        public final SqlColumn<Short> state = column("state", JDBCType.SMALLINT);

        public final SqlColumn<String> channel = column("channel", JDBCType.VARCHAR);

        public final SqlColumn<LocalDateTime> createdAt = column("created_at", JDBCType.TIMESTAMP);

        public final SqlColumn<String> payload = column("payload", JDBCType.LONGVARCHAR);

        public OrderRecord() {
            super("order_record", OrderRecord::new);
        }
    }
}