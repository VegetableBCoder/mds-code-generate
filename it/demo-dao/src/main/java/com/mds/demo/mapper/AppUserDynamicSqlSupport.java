package com.mds.demo.mapper;

import jakarta.annotation.Generated;
import java.math.BigDecimal;
import java.sql.JDBCType;
import java.time.LocalDateTime;
import org.mybatis.dynamic.sql.AliasableSqlTable;
import org.mybatis.dynamic.sql.SqlColumn;

public final class AppUserDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: app_user")
    public static final AppUser appUser = new AppUser();

    /**
     * Database Column Remarks:
     *   主键（应用侧雪花 ID，非自增）
     */
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source field: app_user.id")
    public static final SqlColumn<Long> id = appUser.id;

    /**
     * Database Column Remarks:
     *   登录名
     */
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source field: app_user.username")
    public static final SqlColumn<String> username = appUser.username;

    /**
     * Database Column Remarks:
     *   展示名
     */
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source field: app_user.display_name")
    public static final SqlColumn<String> displayName = appUser.displayName;

    /**
     * Database Column Remarks:
     *   凭据列（DTO 不裁剪，由业务层决定是否外露）
     */
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source field: app_user.password_hash")
    public static final SqlColumn<String> passwordHash = appUser.passwordHash;

    /**
     * Database Column Remarks:
     *   是否启用
     */
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source field: app_user.enabled")
    public static final SqlColumn<Byte> enabled = appUser.enabled;

    /**
     * Database Column Remarks:
     *   账号状态：1 正常 / 2 停用 / 3 锁定
     */
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source field: app_user.status")
    public static final SqlColumn<Short> status = appUser.status;

    /**
     * Database Column Remarks:
     *   余额
     */
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source field: app_user.balance")
    public static final SqlColumn<BigDecimal> balance = appUser.balance;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source field: app_user.created_at")
    public static final SqlColumn<LocalDateTime> createdAt = appUser.createdAt;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source field: app_user.updated_at")
    public static final SqlColumn<LocalDateTime> updatedAt = appUser.updatedAt;

    /**
     * Database Column Remarks:
     *   扩展信息（JSON 文本）
     */
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source field: app_user.profile")
    public static final SqlColumn<String> profile = appUser.profile;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: app_user")
    public static final class AppUser extends AliasableSqlTable<AppUser> {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<String> username = column("username", JDBCType.VARCHAR);

        public final SqlColumn<String> displayName = column("display_name", JDBCType.VARCHAR);

        public final SqlColumn<String> passwordHash = column("password_hash", JDBCType.VARCHAR);

        public final SqlColumn<Byte> enabled = column("enabled", JDBCType.TINYINT);

        public final SqlColumn<Short> status = column("status", JDBCType.SMALLINT);

        public final SqlColumn<BigDecimal> balance = column("balance", JDBCType.DECIMAL);

        public final SqlColumn<LocalDateTime> createdAt = column("created_at", JDBCType.TIMESTAMP);

        public final SqlColumn<LocalDateTime> updatedAt = column("updated_at", JDBCType.TIMESTAMP);

        public final SqlColumn<String> profile = column("profile", JDBCType.LONGVARCHAR);

        public AppUser() {
            super("app_user", AppUser::new);
        }
    }
}