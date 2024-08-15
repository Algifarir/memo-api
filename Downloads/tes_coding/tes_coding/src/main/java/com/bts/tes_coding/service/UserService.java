package com.bts.tes_coding.service;

import java.text.MessageFormat;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import com.bts.tes_coding.dto.request.LoginRequest;
import com.bts.tes_coding.dto.request.RegisterRequest;
import com.bts.tes_coding.dto.response.LoginResponse;
import com.bts.tes_coding.dto.response.MessageResponse;
import com.bts.tes_coding.model.UserDetail;
import com.bts.tes_coding.model.User;
import com.bts.tes_coding.repository.UserRepository;
import com.bts.tes_coding.util.jwt.JwtUtil;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserService {
    @Autowired
    private Validator validator;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    public ResponseEntity<MessageResponse> registerUser(RegisterRequest request) {
        log.info("Registering user with username: {}", request.getUsername());
        try {
            Set<ConstraintViolation<RegisterRequest>> constraintViolations = validator.validate(request);
            if (!constraintViolations.isEmpty()) {
                ConstraintViolation<RegisterRequest> firstViolation = constraintViolations.iterator().next();
                String message = firstViolation.getMessage();
                return ResponseEntity.badRequest()
                        .body(new MessageResponse(message, HttpStatus.BAD_REQUEST.value(),
                                HttpStatus.BAD_REQUEST.getReasonPhrase()));
            }
            if (userRepository.existsByUsername(request.getUsername())) {
                String message = "Username already exists.";
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new MessageResponse(message, HttpStatus.CONFLICT.value(),
                                HttpStatus.CONFLICT.getReasonPhrase()));
            }

            User user = User.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .password(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()))
                    .build();
            userRepository.save(user);

            String message = "User registration successful.";
            String formatMessage = MessageFormat.format(message, request.getUsername());
            return ResponseEntity.ok(
                    MessageResponse.builder()
                            .message(formatMessage)
                            .statusCode(HttpStatus.OK.value())
                            .status(HttpStatus.OK.getReasonPhrase())
                            .build());
        } catch (Exception e) {
            log.error("Error registering user", e);
            String message = "An internal error occurred. Please try again later.";
            return ResponseEntity.internalServerError()
                    .body(new MessageResponse(message, HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()));
        }
    }

    public ResponseEntity<LoginResponse> loginUser(@RequestBody LoginRequest request) {
        log.info("Signing in user with username: {}", request.getUsername());
        try {
            Set<ConstraintViolation<LoginRequest>> constraintViolations = validator.validate(request);
            if (!constraintViolations.isEmpty()) {
                ConstraintViolation<LoginRequest> firstViolation = constraintViolations.iterator().next();
                String message = firstViolation.getMessage();
                return ResponseEntity.badRequest()
                        .body(new LoginResponse(message, HttpStatus.BAD_REQUEST.value(),
                                HttpStatus.BAD_REQUEST.getReasonPhrase(), null));
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = jwtUtil.generateJwtToken(authentication);

            UserDetail userDetail = (UserDetail) authentication.getPrincipal();

            String message = "Login successful.";

            HttpHeaders headers = new HttpHeaders();
            headers.add("Token", jwt);

            return ResponseEntity.ok().headers(headers)
                    .body(LoginResponse.builder()
                            .data(new LoginResponse.UserData(userDetail.getUserId(), jwt, "Bearer",
                                    userDetail.getUsername()))
                            .message(message)
                            .statusCode(HttpStatus.OK.value())
                            .status(HttpStatus.OK.getReasonPhrase())
                            .build());
        } catch (AuthenticationException e) {
            log.error("Invalid credentials", e);
            String message = "Invalid username or password.";
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(message, HttpStatus.UNAUTHORIZED.value(),
                            HttpStatus.UNAUTHORIZED.getReasonPhrase(), null));
        } catch (Exception e) {
            log.error("Error signing in user", e);
            String message = "An internal error occurred. Please try again later.";
            return ResponseEntity.internalServerError()
                    .body(new LoginResponse(message, HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), null));
        }
    }
}
