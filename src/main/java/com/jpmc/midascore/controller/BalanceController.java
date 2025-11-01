package com.jpmc.midascore.controller;

import com.jpmc.midascore.foundation.Balance;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
public class BalanceController {

    private final UserRepository userRepository;

    public BalanceController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/balance")
    public Balance getUserBalance(@RequestParam("userId") Long userId) {
        Optional<UserRecord> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return new Balance(0);
        }
        UserRecord user = userOptional.get();
        return new Balance(user.getBalance());
    }
}
