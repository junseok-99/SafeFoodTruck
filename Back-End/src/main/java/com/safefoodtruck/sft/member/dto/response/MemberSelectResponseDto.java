package com.safefoodtruck.sft.member.dto.response;

import com.safefoodtruck.sft.member.dto.MemberImageDto;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MemberSelectResponseDto {
    private String email;
    private String name;
    private String nickname;
    private int gender;
    private LocalDate birth;
    private String phoneNumber;
    private String businessNumber;
    private String role;
    private LocalDateTime vipExpiredDate;
    private LocalDateTime regDate;
    private int isResign;
    private MemberImageDto memberImage;
}
