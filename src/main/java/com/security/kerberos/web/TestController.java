package com.security.kerberos.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.kerberos.authentication.KerberosServiceRequestToken;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TestController {

    @GetMapping(value={"/"})
    public ResponseEntity<Map<String, Object>> test(){
        Map<String, Object> result = new HashMap<>();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        KerberosServiceRequestToken kauth = (KerberosServiceRequestToken)SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) auth.getPrincipal();

        WebAuthenticationDetails details = (WebAuthenticationDetails) auth.getDetails();

        if(null != auth.getName()) result.put("username", auth.getName());
        if(null != auth.getPrincipal()) {
            result.put("principal-username", principal.getUsername());
            result.put("principal-Password", principal.getPassword());
            result.put("principal-AccountNonExpired", principal.isAccountNonExpired());
            result.put("principal-AccountNonLocked", principal.isAccountNonLocked());
            result.put("principal-credentialsNonExpired", principal.isCredentialsNonExpired());
            result.put("principal-Enabled", principal.isEnabled());
        }

        if(null != auth.getDetails()) {
            result.put("details-RemoteIpAddress", details.getRemoteAddress());
            result.put("details-SessionId", details.getSessionId());
        }

        if(null != auth.getAuthorities()) result.put("authorities", auth.getAuthorities().toString());
        if(null != auth.getCredentials()) result.put("credentials", auth.getCredentials().toString());

        result.put("isAuthenticated", auth.isAuthenticated());
        result.put("summary", auth.toString());
        String token = Base64.getEncoder().encodeToString(kauth.getToken());
        result.put("getToken : ", token );

        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/login")
    public ModelAndView login(){
        ModelAndView mv = new ModelAndView();
        mv.setViewName("login");
        return mv;
    }

}

