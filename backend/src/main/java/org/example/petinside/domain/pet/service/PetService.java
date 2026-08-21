package org.example.petinside.domain.pet.service;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.pet.dto.PetPhotoLikeResponse;
import org.example.petinside.domain.pet.dto.PetPhotoRequest;
import org.example.petinside.domain.pet.dto.PetPhotoResponse;
import org.example.petinside.domain.pet.dto.PetRequest;
import org.example.petinside.domain.pet.dto.PetResponse;
import org.example.petinside.domain.pet.entity.Pet;
import org.example.petinside.domain.pet.entity.PetPhoto;
import org.example.petinside.domain.pet.entity.PetPhotoLike;
import org.example.petinside.domain.pet.repository.PetPhotoLikeRepository;
import org.example.petinside.domain.pet.repository.PetPhotoRepository;
import org.example.petinside.domain.pet.repository.PetRepository;
import org.example.petinside.domain.subscription.entity.SubscriptionStatus;
import org.example.petinside.domain.subscription.repository.SubscriptionRepository;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.user.repository.UserRepository;
import org.example.petinside.global.exception.CustomException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final PetPhotoRepository petPhotoRepository;
    private final PetPhotoLikeRepository petPhotoLikeRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    // 구독자 검증 - PetService 안에서만 쓰이므로 private
    // User 객체를 반환해서 이후 로직에서 재사용 (DB 조회 1번으로 끝냄)
    private User validateSubscriberAndGetUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(404, "유저 없음"));
        if (!subscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.ACTIVE)) {
            throw new CustomException(403, "구독자만 펫 프로필을 이용할 수 있습니다.");
        }
        return user;
    }

    // [F-35] 펫 등록
    @Transactional
    public PetResponse create(Long userId, PetRequest request) {
        User user = validateSubscriberAndGetUser(userId);
        Pet pet = Pet.create(
                user,
                request.getPetName(),
                request.getPetType(),
                request.getPetBirthday(),
                request.getPetIntro(),
                request.getPetImageUrl()
        );
        return PetResponse.from(petRepository.save(pet));
    }

    // [F-35] 내 펫 목록 조회 - 구독이 만료돼도 기존에 등록한 펫은 볼 수 있게 구독 검증 없이 조회
    // 단, 프론트에서 구독 상태에 따라 잠금 UI를 보여주기 때문에 백엔드는 조회만 허용
    public List<PetResponse> getMyPets(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(404, "유저 없음");
        }
        return petRepository.findByUserId(userId).stream()
                .map(PetResponse::from)
                .toList();
    }

    // [F-35] 펫 삭제 - 사진은 cascade로 자동 삭제
    @Transactional
    public void delete(Long userId, Long petId) {
        validateSubscriberAndGetUser(userId);
        Pet pet = petRepository.findByIdAndUserId(petId, userId)
                .orElseThrow(() -> new CustomException(404, "펫을 찾을 수 없습니다."));
        petRepository.delete(pet);
    }

    // [F-35] 펫 수정 - 본인 소유 펫인지 한 번에 확인 (findByIdAndUserId)
    @Transactional
    public PetResponse update(Long userId, Long petId, PetRequest request) {
        validateSubscriberAndGetUser(userId); // 구독자만 수정 가능
        Pet pet = petRepository.findByIdAndUserId(petId, userId)
                .orElseThrow(() -> new CustomException(404, "펫을 찾을 수 없습니다."));
        pet.update(
                request.getPetName(),
                request.getPetType(),
                request.getPetBirthday(),
                request.getPetIntro(),
                request.getPetImageUrl()
        );
        return PetResponse.from(pet);
    }

    // 펫 사진 조회 - 구독 여부 무관
    public List<PetPhotoResponse> getPhotos(Long petId, Long userId) {
        if (!petRepository.existsById(petId)) {
            throw new CustomException(404, "펫을 찾을 수 없습니다.");
        }
        return petPhotoRepository.findByPetIdOrderByCreatedAtDesc(petId).stream()
                .map(p -> PetPhotoResponse.from(p,
                        petPhotoLikeRepository.countByPhotoId(p.getId()),
                        petPhotoLikeRepository.existsByPhotoIdAndUserId(p.getId(), userId)))
                .toList();
    }

    // 펫 사진 좋아요 토글
    @Transactional
    public PetPhotoLikeResponse toggleLike(Long userId, Long photoId) {
        if (!petPhotoRepository.existsById(photoId)) {
            throw new CustomException(404, "사진을 찾을 수 없습니다.");
        }
        petPhotoLikeRepository.findByPhotoIdAndUserId(photoId, userId)
                .ifPresentOrElse(
                        petPhotoLikeRepository::delete,
                        () -> petPhotoLikeRepository.save(PetPhotoLike.of(photoId, userId))
                );
        long count = petPhotoLikeRepository.countByPhotoId(photoId);
        boolean liked = petPhotoLikeRepository.existsByPhotoIdAndUserId(photoId, userId);
        return new PetPhotoLikeResponse(liked, count);
    }

    // 펫 사진 추가 - 구독자 + 본인 소유 펫
    @Transactional
    public PetPhotoResponse addPhoto(Long userId, Long petId, PetPhotoRequest request) {
        validateSubscriberAndGetUser(userId);
        Pet pet = petRepository.findByIdAndUserId(petId, userId)
                .orElseThrow(() -> new CustomException(404, "펫을 찾을 수 없습니다."));
        return PetPhotoResponse.from(petPhotoRepository.save(PetPhoto.of(pet, request.getImageUrl(), request.getCaption())), 0, false);
    }

    // 펫 사진 삭제 - 구독자 + 본인 소유 펫의 사진
    @Transactional
    public void deletePhoto(Long userId, Long petId, Long photoId) {
        validateSubscriberAndGetUser(userId);
        PetPhoto photo = petPhotoRepository.findById(photoId)
                .orElseThrow(() -> new CustomException(404, "사진을 찾을 수 없습니다."));
        Long ownerUserId = photo.getPet().getUser().getId();
        Long ownerPetId = photo.getPet().getId();
        if (!ownerPetId.equals(petId) || !ownerUserId.equals(userId)) {
            throw new CustomException(403, "권한이 없습니다.");
        }
        petPhotoRepository.delete(photo);
    }
}
