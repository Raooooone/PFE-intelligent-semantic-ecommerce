package com.example.backendeco.Controller;

import com.example.backendeco.Entity.User;
import com.example.backendeco.Service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(
        origins = "http://localhost:4200",
        allowCredentials = "true",      // très important pour que le cookie de session soit envoyé
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS}
)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Inscription – on crée l’utilisateur, et on le stocke en session */
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody User user,
            HttpServletRequest request
    ) {
        try {
            User newUser = userService.registerUser(
                    user.getUsername(),
                    user.getEmail(),
                    user.getPassword(),
                    user.getAdresse(),
                    user.getRole()
            );
            // création de la session
            HttpSession session = request.getSession(true);
            session.setAttribute("userEmail", newUser.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Connexion – on autentifie manuellement, puis on mémorise en session */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody User user,
            HttpServletRequest request
    ) {
        User authUser = userService.loginUser(user.getEmail(), user.getPassword());
        if (authUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Email ou mot de passe incorrect");
        }
        HttpSession session = request.getSession(true);
        session.setAttribute("userEmail", authUser.getEmail());
        return ResponseEntity.ok(authUser);
    }

    /** Récupère le profil de l’utilisateur connecté, via la session */
    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userEmail") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Pas de session active");
        }
        String email = (String) session.getAttribute("userEmail");
        User user = userService.findByEmail(email);
        return ResponseEntity.ok(user);
    }

    /** Déconnexion – on invalide la session */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok("Déconnecté");
    }

    // --- Vos autres endpoints (getAll, delete, update) restent inchangés ---
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok("Utilisateur supprimé");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Utilisateur non trouvé");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestBody User userDetails
    ) {
        try {
            User updated = userService.updateUser(id, userDetails);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Utilisateur non trouvé");
        }
    }
}
