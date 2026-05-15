package com.bazylev.server.reports.attendance;

import com.bazylev.server.dao.AttendanceDAO;
import com.bazylev.server.dao.GroupStudentDAO;
import com.bazylev.server.dao.PersonDAO;
import com.bazylev.server.dao.StudentDAO;
import com.bazylev.server.models.entities.Attendance;
import com.bazylev.server.models.entities.GroupStudent;
import com.bazylev.server.models.entities.Person;
import com.bazylev.server.models.entities.Student;
import com.bazylev.server.reports.Report;
import com.bazylev.server.reports.ReportFactory;
import com.google.gson.JsonObject;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendanceReportFactory extends ReportFactory {

    private final AttendanceDAO   attendanceDAO   = new AttendanceDAO();
    private final GroupStudentDAO groupStudentDAO = new GroupStudentDAO();
    private final StudentDAO      studentDAO      = new StudentDAO();
    private final PersonDAO       personDAO       = new PersonDAO();

    @Override
    protected Report buildReport(JsonObject params) {
        int groupId    = params.get("groupId").getAsInt();
        LocalDate from = LocalDate.parse(params.get("from").getAsString());
        LocalDate to   = LocalDate.parse(params.get("to").getAsString());

        List<Attendance> records = attendanceDAO.findByGroupIdAndDateRange(groupId, from, to);

        List<GroupStudent> enrollments = groupStudentDAO.findByGroupId(groupId);
        Map<Integer, String> nameByGsId = new HashMap<>();
        for (GroupStudent gs : enrollments) {
            Student student = studentDAO.findById(gs.getStudentId()).orElse(null);
            if (student != null) {
                Person person = personDAO.findById(student.getPersonId()).orElse(null);
                if (person != null) {
                    nameByGsId.put(gs.getId(), safeFullName(person));
                }
            }
        }

        return new AttendanceReport("Журнал посещаемости", records, nameByGsId);
    }

    private static String safeFullName(Person p) {
        String ln = p.getLastName()   != null ? p.getLastName().strip()   : "";
        String fn = p.getFirstName()  != null ? p.getFirstName().strip()  : "";
        String mn = p.getMiddleName() != null ? p.getMiddleName().strip() : "";
        return (ln + " " + fn + " " + mn).trim().replaceAll("\\s+", " ");
    }
}
