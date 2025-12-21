package com.finpro7.server.controller;

import com.finpro7.server.model.User;
import com.finpro7.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // REGISTER
    @PostMapping("/register")
    public String register(@RequestBody User user) {
        // Cek username ada gak
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return "Username sudah dipakai!";
        }
        // Encrypt password sebelum simpan
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");
        userRepository.save(user);
        return "Register Berhasil!";
    }

    // LOGIN MANUAL (Sederhana)
    @PostMapping("/login")
    public String login(@RequestBody User user) {
        Optional<User> dbUser = userRepository.findByUsername(user.getUsername());

        if (dbUser.isPresent()) {
            // Cek password raw vs password hash di DB
            if (passwordEncoder.matches(user.getPassword(), dbUser.get().getPassword())) {
                return "Login Sukses! Role kamu: " + dbUser.get().getRole();
            }
        }
        return "Username atau Password salah!";
    }
}
