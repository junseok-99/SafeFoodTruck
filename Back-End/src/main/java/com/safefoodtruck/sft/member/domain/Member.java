package com.safefoodtruck.sft.member.domain;

import static jakarta.persistence.CascadeType.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import com.safefoodtruck.sft.member.dto.request.MemberSignUpRequestDto;
import com.safefoodtruck.sft.member.dto.request.MemberUpdateRequestDto;
import com.safefoodtruck.sft.notification.domain.Notification;
import com.safefoodtruck.sft.order.domain.Order;
import com.safefoodtruck.sft.review.domain.Review;
import com.safefoodtruck.sft.store.domain.Store;
import com.safefoodtruck.sft.survey.domain.Survey;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "member")
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Member {

    @Id
    @NotNull
    @Column(name = "email")
    private String email;

    @NotNull
    @Column(name = "password")
    private String password;

    @NotNull
    @Column(name = "name", length = 50)
    private String name;

    @NotNull
    @Column(name = "nickname", length = 50)
    private String nickname;

    @NotNull
    @Column(name = "gender")
    private Integer gender;

    @NotNull
    @Column(name = "birth", columnDefinition = "DATE")
    private LocalDate birth;

    @NotNull
    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "business_number", length = 100)
    @ColumnDefault("'not owner'")
    private String businessNumber;

    @Column(name = "role", length = 20)
    @ColumnDefault("'customer'")
    private String role;

    @Column(name = "vip_expired_date", columnDefinition = "TIMESTAMP")
    private LocalDateTime vipExpiredDate;

    @Column(name = "reg_date", columnDefinition = "TIMESTAMP")
    private LocalDateTime regDate;

    @Column(name = "is_resign")
    @ColumnDefault("0")
    private Integer isResign;

    @OneToMany(mappedBy = "member")
    private List<Notification> notificationList = new ArrayList<>();

    @OneToMany(mappedBy = "member")
    private List<Survey> surveyList = new ArrayList<>();

    @Setter
    @OneToOne(mappedBy = "owner", cascade = ALL, orphanRemoval = true)
    private Store store;

    @OneToMany(mappedBy = "customer")
    private List<Order> orderList = new ArrayList<>();

    @OneToMany(mappedBy = "customer")
    private List<Review> reviewList = new ArrayList<>();

    public void updateMember(MemberUpdateRequestDto memberUpdateRequestDto) {
        this.nickname = memberUpdateRequestDto.getNickname();
        this.phoneNumber = memberUpdateRequestDto.getPhoneNumber();
        this.password = memberUpdateRequestDto.getPassword();
    }

    public void resign() {
        this.isResign = 1;
    }

    public void joinVip(String vipName) {
        this.role = vipName;
        this.vipExpiredDate = LocalDateTime.now().plusDays(30);
    }

    public void deactivateVip(String role) {
        this.role = role;
        this.vipExpiredDate = null;
    }

    public void extendVip() {
        this.vipExpiredDate = this.vipExpiredDate.plusDays(30);
    }

    public void updatePassword(String randomPassword) {
        this.password = randomPassword;
    }

    @Builder(builderMethodName = "signupBuilder")
    public Member(MemberSignUpRequestDto memberSignUpRequestDto) {
        this.email = memberSignUpRequestDto.getEmail();
        this.password = memberSignUpRequestDto.getPassword();
        this.name = memberSignUpRequestDto.getName();
        this.nickname = memberSignUpRequestDto.getNickname();
        this.gender = memberSignUpRequestDto.getGender();
        this.birth = memberSignUpRequestDto.getBirth();
        this.phoneNumber = memberSignUpRequestDto.getPhoneNumber();
        this.businessNumber = memberSignUpRequestDto.getBusinessNumber();
        this.role = memberSignUpRequestDto.getBusinessNumber() == null ? "customer" : "owner";
        this.regDate = LocalDateTime.now();
        this.isResign = 0;
    }
}
