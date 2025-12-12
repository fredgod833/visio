package com.example.chatvideo.mapper;

import com.example.chatvideo.dto.UserDTO;
import com.example.chatvideo.model.UserEntity;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public interface UserMapper extends EntityMapper<UserDTO, UserEntity> {

}
