package com.mds.demo.mapper;

import jakarta.annotation.Generated;
import java.sql.JDBCType;
import java.time.LocalDateTime;
import org.mybatis.dynamic.sql.AliasableSqlTable;
import org.mybatis.dynamic.sql.SqlColumn;

public final class DeviceDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: device")
    public static final Device device = new Device();

    /**
     * Database Column Remarks:
     *   设备唯一标识（主键）
     */
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source field: device.uuid")
    public static final SqlColumn<String> uuid = device.uuid;

    /**
     * Database Column Remarks:
     *   设备名
     */
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source field: device.name")
    public static final SqlColumn<String> name = device.name;

    /**
     * Database Column Remarks:
     *   设备类型：1 真机 / 2 模拟器 / 3 云机
     */
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source field: device.kind")
    public static final SqlColumn<Short> kind = device.kind;

    /**
     * Database Column Remarks:
     *   是否在线
     */
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source field: device.online")
    public static final SqlColumn<Byte> online = device.online;

    /**
     * Database Column Remarks:
     *   最后心跳
     */
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source field: device.last_seen")
    public static final SqlColumn<LocalDateTime> lastSeen = device.lastSeen;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: device")
    public static final class Device extends AliasableSqlTable<Device> {
        public final SqlColumn<String> uuid = column("uuid", JDBCType.CHAR);

        public final SqlColumn<String> name = column("name", JDBCType.VARCHAR);

        public final SqlColumn<Short> kind = column("kind", JDBCType.SMALLINT);

        public final SqlColumn<Byte> online = column("online", JDBCType.TINYINT);

        public final SqlColumn<LocalDateTime> lastSeen = column("last_seen", JDBCType.TIMESTAMP);

        public Device() {
            super("device", Device::new);
        }
    }
}