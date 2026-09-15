package com.asohCloak.asohCloak.mapper.userMappper;

import com.asohCloak.asohCloak.dto.user.LoginResponseDto;
import com.asohCloak.asohCloak.dto.user.RegisterRequestDto;
import com.asohCloak.asohCloak.entity.user.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {
    
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    User toEntity(RegisterRequestDto dto);

    default LoginResponseDto toLoginResponseDto(User user, String accessToken, String refreshToken) {
        return new LoginResponseDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.isAccountVerified(),
                accessToken,
                refreshToken
        );
    }
}