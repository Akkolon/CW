package com.bazylev.server.services;

import com.bazylev.server.dao.CourseDAO;
import com.bazylev.server.dao.GroupDAO;
import com.bazylev.server.dao.GroupStudentDAO;
import com.bazylev.server.dao.PaymentDAO;
import com.bazylev.server.dao.StudentDAO;
import com.bazylev.server.enums.EnrollmentStatus;
import com.bazylev.server.enums.GroupStatus;
import com.bazylev.server.enums.PaymentMethod;
import com.bazylev.server.enums.ResponseStatus;
import com.bazylev.server.models.entities.Course;
import com.bazylev.server.models.entities.Group;
import com.bazylev.server.models.entities.GroupStudent;
import com.bazylev.server.models.entities.Payment;
import com.bazylev.server.models.entities.Student;
import com.bazylev.server.models.tcp.Response;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService — модульные тесты")
class PaymentServiceTest {

    @Mock private PaymentDAO      paymentDAO;
    @Mock private StudentDAO      studentDAO;
    @Mock private GroupStudentDAO groupStudentDAO;
    @Mock private GroupDAO        groupDAO;
    @Mock private CourseDAO       courseDAO;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() throws Exception {
        paymentService = new PaymentService();
        injectMock(paymentService, "paymentDAO",      paymentDAO);
        injectMock(paymentService, "studentDAO",      studentDAO);
        injectMock(paymentService, "groupStudentDAO", groupStudentDAO);
        injectMock(paymentService, "groupDAO",        groupDAO);
        injectMock(paymentService, "courseDAO",       courseDAO);
    }

    // ----------------------------------------------------------------
    // registerPayment
    // ----------------------------------------------------------------

    @Test
    @DisplayName("registerPayment: корректный платёж сохраняется")
    void registerPayment_valid_success() {
        when(studentDAO.findById(1)).thenReturn(Optional.of(new Student()));
        when(paymentDAO.save(any(Payment.class))).thenReturn(10);

        String data = """
                {"studentId":1,"amount":5000.00,"paymentMethod":"CASH",
                 "paymentDate":"2025-03-01","receiptNumber":"REC-001"}
                """;
        Response response = paymentService.registerPayment(data);

        assertEquals(ResponseStatus.OK, response.getStatus());
        verify(paymentDAO).save(any(Payment.class));
    }

    @Test
    @DisplayName("registerPayment: нулевая сумма — ошибка")
    void registerPayment_zeroAmount_returnsError() {
        when(studentDAO.findById(1)).thenReturn(Optional.of(new Student()));

        Response response = paymentService.registerPayment(
                "{\"studentId\":1,\"amount\":0}");

        assertEquals(ResponseStatus.ERROR, response.getStatus());
        assertTrue(response.getMessage().contains("положительной"));
        verify(paymentDAO, never()).save(any());
    }

    @Test
    @DisplayName("registerPayment: отрицательная сумма — ошибка")
    void registerPayment_negativeAmount_returnsError() {
        when(studentDAO.findById(1)).thenReturn(Optional.of(new Student()));

        Response response = paymentService.registerPayment(
                "{\"studentId\":1,\"amount\":-100}");

        assertEquals(ResponseStatus.ERROR, response.getStatus());
        verify(paymentDAO, never()).save(any());
    }

    @Test
    @DisplayName("registerPayment: студент не найден — ошибка")
    void registerPayment_studentNotFound_returnsError() {
        when(studentDAO.findById(999)).thenReturn(Optional.empty());

        Response response = paymentService.registerPayment(
                "{\"studentId\":999,\"amount\":1000}");

        assertEquals(ResponseStatus.ERROR, response.getStatus());
        verify(paymentDAO, never()).save(any());
    }

    @Test
    @DisplayName("registerPayment: неизвестный способ оплаты — ошибка")
    void registerPayment_unknownMethod_returnsError() {
        when(studentDAO.findById(1)).thenReturn(Optional.of(new Student()));

        Response response = paymentService.registerPayment(
                "{\"studentId\":1,\"amount\":1000,\"paymentMethod\":\"BITCOIN\"}");

        assertEquals(ResponseStatus.ERROR, response.getStatus());
        verify(paymentDAO, never()).save(any());
    }

    @Test
    @DisplayName("registerPayment: отсутствует studentId — ошибка")
    void registerPayment_missingStudentId_returnsError() {
        Response response = paymentService.registerPayment("{\"amount\":1000}");
        assertEquals(ResponseStatus.ERROR, response.getStatus());
    }

    // ----------------------------------------------------------------
    // getDebt — расчёт задолженности
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getDebt: задолженность = начислено − оплачено")
    void getDebt_correctCalculation() {
        GroupStudent gs = new GroupStudent(1, 2, 1,
                LocalDate.now().minusMonths(2), EnrollmentStatus.ACTIVE);
        Group group = new Group(2, "Группа", 3, 1,
                LocalDate.now().minusMonths(2), null, 10, GroupStatus.IN_PROGRESS);
        Course course = new Course(3, "Английский A1", "", 80,
                "A1", new BigDecimal("3000.00"), true);

        when(groupStudentDAO.findByStudentId(1)).thenReturn(List.of(gs));
        when(groupDAO.findById(2)).thenReturn(Optional.of(group));
        when(courseDAO.findById(3)).thenReturn(Optional.of(course));
        when(paymentDAO.getTotalPaidByStudent(1)).thenReturn(new BigDecimal("3000.00"));

        Response response = paymentService.getDebt("{\"studentId\":1}");

        assertEquals(ResponseStatus.OK, response.getStatus());
        JsonObject result = JsonParser.parseString(response.getData()).getAsJsonObject();

        BigDecimal debt = result.get("debt").getAsBigDecimal();
        assertTrue(debt.compareTo(BigDecimal.ZERO) >= 0,
                "Задолженность не должна быть отрицательной");
    }

    @Test
    @DisplayName("getDebt: нет начислений — задолженность 0")
    void getDebt_noEnrollments_zeroDebt() {
        when(groupStudentDAO.findByStudentId(5)).thenReturn(List.of());
        when(paymentDAO.getTotalPaidByStudent(5)).thenReturn(BigDecimal.ZERO);

        Response response = paymentService.getDebt("{\"studentId\":5}");

        assertEquals(ResponseStatus.OK, response.getStatus());
        JsonObject result = JsonParser.parseString(response.getData()).getAsJsonObject();
        assertEquals(0, result.get("debt").getAsBigDecimal().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("getDebt: null данные — ошибка")
    void getDebt_nullData_returnsError() {
        Response response = paymentService.getDebt(null);
        assertEquals(ResponseStatus.ERROR, response.getStatus());
    }

    // ----------------------------------------------------------------
    // getGroupProfitability — рентабельность группы
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getGroupProfitability: рентабельность рассчитывается корректно")
    void getGroupProfitability_valid() {
        Group group = new Group(1, "Группа", 2, 1,
                LocalDate.now().minusMonths(1), null, 10, GroupStatus.IN_PROGRESS);
        Course course = new Course(2, "Курс", "", 80,
                "B1", new BigDecimal("5000.00"), true);
        GroupStudent gs = new GroupStudent(1, 1, 10,
                LocalDate.now().minusMonths(1), EnrollmentStatus.ACTIVE);

        when(groupDAO.findById(1)).thenReturn(Optional.of(group));
        when(groupStudentDAO.findByGroupId(1)).thenReturn(List.of(gs));
        when(paymentDAO.getTotalPaidByStudent(10)).thenReturn(new BigDecimal("5000.00"));
        when(courseDAO.findById(2)).thenReturn(Optional.of(course));

        Response response = paymentService.getGroupProfitability("{\"groupId\":1}");

        assertEquals(ResponseStatus.OK, response.getStatus());
        JsonObject result = JsonParser.parseString(response.getData()).getAsJsonObject();
        assertTrue(result.has("profitability"));
        assertTrue(result.has("totalRevenue"));
        assertTrue(result.has("profit"));
    }

    @Test
    @DisplayName("getGroupProfitability: группа не найдена — ошибка")
    void getGroupProfitability_groupNotFound_returnsError() {
        when(groupDAO.findById(999)).thenReturn(Optional.empty());

        Response response = paymentService.getGroupProfitability("{\"groupId\":999}");
        assertEquals(ResponseStatus.ERROR, response.getStatus());
    }

    // ----------------------------------------------------------------
    // Вспомогательные методы
    // ----------------------------------------------------------------

    private void injectMock(Object target, String fieldName, Object mock) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, mock);
    }
}
