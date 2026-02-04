package com.example.chatvideo.controller;

import com.example.chatvideo.exceptions.InvalidRegistrationException;
import com.example.chatvideo.model.UserEntity;
import com.example.chatvideo.payload.request.LoginRequest;
import com.example.chatvideo.payload.request.SignupRequest;
import com.example.chatvideo.payload.request.UpdateUserRequest;
import com.example.chatvideo.payload.response.JwtResponse;
import com.example.chatvideo.payload.response.MessageResponse;
import com.example.chatvideo.repository.UserRepository;
import com.example.chatvideo.security.jwt.JwtService;
import com.example.chatvideo.security.services.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-:]).{7,}");

    AuthController(AuthenticationManager authenticationManager,
                   PasswordEncoder passwordEncoder,
                   UserRepository userRepository, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PostMapping(value = "/login", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Log in Client with credentials.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Log in successfull, bearer token returned.",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))
            ),
            @ApiResponse(responseCode = "400",
                    description = "Invalid form, bad request, user already exists. The reason is returned in message.",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            )})
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        return authenticate(loginRequest.getLogin(), loginRequest.getPassword());
    }

    private ResponseEntity<JwtResponse> authenticate(String loginRequest, String pwd) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest,
                        pwd));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        String jwt = jwtService.generateAccessToken(userDetails);
        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail()));
    }

    @PostMapping(value = "/register", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<JwtResponse> registerUser(@Valid @RequestBody SignupRequest signUpRequest) throws InvalidRegistrationException {

        if (userRepository.findByEmail(signUpRequest.getEmail()).isPresent()) {
            throw new InvalidRegistrationException("Un compte existe avec cet email ! Veuillez vous logger.");
        }

        if (signUpRequest.getPassword().length() < 7) {
            throw new InvalidRegistrationException("Le mot de passe doit avoir 7 caractères minimum.");
        }

        Matcher m = PASSWORD_PATTERN.matcher(signUpRequest.getPassword());
        if (!m.find()) {
            throw new InvalidRegistrationException("Le mot de passe doit contenir une majuscule, une minuscule, un caractère spécial.");
        }

        // Create new user's account
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(signUpRequest.getEmail());
        userEntity.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        userEntity.setFirstName(signUpRequest.getFirstName());
        userEntity.setLastName(signUpRequest.getLastName());

        userRepository.save(userEntity);

        return authenticate(signUpRequest.getEmail(), signUpRequest.getPassword());
    }

    @GetMapping(value = "/me", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the connected user informations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Return the connected user informations.",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))
            ),
            @ApiResponse(responseCode = "401",
                    description = "Invalid connection, token expired, etc..",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            )})
    @SecurityRequirement(name = "Bearer JWT Authentication")
    public ResponseEntity<JwtResponse> getConnectedUser(Authentication auth) {
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        return ResponseEntity.ok(new JwtResponse((String) auth.getCredentials(),
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail()));
    }

    @PutMapping(value = "/me", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<JwtResponse> updateUser(Authentication auth, @RequestBody UpdateUserRequest updateUserRequest) throws InvalidRegistrationException {

        //Recherche de l'utilisateur connecté à modifier
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();

        try {
            //Verification que le mot de passe du formulaire est bien celui du user connecté
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            userDetails.getUsername(),
                            updateUserRequest.getPassword()));
        } catch (BadCredentialsException e) {
            throw new InvalidRegistrationException("Mot de passe erroné.", e);
        }

        // verif d'unicité du nouvel email si modifié
        if (!userDetails.getEmail().equalsIgnoreCase(updateUserRequest.getEmail())) {
            // l'email a été modifié, on recherche si il existe un doublon en base
            if (userRepository.findByEmail(updateUserRequest.getEmail()).isPresent()) {
                throw new InvalidRegistrationException("Un autre compte existe déjà avec cet email !");
            }
        }

        long userId = userDetails.getId();
        UserEntity user = this.userRepository.findById(userId).orElseThrow(() -> new InvalidRegistrationException("utilisateur non trouvé"));
        user.setEmail(updateUserRequest.getEmail());
        user.setFirstName(updateUserRequest.getFirstName());
        user.setLastName(updateUserRequest.getLastName());

        //TODO: traiter nouveau mot de passe.
        String password = updateUserRequest.getPassword();
        if (!updateUserRequest.getNewPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(updateUserRequest.getNewPassword()));
            password = updateUserRequest.getNewPassword();
        }
        userRepository.save(user);
        return authenticate(updateUserRequest.getEmail(), password);
    }

}