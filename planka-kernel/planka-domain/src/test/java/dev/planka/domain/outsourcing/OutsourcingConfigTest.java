//package dev.planka.domain.outsourcing;
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//
///**
// * OutsourcingConfig 单元测试
// */
//@DisplayName("OutsourcingConfig 测试")
//class OutsourcingConfigTest {
//
//    private static final String ORG_ID = "org-test-001";
//    private static final String CONFIG_NAME = "考勤配置";
//
//    @Nested
//    @DisplayName("基本属性测试")
//    class BasicPropertiesTests {
//
//        @Test
//        @DisplayName("创建配置时应设置默认值")
//        void shouldSetDefaultValues() {
//            // When
//            OutsourcingConfig config = new OutsourcingConfig(
//                    OutsourcingConfigId.generate(),
//                    ORG_ID,
//                    CONFIG_NAME
//            );
//
//            // Then
//            assertThat(config.getDurationUnit()).isEqualTo(DurationUnit.MINUTE);
//            assertThat(config.getDecimalScale()).isEqualTo(0);
//            assertThat(config.getCardAttendanceRequired()).isFalse();
//        }
//
//        @Test
//        @DisplayName("应正确返回 SchemaType")
//        void shouldReturnCorrectSchemaType() {
//            // Given
//            OutsourcingConfig config = createDefaultConfig();
//
//            // Then
//            assertThat(config.getSchemaType()).isEqualTo(dev.planka.domain.schema.SchemaType.OUTSOURCING_CONFIG);
//        }
//
//        @Test
//        @DisplayName("应正确返回 SchemaSubType")
//        void shouldReturnCorrectSchemaSubType() {
//            // Given
//            OutsourcingConfig config = createDefaultConfig();
//
//            // Then
//            assertThat(config.getSchemaSubType()).isEqualTo(dev.planka.domain.schema.SchemaSubType.OUTSOURCING_CONFIG);
//        }
//
//        @Test
//        @DisplayName("belongTo 应返回 null（组织级别配置）")
//        void shouldReturnNullForBelongTo() {
//            // Given
//            OutsourcingConfig config = createDefaultConfig();
//
//            // Then
//            assertThat(config.belongTo()).isNull();
//        }
//
//        @Test
//        @DisplayName("secondKeys 应返回空集合")
//        void shouldReturnEmptySecondKeys() {
//            // Given
//            OutsourcingConfig config = createDefaultConfig();
//
//            // Then
//            assertThat(config.secondKeys()).isEmpty();
//        }
//    }
//
//    @Nested
//    @DisplayName("配置校验测试")
//    class ValidationTests {
//
//        @Test
//        @DisplayName("小数位数为负数时应抛出异常")
//        void shouldThrowExceptionWhenDecimalScaleIsNegative() {
//            // Given
//            OutsourcingConfig config = createDefaultConfig();
//            config.setDecimalScale(-1);
//
//            // When & Then
//            assertThatThrownBy(config::validate)
//                    .isInstanceOf(IllegalArgumentException.class)
//                    .hasMessageContaining("decimalScale 必须 ≥ 0");
//        }
//
//        @Test
//        @DisplayName("小数位数为 0 时应通过校验")
//        void shouldPassValidationWhenDecimalScaleIsZero() {
//            // Given
//            OutsourcingConfig config = createDefaultConfig();
//            config.setDecimalScale(0);
//
//            // When & Then
//            config.validate(); // 不应抛出异常
//        }
//
//        @Test
//        @DisplayName("小数位数为正数时应通过校验")
//        void shouldPassValidationWhenDecimalScaleIsPositive() {
//            // Given
//            OutsourcingConfig config = createDefaultConfig();
//            config.setDecimalScale(2);
//
//            // When & Then
//            config.validate(); // 不应抛出异常
//        }
//    }
//
//    @Nested
//    @DisplayName("签到配置校验测试")
//    class AttendanceConfValidationTests {
//
//        @Test
//        @DisplayName("工作结束时间早于开始时间时应抛出异常")
//        void shouldThrowExceptionWhenWorkEndBeforeStart() {
//            // Given
//            OutsourcingConfig config = createDefaultConfig();
//            AttendanceConf attendanceConf = new AttendanceConf();
//            attendanceConf.setWorkStart("17:30");
//            attendanceConf.setWorkEnd("08:30");
//            attendanceConf.setLunchStart("11:30");
//            attendanceConf.setLunchEnd("13:30");
//            attendanceConf.setWorkDuration(8.0);
//            config.setAttendanceConf(attendanceConf);
//
//            // When & Then
//            assertThatThrownBy(config::validate)
//                    .isInstanceOf(IllegalArgumentException.class)
//                    .hasMessageContaining("工作结束时间必须晚于工作开始时间");
//        }
//
//        @Test
//        @DisplayName("午休结束时间早于开始时间时应抛出异常")
//        void shouldThrowExceptionWhenLunchEndBeforeStart() {
//            // Given
//            OutsourcingConfig config = createDefaultConfig();
//            AttendanceConf attendanceConf = new AttendanceConf();
//            attendanceConf.setWorkStart("08:30");
//            attendanceConf.setWorkEnd("17:30");
//            attendanceConf.setLunchStart("13:30");
//            attendanceConf.setLunchEnd("11:30");
//            attendanceConf.setWorkDuration(8.0);
//            config.setAttendanceConf(attendanceConf);
//
//            // When & Then
//            assertThatThrownBy(config::validate)
//                    .isInstanceOf(IllegalArgumentException.class)
//                    .hasMessageContaining("午休结束时间必须晚于午休开始时间");
//        }
//
//        @Test
//        @DisplayName("午休时间超出工作时间时应抛出异常")
//        void shouldThrowExceptionWhenLunchOutsideWorkTime() {
//            // Given
//            OutsourcingConfig config = createDefaultConfig();
//            AttendanceConf attendanceConf = new AttendanceConf();
//            attendanceConf.setWorkStart("08:30");
//            attendanceConf.setWorkEnd("17:30");
//            attendanceConf.setLunchStart("07:00");
//            attendanceConf.setLunchEnd("08:00");
//            attendanceConf.setWorkDuration(8.0);
//            config.setAttendanceConf(attendanceConf);
//
//            // When & Then
//            assertThatThrownBy(config::validate)
//                    .isInstanceOf(IllegalArgumentException.class)
//                    .hasMessageContaining("午休时间必须在工作时间段内");
//        }
//
//        @Test
//        @DisplayName("时间格式错误时应抛出异常")
//        void shouldThrowExceptionWhenTimeFormatInvalid() {
//            // Given
//            OutsourcingConfig config = createDefaultConfig();
//            AttendanceConf attendanceConf = new AttendanceConf();
//            attendanceConf.setWorkStart("25:00");
//            attendanceConf.setWorkEnd("17:30");
//            attendanceConf.setLunchStart("11:30");
//            attendanceConf.setLunchEnd("13:30");
//            attendanceConf.setWorkDuration(8.0);
//            config.setAttendanceConf(attendanceConf);
//
//            // When & Then
//            assertThatThrownBy(config::validate)
//                    .isInstanceOf(IllegalArgumentException.class)
//                    .hasMessageContaining("格式错误");
//        }
//
//        @Test
//        @DisplayName("工作时长为 0 时应抛出异常")
//        void shouldThrowExceptionWhenWorkDurationIsZero() {
//            // Given
//            OutsourcingConfig config = createDefaultConfig();
//            AttendanceConf attendanceConf = new AttendanceConf();
//            attendanceConf.setWorkStart("08:30");
//            attendanceConf.setWorkEnd("17:30");
//            attendanceConf.setLunchStart("11:30");
//            attendanceConf.setLunchEnd("13:30");
//            attendanceConf.setWorkDuration(0.0);
//            config.setAttendanceConf(attendanceConf);
//
//            // When & Then
//            assertThatThrownBy(config::validate)
//                    .isInstanceOf(IllegalArgumentException.class)
//                    .hasMessageContaining("工作时长必须 > 0");
//        }
//
//        @Test
//        @DisplayName("正确的签到配置应通过校验")
//        void shouldPassValidationWithCorrectAttendanceConf() {
//            // Given
//            OutsourcingConfig config = createDefaultConfig();
//            AttendanceConf attendanceConf = new AttendanceConf();
//            attendanceConf.setWorkStart("08:30");
//            attendanceConf.setWorkEnd("17:30");
//            attendanceConf.setLunchStart("11:30");
//            attendanceConf.setLunchEnd("13:30");
//            attendanceConf.setWorkDuration(8.0);
//            config.setAttendanceConf(attendanceConf);
//
//            // When & Then
//            config.validate(); // 不应抛出异常
//        }
//    }
//
//    @Nested
//    @DisplayName("请假配置校验测试")
//    class LeaveConfValidationTests {
//
//        @Test
//        @DisplayName("请假限制规则的 range 为空时应抛出异常")
//        void shouldThrowExceptionWhenLimitRuleRangeIsNull() {
//            // Given
//            OutsourcingConfig config = createDefaultConfig();
//            LeaveConf leaveConf = new LeaveConf();
//            LeaveConf.LeaveLimitRule rule = new LeaveConf.LeaveLimitRule();
//            rule.setRange(null);
//            rule.setItemId("leave-type-001");
//            rule.setLimit(10);
//            leaveConf.setLimitRules(java.util.List.of(rule));
//            config.setLeaveConf(leaveConf);
//
//            // When & Then
//            assertThatThrownBy(config::validate)
//                    .isInstanceOf(IllegalArgumentException.class)
//                    .hasMessageContaining("range 不能为空");
//        }
//
//        @Test
//        @DisplayName("请假限制规则的 itemId 为空时应抛出异常")
//        void shouldThrowExceptionWhenLimitRuleItemIdIsBlank() {
//            // Given
//            OutsourcingConfig config = createDefaultConfig();
//            LeaveConf leaveConf = new LeaveConf();
//            LeaveConf.LeaveLimitRule rule = new LeaveConf.LeaveLimitRule();
//            rule.setRange(DateUnit.MONTH);
//            rule.setItemId("");
//            rule.setLimit(10);
//            leaveConf.setLimitRules(java.util.List.of(rule));
//            config.setLeaveConf(leaveConf);
//
//            // When & Then
//            assertThatThrownBy(config::validate)
//                    .isInstanceOf(IllegalArgumentException.class)
//                    .hasMessageContaining("itemId 不能为空");
//        }
//
//        @Test
//        @DisplayName("请假限制规则的 limit 为负数时应抛出异常")
//        void shouldThrowExceptionWhenLimitRuleLimitIsNegative() {
//            // Given
//            OutsourcingConfig config = createDefaultConfig();
//            LeaveConf leaveConf = new LeaveConf();
//            LeaveConf.LeaveLimitRule rule = new LeaveConf.LeaveLimitRule();
//            rule.setRange(DateUnit.MONTH);
//            rule.setItemId("leave-type-001");
//            rule.setLimit(-1);
//            leaveConf.setLimitRules(java.util.List.of(rule));
//            config.setLeaveConf(leaveConf);
//
//            // When & Then
//            assertThatThrownBy(config::validate)
//                    .isInstanceOf(IllegalArgumentException.class)
//                    .hasMessageContaining("limit 必须 ≥ 0");
//        }
//    }
//
//    @Nested
//    @DisplayName("结算配置校验测试")
//    class SettlementConfValidationTests {
//
//        @Test
//        @DisplayName("成员卡片类型列表为空时应抛出异常")
//        void shouldThrowExceptionWhenVutIdsIsEmpty() {
//            // Given
//            OutsourcingConfig config = createDefaultConfig();
//            SettlementConf settlementConf = new SettlementConf();
//            settlementConf.setMethod(SettlementMethod.MANUAL);
//            settlementConf.setAbsenteeismDeductionCoefficient(2);
//            settlementConf.setDurationUnit(DurationUnit.MINUTE);
//            settlementConf.setDecimalScale(0);
//            settlementConf.setVutIds(java.util.List.of());
//            config.setSettlementConf(settlementConf);
//
//            // When & Then
//            assertThatThrownBy(config::validate)
//                    .isInstanceOf(IllegalArgumentException.class)
//                    .hasMessageContaining("成员卡片类型ID列表不能为空");
//        }
//
//        @Test
//        @DisplayName("缺勤抵扣系数为负数时应抛出异常")
//        void shouldThrowExceptionWhenAbsenteeismDeductionCoefficientIsNegative() {
//            // Given
//            OutsourcingConfig config = createDefaultConfig();
//            SettlementConf settlementConf = new SettlementConf();
//            settlementConf.setMethod(SettlementMethod.MANUAL);
//            settlementConf.setAbsenteeismDeductionCoefficient(-1);
//            settlementConf.setDurationUnit(DurationUnit.MINUTE);
//            settlementConf.setDecimalScale(0);
//            settlementConf.setVutIds(java.util.List.of("card-type-member"));
//            config.setSettlementConf(settlementConf);
//
//            // When & Then
//            assertThatThrownBy(config::validate)
//                    .isInstanceOf(IllegalArgumentException.class)
//                    .hasMessageContaining("缺勤抵扣系数必须 ≥ 0");
//        }
//    }
//
//    // ==================== 辅助方法 ====================
//
//    private OutsourcingConfig createDefaultConfig() {
//        return new OutsourcingConfig(
//                OutsourcingConfigId.generate(),
//                ORG_ID,
//                CONFIG_NAME
//        );
//    }
//}
