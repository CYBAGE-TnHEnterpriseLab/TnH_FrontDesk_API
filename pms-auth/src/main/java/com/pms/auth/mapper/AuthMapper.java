package com.pms.auth.mapper;

import com.pms.auth.dto.RegisterRequest;
import com.pms.auth.entity.Role;
import com.pms.auth.entity.UserAccount;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public UserAccount toEntity(RegisterRequest request, String encodedPassword, Set<Role> roles) {
        UserAccount user = new UserAccount();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPasswordHash(encodedPassword);
        user.setEnabled(true);
        user.setRoles(roles);
        return user;
    }
}

