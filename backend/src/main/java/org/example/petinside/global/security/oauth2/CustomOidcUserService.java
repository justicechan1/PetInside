package org.example.petinside.global.security.oauth2;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.user.entity.SocialAccount;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.user.repository.SocialAccountRepository;
import org.example.petinside.domain.user.repository.UserRepository;
import org.example.petinside.domain.user.service.NicknameGenerator;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private static final String PROVIDER_GOOGLE = "GOOGLE";

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final NicknameGenerator nicknameGenerator;

    // OAuth2는 별도 회원가입 API 없이, 최초 로그인 시 User+SocialAccount를 함께 생성해서 가입
    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);
        String providerId = oidcUser.getSubject();

        User user = socialAccountRepository.findByProviderAndProviderId(PROVIDER_GOOGLE, providerId)
                .map(SocialAccount::getUser)
                .orElseGet(() -> registerNewSocialUser(providerId));

        return new CustomOidcUser(oidcUser, user.getId());
    }

    private User registerNewSocialUser(String providerId) {
        String username = PROVIDER_GOOGLE.toLowerCase() + "_" + providerId;
        String nickname = nicknameGenerator.generate();

        User user = User.createSocialUser(username, nickname);
        userRepository.save(user);

        socialAccountRepository.save(SocialAccount.builder()
                .user(user)
                .provider(PROVIDER_GOOGLE)
                .providerId(providerId)
                .build());

        return user;
    }
}
