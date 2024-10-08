package com.safefoodtruck.sft.favorites.controller;

import com.safefoodtruck.sft.common.dto.ErrorResponseDto;
import com.safefoodtruck.sft.common.util.MemberInfo;
import com.safefoodtruck.sft.favorites.dto.response.CheckIsFavoriteResponseDto;
import com.safefoodtruck.sft.favorites.dto.response.SelectFavoriteResponseDto;
import com.safefoodtruck.sft.favorites.dto.response.SelectMemberFavoriteResponseDto;
import com.safefoodtruck.sft.favorites.exception.ImpossibleRetryException;
import com.safefoodtruck.sft.favorites.exception.NotInsertedFavoriteException;
import com.safefoodtruck.sft.favorites.service.FavoritesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
@Slf4j
public class FavoritesController {

    private final FavoritesService favoritesService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_customer', 'ROLE_vip_customer')")
    @Operation(summary = "내가 찜한 목록 조회", description = "(손님 전용!!) 내가 찜한 목록을 조회할 때 사용하는 API")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
            description = "내가 찜한 목록 조회 성공",
            content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> selectFavorites() {
        String userEmail = MemberInfo.getEmail();
        SelectMemberFavoriteResponseDto selectMemberFavoriteResponseDto = favoritesService.selectMemberFavorite(
            userEmail);
        return ResponseEntity.status(HttpStatus.OK).body(selectMemberFavoriteResponseDto);
    }

    @GetMapping("/{storeId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "가게 찜 개수 조회", description = "가게 찜 개수를 조회할 때 사용하는 API")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
            description = "가게 찜 개수 조회 성공",
            content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> selectFavoriteCount(@PathVariable("storeId") Integer storeId) {
        SelectFavoriteResponseDto selectFavoriteResponseDto = favoritesService.selectFavoriteCount(storeId);
        return ResponseEntity.status(HttpStatus.OK).body(selectFavoriteResponseDto);
    }

    @PostMapping("/{storeId}")
    @PreAuthorize("hasAnyRole('ROLE_customer', 'ROLE_vip_customer')")
    @Operation(summary = "가게 찜하기", description = "(손님 전용!!) 가게를 찜할 때 사용하는 API")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
            description = "가게 찜하기 성공",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500",
            description = "Error Message로 전달함",
            content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> insertFavorite(@PathVariable("storeId") Integer storeId) {
        String userEmail = MemberInfo.getEmail();
        favoritesService.insertMemberFavorite(userEmail, storeId);
        return ResponseEntity.status(HttpStatus.OK).body("가게 찜하기 성공");
    }

    @DeleteMapping("/{favoriteId}")
    @PreAuthorize("hasAnyRole('ROLE_customer', 'ROLE_vip_customer')")
    @Operation(summary = "가게 찜삭제", description = "(손님 전용!!) 내가 찜한 가게를 삭제할 때 사용하는 API")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
            description = "가게 찜삭제 성공",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500",
            description = "Error Message로 전달함",
            content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> deleteFavorite(@PathVariable("favoriteId") Integer favoriteId) {
        String userEmail = MemberInfo.getEmail();
        favoritesService.deleteMemberFavorite(userEmail, favoriteId);
        return ResponseEntity.status(HttpStatus.OK).body("가게 찜삭제 성공");
    }

    @ExceptionHandler({ImpossibleRetryException.class, NotInsertedFavoriteException.class,
        NotWritablePropertyException.class})
    public ResponseEntity<?> ImpossibleExceptionHandler(RuntimeException e) {
        log.error("Favorites 에러: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(new ErrorResponseDto(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    e.getMessage(),
                    LocalDateTime.now()
                )
            );
    }
    @GetMapping("/my/{storeId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "가게 찜 확인",
        description = "가게 상세 조회 할 때 사용자가 해당 가게를 찜 했는지 확인 할 때 사용하는 API")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
            description = "가게 찜 해당 여부 조회 성공",
            content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> checkIsFavorite(@PathVariable("storeId") Integer storeId) {
        String userEmail = MemberInfo.getEmail();
        return ResponseEntity.status(HttpStatus.OK).body(favoritesService.checkIsFavorite(userEmail, storeId));
    }

}
