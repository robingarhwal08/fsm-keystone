package com.fsm.keystone.service;

import com.fsm.keystone.entity.AppUser;
import com.fsm.keystone.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public AppUser requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUser user)) {
            throw new BusinessException("Authenticated user required");
        }
        return user;
    }
}
