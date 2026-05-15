package com.bazylev.server.services;

import com.bazylev.server.dao.GradeDAO;
import com.bazylev.server.dao.GroupStudentDAO;
import com.bazylev.server.enums.GradeType;
import com.bazylev.server.models.entities.Grade;
import com.bazylev.server.models.entities.GroupStudent;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.enums.ResponseStatus;
import com.bazylev.server.network.Session;
import com.bazylev.server.enums.Role;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GradeService — модульные тесты")
class GradeServiceTest {

    @Mock
    private GradeDAO gradeDAO;

    @Mock
    private GroupStudentDAO groupStudentDAO;

    private GradeService gradeService;

    private Session teacherSession;

    @BeforeEach
    void setUp() throws Exception {
        gradeService = new GradeService();
        injectMock(gradeService, "gradeDAO", gradeDAO);
        injectMock(gradeService, "groupStudentDAO", groupStudentDAO);
        teacherSession = new Session("token-1", 10, "teacher1", Role.TEACHER);
    }

    // ----------------------------------------------------------------
    // setGrade — валидация входных данных
    // ----------------------------------------------------------------

    @Test
    @DisplayName("setGrade: оценка в допустимом диапазоне сохраняется")
    void setGrade_validData_returnsOk() {
        when(gradeDAO.save(any(Grade.class))).thenReturn(1);

        String data = """
                {"groupStudentId":1,"value":8.5,"gradeType":"HOMEWORK","gradeDate":"2025-03-01"}
                """;
        Response response = gradeService.setGrade(data);

        assertEquals(ResponseStatus.OK, response.getStatus());
        verify(gradeDAO, times(1)).save(any(Grade.class));
    }

    @Test
    @DisplayName("setGrade: оценка выше 10 отклоняется")
    void setGrade_valueAboveMax_returnsError() {
        String data = """
                {"groupStudentId":1,"value":11.0,"gradeType":"TEST"}
                """;
        Response response = gradeService.setGrade(data);

        assertEquals(ResponseStatus.ERROR, response.getStatus());
        assertTrue(response.getMessage().contains("диапазоне"));
        verify(gradeDAO, never()).save(any());
    }

    @Test
    @DisplayName("setGrade: отрицательная оценка отклоняется")
    void setGrade_negativeValue_returnsError() {
        String data = """
                {"groupStudentId":1,"value":-1.0}
                """;
        Response response = gradeService.setGrade(data);

        assertEquals(ResponseStatus.ERROR, response.getStatus());
        verify(gradeDAO, never()).save(any());
    }

    @Test
    @DisplayName("setGrade: отсутствует groupStudentId — ошибка")
    void setGrade_missingGroupStudentId_returnsError() {
        String data = """
                {"value":7.0,"gradeType":"EXAM"}
                """;
        Response response = gradeService.setGrade(data);

        assertEquals(ResponseStatus.ERROR, response.getStatus());
        verify(gradeDAO, never()).save(any());
    }

    @Test
    @DisplayName("setGrade: неизвестный тип оценки — ошибка")
    void setGrade_unknownGradeType_returnsError() {
        String data = """
                {"groupStudentId":1,"value":7.0,"gradeType":"UNKNOWN"}
                """;
        Response response = gradeService.setGrade(data);

        assertEquals(ResponseStatus.ERROR, response.getStatus());
        verify(gradeDAO, never()).save(any());
    }

    @Test
    @DisplayName("setGrade: пустые данные — ошибка")
    void setGrade_nullData_returnsError() {
        Response response = gradeService.setGrade(null);
        assertEquals(ResponseStatus.ERROR, response.getStatus());

        response = gradeService.setGrade("   ");
        assertEquals(ResponseStatus.ERROR, response.getStatus());
    }

    // ----------------------------------------------------------------
    // getAverageGrade — расчёт среднего балла
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getAverageGrade: корректно возвращает средний балл по groupStudentId")
    void getAverageGrade_byGroupStudentId_returnsCorrectAverage() {
        when(gradeDAO.calcAverageByGroupStudent(5)).thenReturn(OptionalDouble.of(7.25));

        Response response = gradeService.getAverageGrade(
                "{\"groupStudentId\":5}", teacherSession);

        assertEquals(ResponseStatus.OK, response.getStatus());
        JsonObject result = JsonParser.parseString(response.getData()).getAsJsonObject();
        assertEquals(5, result.get("groupStudentId").getAsInt());
        assertEquals(7.25, result.get("average").getAsDouble(), 0.001);
    }

    @Test
    @DisplayName("getAverageGrade: нет оценок — средний балл 0.0")
    void getAverageGrade_noGrades_returnsZero() {
        when(gradeDAO.calcAverageByGroupStudent(7)).thenReturn(OptionalDouble.empty());

        Response response = gradeService.getAverageGrade(
                "{\"groupStudentId\":7}", teacherSession);

        assertEquals(ResponseStatus.OK, response.getStatus());
        JsonObject result = JsonParser.parseString(response.getData()).getAsJsonObject();
        assertEquals(0.0, result.get("average").getAsDouble(), 0.001);
    }

    @Test
    @DisplayName("getAverageGrade: по groupId возвращает список средних баллов")
    void getAverageGrade_byGroupId_returnsList() {
        GroupStudent gs1 = new GroupStudent(1, 3, 10, LocalDate.now(),
                com.bazylev.server.enums.EnrollmentStatus.ACTIVE);
        GroupStudent gs2 = new GroupStudent(2, 3, 11, LocalDate.now(),
                com.bazylev.server.enums.EnrollmentStatus.ACTIVE);

        when(groupStudentDAO.findByGroupId(3)).thenReturn(List.of(gs1, gs2));
        when(gradeDAO.calcAverageByGroupStudent(1)).thenReturn(OptionalDouble.of(8.0));
        when(gradeDAO.calcAverageByGroupStudent(2)).thenReturn(OptionalDouble.of(6.5));

        Response response = gradeService.getAverageGrade(
                "{\"groupId\":3}", teacherSession);

        assertEquals(ResponseStatus.OK, response.getStatus());
        var arr = JsonParser.parseString(response.getData()).getAsJsonArray();
        assertEquals(2, arr.size());
    }

    @Test
    @DisplayName("getAverageGrade: без параметров — ошибка")
    void getAverageGrade_noParams_returnsError() {
        Response response = gradeService.getAverageGrade("{}", teacherSession);
        assertEquals(ResponseStatus.ERROR, response.getStatus());
    }

    // ----------------------------------------------------------------
    // getGrades — получение оценок
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getGrades: по groupStudentId возвращает список оценок")
    void getGrades_byGroupStudentId_returnsList() {
        List<Grade> grades = List.of(
                new Grade(1, 1, LocalDate.now(), 9.0, GradeType.TEST, null),
                new Grade(2, 1, LocalDate.now(), 7.5, GradeType.HOMEWORK, null)
        );
        when(gradeDAO.findByGroupStudentId(1)).thenReturn(grades);

        Response response = gradeService.getGrades("{\"groupStudentId\":1}", teacherSession);

        assertEquals(ResponseStatus.OK, response.getStatus());
        var arr = JsonParser.parseString(response.getData()).getAsJsonArray();
        assertEquals(2, arr.size());
    }

    // ----------------------------------------------------------------
    // Вспомогательный метод: инъекция мока через рефлексию
    // ----------------------------------------------------------------
    private void injectMock(Object target, String fieldName, Object mock) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, mock);
    }
}
