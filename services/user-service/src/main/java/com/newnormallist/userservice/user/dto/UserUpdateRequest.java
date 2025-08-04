package com.newnormallist.userservice.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@NoArgsConstructor
public class UserUpdateRequest {
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    private String password;

    @NotNull(message = "뉴스레터 수신 동의 여부는 필수입니다.")
    private Boolean letterOk;

    private Set<String> hobbies;
}
