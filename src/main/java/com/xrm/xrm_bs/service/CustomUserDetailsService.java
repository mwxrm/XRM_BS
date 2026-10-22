package com.xrm.xrm_bs.service;

import com.xrm.xrm_bs.entity.User;
import com.xrm.xrm_bs.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserService userService; // 使用字段注入

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user;

        // 先尝试按邮箱查找
        try {
            user = userService.findByEmail(usernameOrEmail);
        } catch (UsernameNotFoundException e1) {
            // 如果邮箱找不到，尝试按用户名查找
            try {
                user = userService.findByUsername(usernameOrEmail);
            } catch (UsernameNotFoundException e2) {
                throw new UsernameNotFoundException("用户未找到: " + usernameOrEmail);
            }
        }
        return new CustomUserDetails(user);
    }
}
