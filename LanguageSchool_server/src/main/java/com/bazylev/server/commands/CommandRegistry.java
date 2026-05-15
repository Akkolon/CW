package com.bazylev.server.commands;

import com.bazylev.server.commands.attendance.GetAttendanceCommand;
import com.bazylev.server.commands.attendance.MarkAttendanceCommand;
import com.bazylev.server.commands.auth.LoginCommand;
import com.bazylev.server.commands.auth.LogoutCommand;
import com.bazylev.server.commands.course.CreateCourseCommand;
import com.bazylev.server.commands.course.DeleteCourseCommand;
import com.bazylev.server.commands.course.GetAllCoursesCommand;
import com.bazylev.server.commands.course.UpdateCourseCommand;
import com.bazylev.server.commands.grade.GetAverageGradeCommand;
import com.bazylev.server.commands.grade.GetGradesCommand;
import com.bazylev.server.commands.grade.SetGradeCommand;
import com.bazylev.server.commands.group.CreateGroupCommand;
import com.bazylev.server.commands.group.DropStudentCommand;
import com.bazylev.server.commands.group.EnrollStudentCommand;
import com.bazylev.server.commands.group.GetAllGroupsCommand;
import com.bazylev.server.commands.group.UpdateGroupCommand;
import com.bazylev.server.commands.payment.GetDebtCommand;
import com.bazylev.server.commands.payment.GetDebtorsCommand;
import com.bazylev.server.commands.payment.GetGroupProfitabilityCommand;
import com.bazylev.server.commands.payment.GetPaymentsCommand;
import com.bazylev.server.commands.payment.RegisterPaymentCommand;
import com.bazylev.server.commands.person.GetAllPersonsCommand;
import com.bazylev.server.commands.report.GenerateCertificateCommand;
import com.bazylev.server.commands.report.GenerateReportCommand;
import com.bazylev.server.commands.schedule.GenerateScheduleCommand;
import com.bazylev.server.commands.schedule.GetScheduleCommand;
import com.bazylev.server.commands.schedule.UpdateScheduleSlotCommand;
import com.bazylev.server.commands.student.CreateStudentCommand;
import com.bazylev.server.commands.student.GetAllStudentsCommand;
import com.bazylev.server.commands.student.GetStudentHistoryCommand;
import com.bazylev.server.commands.student.UpdateStudentCommand;
import com.bazylev.server.commands.teacher.CreateTeacherCommand;
import com.bazylev.server.commands.teacher.GetAllTeachersCommand;
import com.bazylev.server.commands.teacher.UpdateTeacherCommand;
import com.bazylev.server.commands.user.BlockUserCommand;
import com.bazylev.server.commands.user.CreateUserCommand;
import com.bazylev.server.commands.user.DeleteUserCommand;
import com.bazylev.server.commands.user.GetActionLogCommand;
import com.bazylev.server.commands.user.GetAllUsersCommand;
import com.bazylev.server.commands.user.UpdateUserCommand;
import com.bazylev.server.enums.RequestType;
import com.bazylev.server.network.SessionManager;
import com.bazylev.server.services.AttendanceService;
import com.bazylev.server.services.AuthService;
import com.bazylev.server.services.CourseService;
import com.bazylev.server.services.GradeService;
import com.bazylev.server.services.GroupService;
import com.bazylev.server.services.PaymentService;
import com.bazylev.server.services.ReportService;
import com.bazylev.server.services.ScheduleService;
import com.bazylev.server.services.StudentService;
import com.bazylev.server.services.TeacherService;
import com.bazylev.server.services.UserService;
import com.bazylev.server.commands.student.RegisterStudentCommand;
import com.bazylev.server.commands.schedule.GetScheduleStatsCommand;
import com.bazylev.server.commands.student.DeleteStudentCommand;
import com.bazylev.server.commands.teacher.DeleteTeacherCommand;
import com.bazylev.server.commands.schedule.DeleteScheduleSlotCommand;
import com.bazylev.server.commands.student.GetStudentsByGroupCommand;
import com.bazylev.server.commands.grade.UpdateGradeCommand;
import com.bazylev.server.commands.grade.DeleteGradeCommand;
import com.bazylev.server.commands.attendance.UpdateAttendanceCommand;
import com.bazylev.server.commands.attendance.DeleteAttendanceCommand;

public class CommandRegistry {

    private CommandRegistry() {}

    public static CommandDispatcher build() {
        AuthService authService         = new AuthService();
        UserService userService         = new UserService();
        CourseService courseService     = new CourseService();
        TeacherService teacherService   = new TeacherService();
        StudentService studentService   = new StudentService();
        GroupService groupService       = new GroupService();
        ScheduleService scheduleService = new ScheduleService();
        AttendanceService attendService = new AttendanceService();
        GradeService gradeService       = new GradeService();
        PaymentService paymentService   = new PaymentService();
        ReportService reportService     = new ReportService();
        SessionManager sessionManager   = SessionManager.getInstance();

        CommandDispatcher dispatcher = new CommandDispatcher();

        // Аутентификация
        dispatcher.register(RequestType.LOGIN,          new LoginCommand(authService));
        dispatcher.register(RequestType.LOGOUT,         new LogoutCommand(sessionManager));

        // Пользователи
        dispatcher.register(RequestType.CREATE_USER,    new CreateUserCommand(userService));
        dispatcher.register(RequestType.UPDATE_USER,    new UpdateUserCommand(userService));
        dispatcher.register(RequestType.DELETE_USER,    new DeleteUserCommand(userService));
        dispatcher.register(RequestType.BLOCK_USER,     new BlockUserCommand(userService));
        dispatcher.register(RequestType.GET_ALL_USERS,  new GetAllUsersCommand(userService));
        dispatcher.register(RequestType.GET_ACTION_LOG, new GetActionLogCommand(userService));

        // Персоны
        dispatcher.register(RequestType.GET_ALL_PERSONS, new GetAllPersonsCommand(userService));

        // Курсы
        dispatcher.register(RequestType.GET_ALL_COURSES, new GetAllCoursesCommand(courseService));
        dispatcher.register(RequestType.CREATE_COURSE,   new CreateCourseCommand(courseService));
        dispatcher.register(RequestType.UPDATE_COURSE,   new UpdateCourseCommand(courseService));
        dispatcher.register(RequestType.DELETE_COURSE,   new DeleteCourseCommand(courseService));

        // Преподаватели
        dispatcher.register(RequestType.GET_ALL_TEACHERS, new GetAllTeachersCommand(teacherService));
        dispatcher.register(RequestType.CREATE_TEACHER,   new CreateTeacherCommand(teacherService));
        dispatcher.register(RequestType.UPDATE_TEACHER,   new UpdateTeacherCommand(teacherService));
        dispatcher.register(RequestType.DELETE_TEACHER,   new DeleteTeacherCommand(teacherService));

        // Студенты
        dispatcher.register(RequestType.GET_ALL_STUDENTS,     new GetAllStudentsCommand(studentService));
        dispatcher.register(RequestType.CREATE_STUDENT,       new CreateStudentCommand(studentService));
        dispatcher.register(RequestType.REGISTER_STUDENT,     new RegisterStudentCommand(studentService));
        dispatcher.register(RequestType.UPDATE_STUDENT,       new UpdateStudentCommand(studentService));
        dispatcher.register(RequestType.GET_STUDENT_HISTORY,  new GetStudentHistoryCommand(studentService));
        dispatcher.register(RequestType.DELETE_STUDENT,       new DeleteStudentCommand(studentService));
        dispatcher.register(RequestType.GET_STUDENTS_BY_GROUP,new GetStudentsByGroupCommand(studentService));

        // Группы
        dispatcher.register(RequestType.GET_ALL_GROUPS, new GetAllGroupsCommand(groupService));
        dispatcher.register(RequestType.CREATE_GROUP,   new CreateGroupCommand(groupService));
        dispatcher.register(RequestType.UPDATE_GROUP,   new UpdateGroupCommand(groupService));
        dispatcher.register(RequestType.ENROLL_STUDENT, new EnrollStudentCommand(groupService));
        dispatcher.register(RequestType.DROP_STUDENT,   new DropStudentCommand(groupService));

        // Расписание
        dispatcher.register(RequestType.GET_SCHEDULE,        new GetScheduleCommand(scheduleService));
        dispatcher.register(RequestType.GENERATE_SCHEDULE,   new GenerateScheduleCommand(scheduleService));
        dispatcher.register(RequestType.UPDATE_SCHEDULE_SLOT,new UpdateScheduleSlotCommand(scheduleService));
        dispatcher.register(RequestType.GET_SCHEDULE_STATS,  new GetScheduleStatsCommand(scheduleService));
        dispatcher.register(RequestType.DELETE_SCHEDULE_SLOT,new DeleteScheduleSlotCommand(scheduleService));

        // Посещаемость
        dispatcher.register(RequestType.MARK_ATTENDANCE,   new MarkAttendanceCommand(attendService));
        dispatcher.register(RequestType.GET_ATTENDANCE,    new GetAttendanceCommand(attendService));
        dispatcher.register(RequestType.UPDATE_ATTENDANCE, new UpdateAttendanceCommand(attendService));
        dispatcher.register(RequestType.DELETE_ATTENDANCE, new DeleteAttendanceCommand(attendService));

        // Оценки
        dispatcher.register(RequestType.SET_GRADE,        new SetGradeCommand(gradeService));
        dispatcher.register(RequestType.GET_GRADES,       new GetGradesCommand(gradeService));
        dispatcher.register(RequestType.GET_AVERAGE_GRADE,new GetAverageGradeCommand(gradeService));
        dispatcher.register(RequestType.UPDATE_GRADE,     new UpdateGradeCommand(gradeService));
        dispatcher.register(RequestType.DELETE_GRADE,     new DeleteGradeCommand(gradeService));

        // Финансы
        dispatcher.register(RequestType.REGISTER_PAYMENT,      new RegisterPaymentCommand(paymentService));
        dispatcher.register(RequestType.GET_PAYMENTS,           new GetPaymentsCommand(paymentService));
        dispatcher.register(RequestType.GET_DEBT,               new GetDebtCommand(paymentService));
        dispatcher.register(RequestType.GET_DEBTORS,            new GetDebtorsCommand(paymentService));
        dispatcher.register(RequestType.GET_GROUP_PROFITABILITY,new GetGroupProfitabilityCommand(paymentService));

        // Отчёты и сертификаты
        dispatcher.register(RequestType.GENERATE_REPORT,      new GenerateReportCommand(reportService));
        dispatcher.register(RequestType.GENERATE_CERTIFICATE, new GenerateCertificateCommand(reportService));

        return dispatcher;
    }
}