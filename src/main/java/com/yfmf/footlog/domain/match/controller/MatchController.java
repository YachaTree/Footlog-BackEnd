package com.yfmf.footlog.domain.match.controller;

import com.yfmf.footlog.domain.auth.dto.LoginedInfo;
import com.yfmf.footlog.domain.auth.exception.LoginRequiredException;
import com.yfmf.footlog.domain.match.dto.MatchRegisterRequestDTO;
import com.yfmf.footlog.domain.match.dto.MatchResponseDTO;
import com.yfmf.footlog.domain.match.entity.Match;
import com.yfmf.footlog.domain.match.service.MatchService;
import com.yfmf.footlog.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@Tag(name = "Match API", description = "매치 관리 API")
@RequestMapping("/api/v1/matches")
public class MatchController {

    private MatchService matchService;

    @Autowired
    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    // 매치 id로 매치 정보 반환
    @GetMapping("/detail/{matchId}")
    public MatchResponseDTO findAllMatches(@PathVariable("matchId") Long matchId) {
        return matchService.findMatchByMatchId(matchId);
    }

    // 모든 경기 조회
    @Operation(summary = "모든 경기 조회", description = "등록된 모든 경기를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "경기 목록이 성공적으로 조회되었습니다."),
            @ApiResponse(responseCode = "404", description = "경기목록이 존재하지 않습니다.", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(
                            value = "{\"status\": 404, \"errorType\": \"Not Found\", \"message\": \"경기 목록이 존재하지 않습니다.\"}"
                    )
            ))
    })
    @GetMapping
    public ResponseEntity<List<MatchResponseDTO>> getAllMatches() {
        List<MatchResponseDTO> matches = matchService.findAllMatches();
        return ResponseEntity.ok(matches);
    }

    // 단일 경기 조회
    @Operation(summary = "단일 경기 조회", description = "등록된 단일 경기를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "등록된 경기를 성공적으로 조회되었습니다."),
            @ApiResponse(responseCode = "404", description = "등록된 경기가 존재하지 않습니다.", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(
                            value = "{\"status\": 404, \"errorType\": \"Not Found\", \"message\": \"등록된 경기 정보가 존재하지 않습니다.\"}"
                    )
            ))
    })
    @GetMapping("/{matchId}")
    public ResponseEntity<MatchResponseDTO> getMatchById(@PathVariable("matchId") Long matchId) {
        MatchResponseDTO match = matchService.findMatchByMatchId(matchId);
        return ResponseEntity.ok(match);
    }

    // 경기 등록
    @Operation(summary = "경기 등록", description = "경기를 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "경기가 성공적으로 등록되었습니다.", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MatchResponseDTO.class),
                    examples = @ExampleObject(
                            value = "{\"matchId\": 1, \"matchIntroduce\": \"Friendly match\", \"matchStatus\": \"PENDING\", \"fieldLocation\": \"Seoul\", ...}"
                    )
            )),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다. 입력 데이터를 확인해주세요.", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(
                            value = "{\"status\": 400, \"errorType\": \"Bad Request\", \"message\": \"필수 필드가 누락되었습니다.\"}"
                    )
            ))
    })
    @PostMapping
    public ResponseEntity<MatchResponseDTO> createMatch(@RequestBody @Valid MatchRegisterRequestDTO matchRegisterRequestDTO,
                                                        @AuthenticationPrincipal LoginedInfo logined) {

        log.info("[MatchController-createMatch] 경기 등록 요청 시작: ", matchRegisterRequestDTO.getMatchEnrollUserId());

        if (logined == null) {
            log.error("[MatchController-createMatch] 로그인되지 않은 사용자가 구단 등록 시도.");
            throw new LoginRequiredException("로그인 후 이용이 가능합니다.", "[MatchController-createMatch] 시 로그인 정보 확인하세요.");
        }
        log.info("matchRegisterRequestDTO: {}", matchRegisterRequestDTO.getMatchEnrollUserId());

        log.trace("matchRegisterRequestDTO: {}", matchRegisterRequestDTO.toString());
        if(matchRegisterRequestDTO.getMatchEnrollUserId() == null) {
            matchRegisterRequestDTO.setMatchEnrollUserId(logined.getUserId());
        }
        log.info("matchRegisterRequestDTO set: {}", matchRegisterRequestDTO.getMatchEnrollUserId());
        // 자동으로 설정되어야 할 설정들? 로그인 정보에서 작성자 아이디 fetch.

        MatchResponseDTO createdMatch = matchService.saveMatch(matchRegisterRequestDTO);
        log.info("[MatchController-createMatch] 경기 등록 성공, 매치 상태 확인하기 : ", createdMatch.getMatchStatus());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMatch);
    }

    // 경기 수정
    @Operation(summary = "경기 수정", description = "등록된 경기를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "경기가 성공적으로 수정되었습니다.", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MatchResponseDTO.class),
                    examples = @ExampleObject(
                            value = "{\"matchId\": 1, \"matchIntroduce\": \"Updated match description\", \"matchStatus\": \"IN_PROGRESS\", \"fieldLocation\": \"Busan\", \"matchDate\": \"2024-10-05\", \"matchStartTime\": \"15:00\", \"matchEndTime\": \"17:00\", \"myClub\": {\"clubId\": 1, \"clubName\": \"Seoul FC\"}, \"enemyClub\": {\"clubId\": 2, \"clubName\": \"Busan FC\"}}"
                    )
            )),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다. 입력 데이터를 확인해주세요.", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(
                            value = "{\"status\": 400, \"errorType\": \"Bad Request\", \"message\": \"필수 필드가 누락되었습니다.\"}"
                    )
            )),
            @ApiResponse(responseCode = "404", description = "등록된 경기가 존재하지 않습니다.", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(
                            value = "{\"status\": 404, \"errorType\": \"Not Found\", \"message\": \"경기를 찾을 수 없습니다. matchId=1\"}"
                    )
            ))
    })
    @PutMapping("/{matchId}")
    public ResponseEntity<MatchResponseDTO> updateMatch(@PathVariable("matchId") Long matchId, @RequestBody MatchRegisterRequestDTO updateRequestDTO) {
        MatchResponseDTO updatedMatch = matchService.updateMatch(matchId, updateRequestDTO);
        return ResponseEntity.ok(updatedMatch);
    }

    // 경기 삭제
    @Operation(summary = "경기 삭제", description = "등록된 경기를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "경기가 성공적으로 삭제되었습니다."),
            @ApiResponse(responseCode = "404", description = "등록된 경기가 존재하지 않습니다.", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(
                            value = "{\"status\": 404, \"errorType\": \"Not Found\", \"message\": \"경기를 찾을 수 없습니다. matchId=1\"}"
                    )
            ))
    })
    @DeleteMapping("/{matchId}")
    public ResponseEntity<Void> deleteMatch(@PathVariable("matchId") Long matchId) {
        matchService.removeMatch(matchId);
        return ResponseEntity.noContent().build();
    }

    // 매칭 신청
    @PostMapping("/{matchId}/application")
    public ResponseEntity<MatchResponseDTO> applyForMatch(@PathVariable("matchId") Long matchId,
                                                          @AuthenticationPrincipal LoginedInfo logined,
                                                          @RequestParam("enemyClubId") Long enemyClubId) {

        if (logined == null) {
            log.error("[MatchController-applyForMatch] 로그인되지 않은 사용자가 매칭 신청 시도.");
            throw new LoginRequiredException("로그인 후 이용이 가능합니다.", "[MatchController-applyForMatch] 시 로그인 정보 확인하세요.");
        }

        Long applyingUserId = logined.getUserId();

        // 매칭 신청 처리
        Match updatedMatch = matchService.applyForMatch(matchId, applyingUserId, enemyClubId);

        // 신청 후 MatchResponseDTO로 반환
        MatchResponseDTO matchResponseDTO = new MatchResponseDTO(updatedMatch);
        return ResponseEntity.ok(matchResponseDTO);
    }

    // 매칭 수락
    @PostMapping("/{matchId}/accept")
    public ResponseEntity<MatchResponseDTO> acceptMatch(@PathVariable("matchId") Long matchId,
                                                        @AuthenticationPrincipal LoginedInfo logined) {

        if (logined == null) {
            log.error("[MatchController-acceptMatch] 로그인되지 않은 사용자가 매칭 거절 시도.");
            throw new LoginRequiredException("로그인 후 이용이 가능합니다.", "[MatchController-acceptMatch] 시 로그인 정보 확인하세요.");
        }

        Long matchOwnerId = logined.getUserId();
        // 매칭 수락 처리
        Match acceptedMatch = matchService.acceptMatch(matchId, matchOwnerId);

        // 수락 후 MatchResponseDTO로 반환
        MatchResponseDTO matchResponseDTO = new MatchResponseDTO(acceptedMatch);
        return ResponseEntity.ok(matchResponseDTO);
    }

    // 매칭 거절
    @PostMapping("/{matchId}/reject")
    public ResponseEntity<MatchResponseDTO> rejectMatch(@PathVariable("matchId") Long matchId,
                                                        @AuthenticationPrincipal LoginedInfo logined) {

        if (logined == null) {
            log.error("[MatchController-rejectMatch] 로그인되지 않은 사용자가 매칭 거절 시도.");
            throw new LoginRequiredException("로그인 후 이용이 가능합니다.", "[MatchController-rejectMatch] 시 로그인 정보 확인하세요.");
        }

        Long matchOwnerId = logined.getUserId();
        // 매칭 거절 처리
        Match rejectedMatch = matchService.rejectMatch(matchId, matchOwnerId);

        // 거절 후 MatchResponseDTO로 반환
        MatchResponseDTO matchResponseDTO = new MatchResponseDTO(rejectedMatch);
        return ResponseEntity.ok(matchResponseDTO);
    }


}
