package org.example.petinside.mypage.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserInfoResponse {
    private  Long id;
    private String username;
    private String nickname;
    private String profileImageUrl;
    private String role;
    private LocalDateTime createdAt;
    private String provider;
}
