package com.example.backendeco.Service;

import com.example.backendeco.Entity.User;
import com.example.backendeco.Repository.UserRepository;
import com.example.backendeco.enums.UserRole;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Inscription d'un utilisateur (mot de passe en clair stocké)
     */
    public User registerUser(String username,
                             String email,
                             String password,
                             String adresse,
                             UserRole role) {
        // Vérifier unicité de l'email
        if (userRepository.findByEmail(email) != null) {
            throw new IllegalArgumentException("L'utilisateur avec cet email existe déjà");
        }

        // Interdire l'inscription d'un admin
        if (role == UserRole.ADMIN) {
            throw new IllegalArgumentException("L'admin ne peut pas s'inscrire, il doit se connecter.");
        }

        User user = new User(username, email, password, adresse, role);
        return userRepository.save(user);
    }

    /**
     * Connexion d'un utilisateur (mot de passe en clair comparé)
     */
    public User loginUser(String email, String password) {
        User user = userRepository.findByEmail(email);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    /**
     * Recherche par email (pour /me)
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Vérification du rôle admin
     */
    public boolean isAdmin(User user) {
        return user != null && UserRole.ADMIN.equals(user.getRole());
    }

    /**
     * Récupération de tous les utilisateurs
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Suppression d'un utilisateur
     */
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    /**
     * Mise à jour d'un utilisateur (hors mot de passe)
     */
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
        user.setUsername(userDetails.getUsername());
        user.setEmail(userDetails.getEmail());
        user.setAdresse(userDetails.getAdresse());
        // Ne modifie pas le mot de passe
        return userRepository.save(user);
    }
}
