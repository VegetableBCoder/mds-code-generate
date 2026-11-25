package com.mds.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.mds.demo.common.IdGenerator;
import com.mds.demo.common.SnowflakeIdGenerator;
import com.mds.demo.domain.device.DeviceKindEnum;
import com.mds.demo.domain.order.OrderStateEnum;
import com.mds.demo.domain.user.UserStatusEnum;
import com.mds.demo.dto.AppUserDto;
import com.mds.demo.dto.DeviceDto;
import com.mds.demo.dto.OrderRecordDto;
import com.mds.demo.model.device.DeviceModelService;
import com.mds.demo.model.order.OrderRecordModelService;
import com.mds.demo.model.user.AppUserModelService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 端到端链路验证：**真实 MySQL** + MBG 产物 + 插件生成物。
 *
 * <p>覆盖三种主键形态：
 * <ul>
 *   <li>{@code AppUser}：{@code id : Long} → 插件注入 {@code IdGenerator}，insert 前自动生成主键</li>
 *   <li>{@code OrderRecord}：{@code orderNo : String} → 主键由调用方给</li>
 *   <li>{@code Device}：{@code uuid : String} → 同上</li>
 * </ul>
 *
 * <p>DTO 带 Lombok {@code @Builder}；生成的实现类里 {@code mapper} / {@code converter} 是
 * {@code protected}，要加领域方法直接写进实现类即可（之后靠 git 对比/回滚）。
 */
@SpringBootTest(classes = CodegenIntegrationTest.TestApp.class)
class CodegenIntegrationTest {

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = "com.mds.demo")
    @MapperScan("com.mds.demo.mapper")
    static class TestApp {
        @Bean
        IdGenerator idGenerator() {
            return new SnowflakeIdGenerator(1);
        }
    }

    @Autowired
    private AppUserModelService userService;

    @Autowired
    private OrderRecordModelService orderService;

    @Autowired
    private DeviceModelService deviceService;

    @Test
    void appUserCrudGeneratesSnowflakeIdAndMapsEnum() {
        AppUserDto dto = newUser("alice");
        userService.insert(dto);
        assertThat(dto.getId()).as("insert 时插件注入的雪花主键应回填到 DTO").isNotNull();

        AppUserDto loaded = userService.findById(dto.getId()).orElseThrow();
        assertThat(loaded.getUsername()).isEqualTo(dto.getUsername());
        assertThat(loaded.getStatus()).as("smallint → 枚举").isEqualTo(UserStatusEnum.NORMAL);

        loaded.setStatus(UserStatusEnum.LOCKED);
        assertThat(userService.updateSelectiveById(loaded.getId(), loaded)).isEqualTo(1);
        assertThat(userService.findById(loaded.getId()).orElseThrow().getStatus())
                .isEqualTo(UserStatusEnum.LOCKED);

        assertThat(userService.findByIds(List.of(dto.getId()))).hasSize(1);
        assertThat(userService.deleteById(dto.getId())).isEqualTo(1);
        assertThat(userService.findById(dto.getId())).isEmpty();
    }

    @Test
    void dtoBuilderIsGenerated() {
        AppUserDto dto = AppUserDto.builder()
                .id(123L)
                .username("builder_user")
                .status(UserStatusEnum.NORMAL)
                .enabled((byte) 1)
                .balance(new BigDecimal("1.2300"))
                .build();
        assertThat(dto.getId()).isEqualTo(123L);
        assertThat(dto.getStatus()).isEqualTo(UserStatusEnum.NORMAL);
    }

    @Test
    void orderRecordUsesCallerProvidedBusinessKey() {
        OrderRecordDto dto = new OrderRecordDto();
        dto.setOrderNo("ORD-" + UUID.randomUUID().toString().substring(0, 8));
        dto.setUserId(1001L);
        dto.setAmount(new BigDecimal("99.9900"));
        dto.setState(OrderStateEnum.CREATED);
        dto.setChannel("app");
        dto.setCreatedAt(LocalDateTime.now());

        orderService.insert(dto);
        assertThat(orderService.findById(dto.getOrderNo())).isPresent();

        dto.setState(OrderStateEnum.PAID);
        assertThat(orderService.updateById(dto.getOrderNo(), dto)).isEqualTo(1);
        assertThat(orderService.findById(dto.getOrderNo()).orElseThrow().getState())
                .isEqualTo(OrderStateEnum.PAID);

        assertThat(orderService.deleteById(dto.getOrderNo())).isEqualTo(1);
    }

    @Test
    void deviceUsesChar36PrimaryKey() {
        DeviceDto dto = new DeviceDto();
        dto.setUuid(UUID.randomUUID().toString());
        dto.setName("pixel-8");
        dto.setKind(DeviceKindEnum.EMULATOR);
        dto.setOnline((byte) 1);
        dto.setLastSeen(LocalDateTime.now());

        deviceService.insert(dto);
        assertThat(deviceService.findById(dto.getUuid())).isPresent();
        assertThat(deviceService.findById(dto.getUuid()).orElseThrow().getKind())
                .isEqualTo(DeviceKindEnum.EMULATOR);
        assertThat(deviceService.deleteById(dto.getUuid())).isEqualTo(1);
    }

    private AppUserDto newUser(String name) {
        AppUserDto dto = new AppUserDto();
        dto.setUsername(name + "_" + UUID.randomUUID().toString().substring(0, 8));
        dto.setDisplayName(name);
        dto.setEnabled((byte) 1);
        dto.setStatus(UserStatusEnum.NORMAL);
        dto.setBalance(new BigDecimal("12.3400"));
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }
}