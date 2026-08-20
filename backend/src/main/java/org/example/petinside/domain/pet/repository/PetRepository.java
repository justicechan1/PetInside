package org.example.petinside.domain.pet.repository;

import org.example.petinside.domain.pet.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Long> {

    List<Pet> findByUserId(Long userId);

    // 수정/삭제 시 본인 소유 펫인지 한 번에 확인하기 위해 userId도 같이 조건에 넣음
    Optional<Pet> findByIdAndUserId(Long id, Long userId);
}
