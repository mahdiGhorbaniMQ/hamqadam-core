package ir.hamqadam.core.security;

import ir.hamqadam.core.model.User; // Your User entity
import ir.hamqadam.core.repository.UserRepository; // Your UserRepository
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
// Import other necessary collections if User model has roles/permissions directly
// For Phase 1, roles might be simple strings or an enum.

@Service
@Primary
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // Assuming users can log in with either username or email.
        // Adjust the query if your User entity has a separate username field.
        User user = userRepository.findByEmail(usernameOrEmail) // Or findByUsernameOrEmail if you have that
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail)
                );

        // For Phase 1, authorities can be simple role strings.
        // This needs to be expanded if you have a more complex Role/Permission entity structure.
        Set<GrantedAuthority> authorities = new HashSet<>();
        // Example: if user.getRoles() returns a Set<String> of role names
        // user.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));

        // For now, let's assume a default authority or fetch from a simple field if present
        // This part needs to be aligned with your actual User model's role/permission storage for Phase 1.
        // Example: if you have a 'role' field in User:
        // authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()));
        // For a very basic setup, you might give a default role to all authenticated users:
        authorities.add(new SimpleGrantedAuthority("ROLE_USER")); // Placeholder - customize this

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), // Or user.getUsername() if you have one
                user.getPasswordHash(),
                user.getAccountStatus().toString().equalsIgnoreCase("active"), // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked - you might want a field for this in User model
                authorities
        );
    }
}