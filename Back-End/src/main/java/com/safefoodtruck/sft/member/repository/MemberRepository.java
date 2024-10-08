package com.safefoodtruck.sft.member.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.safefoodtruck.sft.member.domain.Member;

public interface MemberRepository extends JpaRepository<Member, String>, MemberRepositoryCustom {
    @EntityGraph(attributePaths = {"store", "store.storeImage"})
    Optional<Member> findByEmail(String email);
    @EntityGraph(attributePaths = {"store", "store.storeImage"})
    Optional<Member> findByEmailAndNameAndBirthAndPhoneNumber(String email, String name, LocalDate birth, String phoneNumber);
}
