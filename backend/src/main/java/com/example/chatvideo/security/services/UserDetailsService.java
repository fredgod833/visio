package com.example.chatvideo.security.services;

import com.example.chatvideo.model.UserEntity;
import com.example.chatvideo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;

@Service
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

    @Autowired
    private UserRepository usersRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity entity = usersRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(String.format("utilisateur %s inconnu.", email)));
        return user2UserDetails(entity);
    }

    private UserDetails user2UserDetails(UserEntity userEntity) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        return new UserDetailsImpl(
                userEntity.getId(),
                userEntity.getFirstName(),
                userEntity.getLastName(),
                userEntity.getEmail(),
                userEntity.getEmail(),
                userEntity.getPassword(),
                authorities);
    }

}
