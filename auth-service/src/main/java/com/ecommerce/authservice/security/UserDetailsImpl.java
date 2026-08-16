package com.ecommerce.authservice.security;

import com.ecommerce.authservice.models.AuthUser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * INTERN GUIDE: SPRING SECURITY ADAPTER
 * -------------------------------------
 * Spring Security's `AuthenticationManager` doesn't know what our `AuthUser` database entity is. 
 * It only understands objects that implement the `UserDetails` interface. 
 * 
 * This class acts as an adapter, translating our `AuthUser` into a standard format 
 * that Spring Security can understand.
 */
@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {

    @Getter
    private Long id;
    private String username;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    private boolean isActive;

    /**
     * Builds a UserDetailsImpl object from our AuthUser entity and a pre-computed list of authorities.
     */
    public static UserDetailsImpl build(AuthUser user, List<GrantedAuthority> authorities) {
        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                authorities,
                user.isActive()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
}
