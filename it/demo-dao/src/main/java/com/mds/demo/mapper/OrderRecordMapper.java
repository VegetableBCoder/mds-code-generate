package com.mds.demo.mapper;

import static com.mds.demo.mapper.OrderRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

import com.mds.demo.entity.OrderRecord;
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
public interface OrderRecordMapper extends CommonCountMapper, CommonDeleteMapper, CommonInsertMapper<OrderRecord>, CommonUpdateMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: order_record")
    BasicColumn[] selectList = BasicColumn.columnList(orderNo, userId, amount, state, channel, createdAt, payload);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: order_record")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @Results(id="OrderRecordResult", value = {
        @Result(column="order_no", property="orderNo", jdbcType=JdbcType.VARCHAR, id=true),
        @Result(column="user_id", property="userId", jdbcType=JdbcType.BIGINT),
        @Result(column="amount", property="amount", jdbcType=JdbcType.DECIMAL),
        @Result(column="state", property="state", jdbcType=JdbcType.SMALLINT),
        @Result(column="channel", property="channel", jdbcType=JdbcType.VARCHAR),
        @Result(column="created_at", property="createdAt", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="payload", property="payload", jdbcType=JdbcType.LONGVARCHAR)
    })
    List<OrderRecord> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: order_record")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ResultMap("OrderRecordResult")
    Optional<OrderRecord> selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: order_record")
    default long count(CountDSLCompleter completer) {
        return MyBatis3Utils.countFrom(this::count, orderRecord, completer);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: order_record")
    default int delete(DeleteDSLCompleter completer) {
        return MyBatis3Utils.deleteFrom(this::delete, orderRecord, completer);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: order_record")
    default int deleteByPrimaryKey(String orderNo_) {
        return delete(c -> 
            c.where(orderNo, isEqualTo(orderNo_))
        );
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: order_record")
    default int insert(OrderRecord row) {
        return MyBatis3Utils.insert(this::insert, row, orderRecord, c ->
            c.map(orderNo).toProperty("orderNo")
            .map(userId).toProperty("userId")
            .map(amount).toProperty("amount")
            .map(state).toProperty("state")
            .map(channel).toProperty("channel")
            .map(createdAt).toProperty("createdAt")
            .map(payload).toProperty("payload")
        );
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: order_record")
    default int insertMultiple(Collection<OrderRecord> records) {
        return MyBatis3Utils.insertMultiple(this::insertMultiple, records, orderRecord, c ->
            c.map(orderNo).toProperty("orderNo")
            .map(userId).toProperty("userId")
            .map(amount).toProperty("amount")
            .map(state).toProperty("state")
            .map(channel).toProperty("channel")
            .map(createdAt).toProperty("createdAt")
            .map(payload).toProperty("payload")
        );
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: order_record")
    default int insertSelective(OrderRecord row) {
        return MyBatis3Utils.insert(this::insert, row, orderRecord, c ->
            c.map(orderNo).toPropertyWhenPresent("orderNo", row::getOrderNo)
            .map(userId).toPropertyWhenPresent("userId", row::getUserId)
            .map(amount).toPropertyWhenPresent("amount", row::getAmount)
            .map(state).toPropertyWhenPresent("state", row::getState)
            .map(channel).toPropertyWhenPresent("channel", row::getChannel)
            .map(createdAt).toPropertyWhenPresent("createdAt", row::getCreatedAt)
            .map(payload).toPropertyWhenPresent("payload", row::getPayload)
        );
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: order_record")
    default Optional<OrderRecord> selectOne(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectOne(this::selectOne, selectList, orderRecord, completer);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: order_record")
    default List<OrderRecord> select(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectList(this::selectMany, selectList, orderRecord, completer);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: order_record")
    default List<OrderRecord> selectDistinct(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectDistinct(this::selectMany, selectList, orderRecord, completer);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: order_record")
    default Optional<OrderRecord> selectByPrimaryKey(String orderNo_) {
        return selectOne(c ->
            c.where(orderNo, isEqualTo(orderNo_))
        );
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: order_record")
    default int update(UpdateDSLCompleter completer) {
        return MyBatis3Utils.update(this::update, orderRecord, completer);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: order_record")
    static UpdateDSL<UpdateModel> updateAllColumns(OrderRecord row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(orderNo).equalTo(row::getOrderNo)
                .set(userId).equalTo(row::getUserId)
                .set(amount).equalTo(row::getAmount)
                .set(state).equalTo(row::getState)
                .set(channel).equalTo(row::getChannel)
                .set(createdAt).equalTo(row::getCreatedAt)
                .set(payload).equalTo(row::getPayload);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: order_record")
    static UpdateDSL<UpdateModel> updateSelectiveColumns(OrderRecord row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(orderNo).equalToWhenPresent(row::getOrderNo)
                .set(userId).equalToWhenPresent(row::getUserId)
                .set(amount).equalToWhenPresent(row::getAmount)
                .set(state).equalToWhenPresent(row::getState)
                .set(channel).equalToWhenPresent(row::getChannel)
                .set(createdAt).equalToWhenPresent(row::getCreatedAt)
                .set(payload).equalToWhenPresent(row::getPayload);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: order_record")
    default int updateByPrimaryKey(OrderRecord row) {
        return update(c ->
            c.set(userId).equalTo(row::getUserId)
            .set(amount).equalTo(row::getAmount)
            .set(state).equalTo(row::getState)
            .set(channel).equalTo(row::getChannel)
            .set(createdAt).equalTo(row::getCreatedAt)
            .set(payload).equalTo(row::getPayload)
            .where(orderNo, isEqualTo(row::getOrderNo))
        );
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", comments="Source Table: order_record")
    default int updateByPrimaryKeySelective(OrderRecord row) {
        return update(c ->
            c.set(userId).equalToWhenPresent(row::getUserId)
            .set(amount).equalToWhenPresent(row::getAmount)
            .set(state).equalToWhenPresent(row::getState)
            .set(channel).equalToWhenPresent(row::getChannel)
            .set(createdAt).equalToWhenPresent(row::getCreatedAt)
            .set(payload).equalToWhenPresent(row::getPayload)
            .where(orderNo, isEqualTo(row::getOrderNo))
        );
    }
}