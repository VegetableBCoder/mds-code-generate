package com.mds.demo.mapper;

import static com.mds.demo.mapper.AppUserDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

import com.mds.demo.entity.AppUser;
import jakarta.annotation.Generated;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.dynamic.sql.BasicColumn;
import org.mybatis.dynamic.sql.delete.DeleteDSLCompleter;
import org.mybatis.dynamic.sql.select.CountDSLCompleter;
import org.mybatis.dynamic.sql.select.SelectDSLCompleter;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.update.UpdateDSL;
import org.mybatis.dynamic.sql.update.UpdateDSLCompleter;
import org.mybatis.dynamic.sql.update.UpdateModel;
import org.mybatis.dynamic.sql.util.SqlProviderAdapter;
import org.mybatis.dynamic.sql.util.mybatis3.CommonCountMapper;
import org.mybatis.dynamic.sql.util.mybatis3.CommonDeleteMapper;
import org.mybatis.dynamic.sql.util.mybatis3.CommonInsertMapper;
import org.mybatis.dynamic.sql.util.mybatis3.CommonUpdateMapper;
import org.mybatis.dynamic.sql.util.mybatis3.MyBatis3Utils;

@Mapper
public interface AppUserMapper extends CommonCountMapper, CommonDeleteMapper, CommonInsertMapper<AppUser>, CommonUpdateMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: app_user")
    BasicColumn[] selectList = BasicColumn.columnList(id, username, displayName, passwordHash, enabled, status, balance, createdAt, updatedAt, profile);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: app_user")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @Results(id="AppUserResult", value = {
        @Result(column="id", property="id", jdbcType=JdbcType.BIGINT, id=true),
        @Result(column="username", property="username", jdbcType=JdbcType.VARCHAR),
        @Result(column="display_name", property="displayName", jdbcType=JdbcType.VARCHAR),
        @Result(column="password_hash", property="passwordHash", jdbcType=JdbcType.VARCHAR),
        @Result(column="enabled", property="enabled", jdbcType=JdbcType.TINYINT),
        @Result(column="status", property="status", jdbcType=JdbcType.SMALLINT),
        @Result(column="balance", property="balance", jdbcType=JdbcType.DECIMAL),
        @Result(column="created_at", property="createdAt", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="updated_at", property="updatedAt", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="profile", property="profile", jdbcType=JdbcType.LONGVARCHAR)
    })
    List<AppUser> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: app_user")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ResultMap("AppUserResult")
    Optional<AppUser> selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: app_user")
    default long count(CountDSLCompleter completer) {
        return MyBatis3Utils.countFrom(this::count, appUser, completer);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: app_user")
    default int delete(DeleteDSLCompleter completer) {
        return MyBatis3Utils.deleteFrom(this::delete, appUser, completer);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: app_user")
    default int deleteByPrimaryKey(Long id_) {
        return delete(c -> 
            c.where(id, isEqualTo(id_))
        );
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: app_user")
    default int insert(AppUser row) {
        return MyBatis3Utils.insert(this::insert, row, appUser, c ->
            c.map(id).toProperty("id")
            .map(username).toProperty("username")
            .map(displayName).toProperty("displayName")
            .map(passwordHash).toProperty("passwordHash")
            .map(enabled).toProperty("enabled")
            .map(status).toProperty("status")
            .map(balance).toProperty("balance")
            .map(createdAt).toProperty("createdAt")
            .map(updatedAt).toProperty("updatedAt")
            .map(profile).toProperty("profile")
        );
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: app_user")
    default int insertMultiple(Collection<AppUser> records) {
        return MyBatis3Utils.insertMultiple(this::insertMultiple, records, appUser, c ->
            c.map(id).toProperty("id")
            .map(username).toProperty("username")
            .map(displayName).toProperty("displayName")
            .map(passwordHash).toProperty("passwordHash")
            .map(enabled).toProperty("enabled")
            .map(status).toProperty("status")
            .map(balance).toProperty("balance")
            .map(createdAt).toProperty("createdAt")
            .map(updatedAt).toProperty("updatedAt")
            .map(profile).toProperty("profile")
        );
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: app_user")
    default int insertSelective(AppUser row) {
        return MyBatis3Utils.insert(this::insert, row, appUser, c ->
            c.map(id).toPropertyWhenPresent("id", row::getId)
            .map(username).toPropertyWhenPresent("username", row::getUsername)
            .map(displayName).toPropertyWhenPresent("displayName", row::getDisplayName)
            .map(passwordHash).toPropertyWhenPresent("passwordHash", row::getPasswordHash)
            .map(enabled).toPropertyWhenPresent("enabled", row::getEnabled)
            .map(status).toPropertyWhenPresent("status", row::getStatus)
            .map(balance).toPropertyWhenPresent("balance", row::getBalance)
            .map(createdAt).toPropertyWhenPresent("createdAt", row::getCreatedAt)
            .map(updatedAt).toPropertyWhenPresent("updatedAt", row::getUpdatedAt)
            .map(profile).toPropertyWhenPresent("profile", row::getProfile)
        );
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: app_user")
    default Optional<AppUser> selectOne(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectOne(this::selectOne, selectList, appUser, completer);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: app_user")
    default List<AppUser> select(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectList(this::selectMany, selectList, appUser, completer);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: app_user")
    default List<AppUser> selectDistinct(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectDistinct(this::selectMany, selectList, appUser, completer);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: app_user")
    default Optional<AppUser> selectByPrimaryKey(Long id_) {
        return selectOne(c ->
            c.where(id, isEqualTo(id_))
        );
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: app_user")
    default int update(UpdateDSLCompleter completer) {
        return MyBatis3Utils.update(this::update, appUser, completer);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: app_user")
    static UpdateDSL<UpdateModel> updateAllColumns(AppUser row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(id).equalTo(row::getId)
                .set(username).equalTo(row::getUsername)
                .set(displayName).equalTo(row::getDisplayName)
                .set(passwordHash).equalTo(row::getPasswordHash)
                .set(enabled).equalTo(row::getEnabled)
                .set(status).equalTo(row::getStatus)
                .set(balance).equalTo(row::getBalance)
                .set(createdAt).equalTo(row::getCreatedAt)
                .set(updatedAt).equalTo(row::getUpdatedAt)
                .set(profile).equalTo(row::getProfile);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: app_user")
    static UpdateDSL<UpdateModel> updateSelectiveColumns(AppUser row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(id).equalToWhenPresent(row::getId)
                .set(username).equalToWhenPresent(row::getUsername)
                .set(displayName).equalToWhenPresent(row::getDisplayName)
                .set(passwordHash).equalToWhenPresent(row::getPasswordHash)
                .set(enabled).equalToWhenPresent(row::getEnabled)
                .set(status).equalToWhenPresent(row::getStatus)
                .set(balance).equalToWhenPresent(row::getBalance)
                .set(createdAt).equalToWhenPresent(row::getCreatedAt)
                .set(updatedAt).equalToWhenPresent(row::getUpdatedAt)
                .set(profile).equalToWhenPresent(row::getProfile);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: app_user")
    default int updateByPrimaryKey(AppUser row) {
        return update(c ->
            c.set(username).equalTo(row::getUsername)
            .set(displayName).equalTo(row::getDisplayName)
            .set(passwordHash).equalTo(row::getPasswordHash)
            .set(enabled).equalTo(row::getEnabled)
            .set(status).equalTo(row::getStatus)
            .set(balance).equalTo(row::getBalance)
            .set(createdAt).equalTo(row::getCreatedAt)
            .set(updatedAt).equalTo(row::getUpdatedAt)
            .set(profile).equalTo(row::getProfile)
            .where(id, isEqualTo(row::getId))
        );
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: app_user")
    default int updateByPrimaryKeySelective(AppUser row) {
        return update(c ->
            c.set(username).equalToWhenPresent(row::getUsername)
            .set(displayName).equalToWhenPresent(row::getDisplayName)
            .set(passwordHash).equalToWhenPresent(row::getPasswordHash)
            .set(enabled).equalToWhenPresent(row::getEnabled)
            .set(status).equalToWhenPresent(row::getStatus)
            .set(balance).equalToWhenPresent(row::getBalance)
            .set(createdAt).equalToWhenPresent(row::getCreatedAt)
            .set(updatedAt).equalToWhenPresent(row::getUpdatedAt)
            .set(profile).equalToWhenPresent(row::getProfile)
            .where(id, isEqualTo(row::getId))
        );
    }
}