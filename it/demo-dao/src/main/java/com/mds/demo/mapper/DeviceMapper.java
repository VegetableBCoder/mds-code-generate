package com.mds.demo.mapper;

import static com.mds.demo.mapper.DeviceDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

import com.mds.demo.entity.Device;
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
public interface DeviceMapper extends CommonCountMapper, CommonDeleteMapper, CommonInsertMapper<Device>, CommonUpdateMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: device")
    BasicColumn[] selectList = BasicColumn.columnList(uuid, name, kind, online, lastSeen);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: device")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @Results(id="DeviceResult", value = {
        @Result(column="uuid", property="uuid", jdbcType=JdbcType.CHAR, id=true),
        @Result(column="name", property="name", jdbcType=JdbcType.VARCHAR),
        @Result(column="kind", property="kind", jdbcType=JdbcType.SMALLINT),
        @Result(column="online", property="online", jdbcType=JdbcType.TINYINT),
        @Result(column="last_seen", property="lastSeen", jdbcType=JdbcType.TIMESTAMP)
    })
    List<Device> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: device")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ResultMap("DeviceResult")
    Optional<Device> selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: device")
    default long count(CountDSLCompleter completer) {
        return MyBatis3Utils.countFrom(this::count, device, completer);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: device")
    default int delete(DeleteDSLCompleter completer) {
        return MyBatis3Utils.deleteFrom(this::delete, device, completer);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: device")
    default int deleteByPrimaryKey(String uuid_) {
        return delete(c -> 
            c.where(uuid, isEqualTo(uuid_))
        );
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: device")
    default int insert(Device row) {
        return MyBatis3Utils.insert(this::insert, row, device, c ->
            c.map(uuid).toProperty("uuid")
            .map(name).toProperty("name")
            .map(kind).toProperty("kind")
            .map(online).toProperty("online")
            .map(lastSeen).toProperty("lastSeen")
        );
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: device")
    default int insertMultiple(Collection<Device> records) {
        return MyBatis3Utils.insertMultiple(this::insertMultiple, records, device, c ->
            c.map(uuid).toProperty("uuid")
            .map(name).toProperty("name")
            .map(kind).toProperty("kind")
            .map(online).toProperty("online")
            .map(lastSeen).toProperty("lastSeen")
        );
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: device")
    default int insertSelective(Device row) {
        return MyBatis3Utils.insert(this::insert, row, device, c ->
            c.map(uuid).toPropertyWhenPresent("uuid", row::getUuid)
            .map(name).toPropertyWhenPresent("name", row::getName)
            .map(kind).toPropertyWhenPresent("kind", row::getKind)
            .map(online).toPropertyWhenPresent("online", row::getOnline)
            .map(lastSeen).toPropertyWhenPresent("lastSeen", row::getLastSeen)
        );
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: device")
    default Optional<Device> selectOne(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectOne(this::selectOne, selectList, device, completer);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: device")
    default List<Device> select(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectList(this::selectMany, selectList, device, completer);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: device")
    default List<Device> selectDistinct(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectDistinct(this::selectMany, selectList, device, completer);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: device")
    default Optional<Device> selectByPrimaryKey(String uuid_) {
        return selectOne(c ->
            c.where(uuid, isEqualTo(uuid_))
        );
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: device")
    default int update(UpdateDSLCompleter completer) {
        return MyBatis3Utils.update(this::update, device, completer);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: device")
    static UpdateDSL<UpdateModel> updateAllColumns(Device row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(uuid).equalTo(row::getUuid)
                .set(name).equalTo(row::getName)
                .set(kind).equalTo(row::getKind)
                .set(online).equalTo(row::getOnline)
                .set(lastSeen).equalTo(row::getLastSeen);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: device")
    static UpdateDSL<UpdateModel> updateSelectiveColumns(Device row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(uuid).equalToWhenPresent(row::getUuid)
                .set(name).equalToWhenPresent(row::getName)
                .set(kind).equalToWhenPresent(row::getKind)
                .set(online).equalToWhenPresent(row::getOnline)
                .set(lastSeen).equalToWhenPresent(row::getLastSeen);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: device")
    default int updateByPrimaryKey(Device row) {
        return update(c ->
            c.set(name).equalTo(row::getName)
            .set(kind).equalTo(row::getKind)
            .set(online).equalTo(row::getOnline)
            .set(lastSeen).equalTo(row::getLastSeen)
            .where(uuid, isEqualTo(row::getUuid))
        );
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: device")
    default int updateByPrimaryKeySelective(Device row) {
        return update(c ->
            c.set(name).equalToWhenPresent(row::getName)
            .set(kind).equalToWhenPresent(row::getKind)
            .set(online).equalToWhenPresent(row::getOnline)
            .set(lastSeen).equalToWhenPresent(row::getLastSeen)
            .where(uuid, isEqualTo(row::getUuid))
        );
    }
}