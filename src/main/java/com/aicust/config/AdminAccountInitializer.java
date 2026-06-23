package com.aicust.config;

import com.aicust.model.User;
import com.aicust.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.init-enabled:true}")
    private boolean initEnabled;

    @Value("${app.admin.username:admin}")
    private String username;

    @Value("${app.admin.password:admin123}")
    private String password;

    @Value("${app.admin.balance:100000}")
    private Integer balance;

    @Value("${app.admin.reset-password:false}")
    private boolean resetPassword;

    public AdminAccountInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!initEnabled) {
            log.info("Default admin account initialization is disabled");
            return;
        }

        userRepository.findByUsername(username).ifPresentOrElse(user -> {
            boolean changed = false;
            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                user.setRole("ADMIN");
                changed = true;
            }
            if (user.getBalance() == null) {
                user.setBalance(balance);
                changed = true;
            }
            if (resetPassword) {
                user.setPassword(passwordEncoder.encode(password));
                changed = true;
            }
            if (changed) {
                userRepository.save(user);
                log.info("Default admin account '{}' updated", username);
            } else {
                log.info("Default admin account '{}' already exists", username);
            }
        }, () -> {
            User admin = new User();
            admin.setUsername(username);
            admin.setPassword(passwordEncoder.encode(password));
            admin.setBalance(balance);
            admin.setRole("ADMIN");
            userRepository.save(admin);
            log.info("Default admin account '{}' created", username);
        });
    }
}
