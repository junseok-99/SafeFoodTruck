package com.safefoodtruck.sft.review.controller;

import static org.springframework.http.HttpStatus.*;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.safefoodtruck.sft.review.dto.request.ReviewRegistRequestDto;
import com.safefoodtruck.sft.review.dto.response.ReviewListResponseDto;
import com.safefoodtruck.sft.review.dto.response.ReviewResponseDto;
import com.safefoodtruck.sft.review.service.ReviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestMapping("/reviews")
@RestController
@RequiredArgsConstructor
public class ReviewController {

	private final ReviewService reviewService;

	@PostMapping
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "리뷰 등록", description = "리뷰를 등록할 때 사용하는 API")
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "201",
			description = "리뷰 등록에 성공하였습니다!",
			content = @Content(mediaType = "application/json")
		),
		@ApiResponse(
			responseCode = "500",
			description = "Error Message 로 전달함",
			content = @Content(mediaType = "application/json")
		)
	})
	public ResponseEntity<ReviewResponseDto> registReview(
		@RequestBody ReviewRegistRequestDto reviewRegistRequestDto) {
		ReviewResponseDto reviewResponseDto = reviewService.registReview(reviewRegistRequestDto);
		return new ResponseEntity<>(reviewResponseDto, CREATED);
	}

	@GetMapping
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "내가 쓴 리뷰 조회", description = "내가 쓴 리뷰를 조회할 때 사용하는 API")
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "리뷰 조회에 성공하였습니다!",
			content = @Content(mediaType = "application/json")
		),
		@ApiResponse(
			responseCode = "500",
			description = "Error Message 로 전달함",
			content = @Content(mediaType = "application/json")
		)
	})
	public ResponseEntity<ReviewListResponseDto> findCustomerReviews() {
		ReviewListResponseDto customerReviews = reviewService.findCustomerReviews();
		return new ResponseEntity<>(customerReviews, OK);
	}

	@GetMapping("{storeId}")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "가게 리뷰 조회", description = "가게 리뷰를 조회할 때 사용하는 API")
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "가게 리뷰 조회에 성공하였습니다!",
			content = @Content(mediaType = "application/json")
		),
		@ApiResponse(
			responseCode = "500",
			description = "Error Message 로 전달함",
			content = @Content(mediaType = "application/json")
		)
	})
	public ResponseEntity<ReviewListResponseDto> findStoreReviews(
		@PathVariable final Integer storeId) {
		ReviewListResponseDto storeReviews = reviewService.findStoreReviews(storeId);
		return new ResponseEntity<>(storeReviews, OK);
	}
}
