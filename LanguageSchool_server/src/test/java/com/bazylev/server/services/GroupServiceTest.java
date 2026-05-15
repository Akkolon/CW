package com.bazylev.server.services;

import com.bazylev.server.dao.CourseDAO;
import com.bazylev.server.dao.GroupDAO;
import com.bazylev.server.dao.GroupStudentDAO;
import com.bazylev.server.dao.StudentDAO;
import com.bazylev.server.enums.EnrollmentStatus;
import com.bazylev.server.enums.GroupStatus;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.entities.Group;
import com.bazylev.server.models.entities.GroupStudent;
import com.bazylev.server.models.entities.Student;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.enums.ResponseStatus;
import com.bazylev.server.network.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroupService — модульные тесты зачисления")
class GroupServiceTest {

    @Mock private GroupDAO        groupDAO;
    @Mock private GroupStudentDAO groupStudentDAO;
    @Mock private StudentDAO      studentDAO;
    @Mock private CourseDAO       courseDAO;

    private GroupService groupService;
    private Session adminSession;

    @BeforeEach
    void setUp() throws Exception {
        groupService = new GroupService();
        injectMock(groupService, "groupDAO",        groupDAO);
        injectMock(groupService, "groupStudentDAO", groupStudentDAO);
        injectMock(groupService, "studentDAO",      studentDAO);
        injectMock(groupService, "courseDAO",       courseDAO);
        adminSession = new Session("token-admin", 1, "admin", Role.ADMIN);
    }

    // ----------------------------------------------------------------
    // enrollStudent — проверка мест
    // ----------------------------------------------------------------

    @Test
    @DisplayName("enrollStudent: успешное зачисление при наличии мест")
    void enrollStudent_freePlace_success() throws Exception {
        Group group = activeGroup(5, 15);
        Student student = activeStudent(10);

        when(groupDAO.findById(5)).thenReturn(Optional.of(group));
        when(groupDAO.countActiveStudents(5)).thenReturn(10);
        when(studentDAO.findById(10)).thenReturn(Optional.of(student));
        when(groupStudentDAO.findActiveByGroupAndStudent(5, 10)).thenReturn(Optional.empty());
        when(groupStudentDAO.save(any(GroupStudent.class))).thenReturn(1);

        Response response = groupService.enrollStudent(
                "{\"groupId\":5,\"studentId\":10}");

        assertEquals(ResponseStatus.OK, response.getStatus());
        verify(groupStudentDAO, times(1)).save(any(GroupStudent.class));
    }

    @Test
    @DisplayName("enrollStudent: группа заполнена — ошибка")
    void enrollStudent_groupFull_returnsError() {
        Group group = activeGroup(5, 10);

        when(groupDAO.findById(5)).thenReturn(Optional.of(group));
        when(groupDAO.countActiveStudents(5)).thenReturn(10);

        Response response = groupService.enrollStudent(
                "{\"groupId\":5,\"studentId\":3}");

        assertEquals(ResponseStatus.ERROR, response.getStatus());
        assertTrue(response.getMessage().contains("мест"));
        verify(groupStudentDAO, never()).save(any());
    }

    @Test
    @DisplayName("enrollStudent: группа завершена — ошибка")
    void enrollStudent_completedGroup_returnsError() {
        Group group = new Group(6, "Группа B", 1, 1,
                LocalDate.now(), null, 15, GroupStatus.COMPLETED);

        when(groupDAO.findById(6)).thenReturn(Optional.of(group));

        Response response = groupService.enrollStudent(
                "{\"groupId\":6,\"studentId\":3}");

        assertEquals(ResponseStatus.ERROR, response.getStatus());
        assertTrue(response.getMessage().contains("завершённую"));
        verify(groupStudentDAO, never()).save(any());
    }

    @Test
    @DisplayName("enrollStudent: студент уже зачислен — ошибка")
    void enrollStudent_alreadyEnrolled_returnsError() {
        Group group = activeGroup(5, 15);
        Student student = activeStudent(10);
        GroupStudent existing = new GroupStudent(99, 5, 10,
                LocalDate.now(), EnrollmentStatus.ACTIVE);

        when(groupDAO.findById(5)).thenReturn(Optional.of(group));
        when(groupDAO.countActiveStudents(5)).thenReturn(5);
        when(studentDAO.findById(10)).thenReturn(Optional.of(student));
        when(groupStudentDAO.findActiveByGroupAndStudent(5, 10))
                .thenReturn(Optional.of(existing));

        Response response = groupService.enrollStudent(
                "{\"groupId\":5,\"studentId\":10}");

        assertEquals(ResponseStatus.ERROR, response.getStatus());
        assertTrue(response.getMessage().contains("уже зачислен"));
        verify(groupStudentDAO, never()).save(any());
    }

    @Test
    @DisplayName("enrollStudent: группа не найдена — ошибка")
    void enrollStudent_groupNotFound_returnsError() {
        when(groupDAO.findById(999)).thenReturn(Optional.empty());

        Response response = groupService.enrollStudent(
                "{\"groupId\":999,\"studentId\":1}");

        assertEquals(ResponseStatus.ERROR, response.getStatus());
        assertTrue(response.getMessage().contains("не найдена"));
    }

    @Test
    @DisplayName("enrollStudent: студент не найден — ошибка")
    void enrollStudent_studentNotFound_returnsError() {
        Group group = activeGroup(5, 15);

        when(groupDAO.findById(5)).thenReturn(Optional.of(group));
        when(groupDAO.countActiveStudents(5)).thenReturn(3);
        when(studentDAO.findById(999)).thenReturn(Optional.empty());

        Response response = groupService.enrollStudent(
                "{\"groupId\":5,\"studentId\":999}");

        assertEquals(ResponseStatus.ERROR, response.getStatus());
        assertTrue(response.getMessage().contains("не найден"));
    }

    @Test
    @DisplayName("enrollStudent: отсутствуют обязательные поля — ошибка")
    void enrollStudent_missingFields_returnsError() {
        Response response = groupService.enrollStudent("{\"groupId\":5}");
        assertEquals(ResponseStatus.ERROR, response.getStatus());

        response = groupService.enrollStudent("{\"studentId\":1}");
        assertEquals(ResponseStatus.ERROR, response.getStatus());
    }

    @Test
    @DisplayName("enrollStudent: null данные — ошибка")
    void enrollStudent_nullData_returnsError() {
        Response response = groupService.enrollStudent(null);
        assertEquals(ResponseStatus.ERROR, response.getStatus());
    }

    // ----------------------------------------------------------------
    // dropStudent
    // ----------------------------------------------------------------

    @Test
    @DisplayName("dropStudent: успешное отчисление активного зачисления")
    void dropStudent_activeEnrollment_success() {
        GroupStudent gs = new GroupStudent(10, 5, 3,
                LocalDate.now(), EnrollmentStatus.ACTIVE);

        when(groupStudentDAO.findActiveByGroupAndStudent(5, 3))
                .thenReturn(Optional.of(gs));

        Response response = groupService.dropStudent(
                "{\"groupId\":5,\"studentId\":3}");

        assertEquals(ResponseStatus.OK, response.getStatus());
        verify(groupStudentDAO).updateStatus(10, EnrollmentStatus.DROPPED);
    }

    @Test
    @DisplayName("dropStudent: зачисление не найдено — ошибка")
    void dropStudent_notFound_returnsError() {
        when(groupStudentDAO.findActiveByGroupAndStudent(5, 3))
                .thenReturn(Optional.empty());

        Response response = groupService.dropStudent(
                "{\"groupId\":5,\"studentId\":3}");

        assertEquals(ResponseStatus.ERROR, response.getStatus());
        verify(groupStudentDAO, never()).updateStatus(anyInt(), any());
    }

    // ----------------------------------------------------------------
    // createGroup
    // ----------------------------------------------------------------

    @Test
    @DisplayName("createGroup: корректные данные — группа создаётся")
    void createGroup_validData_success() {
        when(groupDAO.save(any(Group.class))).thenReturn(1);

        String data = """
                {
                  "name": "Английский A1",
                  "courseId": 1,
                  "teacherId": 2,
                  "maxStudents": 12,
                  "startDate": "2025-09-01"
                }
                """;
        Response response = groupService.createGroup(data);

        assertEquals(ResponseStatus.OK, response.getStatus());
        verify(groupDAO, times(1)).save(any(Group.class));
    }

    @Test
    @DisplayName("createGroup: отсутствует courseId — ошибка")
    void createGroup_missingCourseId_returnsError() {
        Response response = groupService.createGroup("{\"teacherId\":1}");
        assertEquals(ResponseStatus.ERROR, response.getStatus());
        verify(groupDAO, never()).save(any());
    }

    // ----------------------------------------------------------------
    // Вспомогательные методы
    // ----------------------------------------------------------------

    private Group activeGroup(int id, int maxStudents) {
        return new Group(id, "Тестовая группа", 1, 1,
                LocalDate.now(), null, maxStudents, GroupStatus.IN_PROGRESS);
    }

    private Student activeStudent(int id) {
        return new Student(id, 20, LocalDate.now(), true);
    }

    private void injectMock(Object target, String fieldName, Object mock) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, mock);
    }
}
