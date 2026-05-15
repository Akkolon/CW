package com.bazylev.server.bootstrap;

import com.bazylev.server.dao.PersonDAO;
import com.bazylev.server.dao.UserDAO;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.entities.Person;
import com.bazylev.server.models.entities.User;
import com.bazylev.server.services.AuthService;

import java.util.Optional;

public final class AdminBootstrap {

    private static final String DEFAULT_ADMIN_LOGIN = "stas";
    private static final String DEFAULT_ADMIN_PASSWORD = "1234";

    private AdminBootstrap() {}

    public static void ensureDefaultAdmin() {
        UserDAO userDAO = new UserDAO();

        Optional<User> existing = userDAO.findByLogin(DEFAULT_ADMIN_LOGIN);
        if (existing.isPresent()) {
            User user = existing.get();
            boolean changed = false;
            if (user.getRole() != Role.ADMIN) {
                user.setRole(Role.ADMIN);
                changed = true;
            }
            if (user.isBlocked()) {
                user.setBlocked(false);
                changed = true;
            }
            if (changed) {
                userDAO.update(user);
                System.out.println("Default admin account updated: " + DEFAULT_ADMIN_LOGIN);
            } else {
                System.out.println("Default admin account exists: " + DEFAULT_ADMIN_LOGIN);
            }
            return;
        }

        PersonDAO personDAO = new PersonDAO();

        Person person = new Person();
        person.setFirstName("Admin");
        person.setLastName("Admin");
        person.setMiddleName("");
        person.setEmail("");
        int personId = personDAO.save(person);

        User user = new User();
        user.setLogin(DEFAULT_ADMIN_LOGIN);
        user.setPasswordHash(AuthService.hashPassword(DEFAULT_ADMIN_PASSWORD));
        user.setRole(Role.ADMIN);
        user.setBlocked(false);
        user.setPersonId(personId);
        userDAO.save(user);

        System.out.println("Default admin account created: " + DEFAULT_ADMIN_LOGIN);
    }
}

