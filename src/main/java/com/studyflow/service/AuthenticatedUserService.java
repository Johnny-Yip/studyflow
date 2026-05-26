package com.studyflow.service;

import com.studyflow.entity.User;
import com.studyflow.exception.ResourceNotFoundException;
import com.studyflow.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticatedUserService {

    private final UserRepository userRepository;

    public AuthenticatedUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User getRequiredUser(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User not found for email " + email));
    }

    @Transactional(readOnly = true)
    public Long getRequiredUserId(String email) {
        return getRequiredUser(email).getId();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
