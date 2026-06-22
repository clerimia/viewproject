package com.aicust.controller;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.aicust.model.User;
import com.aicust.repository.UserRepository;
import com.aicust.security.JwtUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/captcha")
    public void captcha(HttpServletResponse response) throws IOException {
        int width = 200;
        int height = 100;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        Random random = new Random();
        g.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < 20; i++) {
            int x1 = random.nextInt(width);
            int y1 = random.nextInt(height);
            int x2 = random.nextInt(width);
            int y2 = random.nextInt(height);
            g.drawLine(x1, y1, x2, y2);
        }

        String str = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder codeBuilder = new StringBuilder();
        g.setFont(new Font("Arial", Font.BOLD, 40));

        for (int i = 0; i < 4; i++) {
            char ch = str.charAt(random.nextInt(str.length()));
            codeBuilder.append(ch);
            g.setColor(new Color(random.nextInt(100), random.nextInt(100), random.nextInt(100)));
            g.drawString(String.valueOf(ch), 40 * i + 20, 60 + random.nextInt(10));
        }

        String code = codeBuilder.toString();
        g.dispose();

        String uuid = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("captcha:" + uuid, code, 5, TimeUnit.MINUTES);

        response.setHeader("X-Captcha-UUID", uuid);
        response.setContentType("image/png");
        ImageIO.write(image, "png", response.getOutputStream());
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String code = body.get("code");
        String uuid = body.get("uuid");
        String loginRole = body.getOrDefault("loginRole", "USER");

        if (uuid == null || uuid.isBlank()) {
            return Map.of("success", false, "message", "缺少验证码UUID，请刷新验证码");
        }

        String cachedCode = redisTemplate.opsForValue().get("captcha:" + uuid);
        if (cachedCode == null) {
            return Map.of("success", false, "message", "验证码已过期，请刷新验证码");
        }
        if (!cachedCode.equalsIgnoreCase(code)) {
            return Map.of("success", false, "message", "验证码错误");
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return Map.of("success", false, "message", "用户不存在，请先注册");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Map.of("success", false, "message", "密码错误");
        }

        String userRole = user.getRole() == null ? "USER" : user.getRole();
        if ("ADMIN".equalsIgnoreCase(loginRole) && !"ADMIN".equalsIgnoreCase(userRole)) {
            return Map.of("success", false, "message", "该账号不是管理员，不能进入管理端");
        }
        if ("USER".equalsIgnoreCase(loginRole) && "ADMIN".equalsIgnoreCase(userRole)) {
            return Map.of("success", false, "message", "管理员账号请从管理端登录，不能进入游客端");
        }

        // 验证成功后删除已使用的验证码
        redisTemplate.delete("captcha:" + uuid);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), userRole);
        return Map.of("success", true, "token", token, "userId", user.getId(), "role", userRole);
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            return Map.of("success", false, "message", "用户名不能为空");
        }
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return Map.of("success", false, "message", "用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // 公开注册入口只允许创建游客账号，管理员账号必须由系统初始化或数据库侧创建。
        user.setRole("USER");
        userRepository.save(user);
        return Map.of("success", true, "message", "注册成功");
    }
}
