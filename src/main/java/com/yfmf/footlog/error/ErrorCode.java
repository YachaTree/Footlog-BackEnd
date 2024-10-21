package com.yfmf.footlog.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error", "서버에 문제 발생했습니다.", "FOOTLOG-000"),
    /* User 도메인 예외 */
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "Not Found Member", "해당 회원은 존재하지 않습니다.", "USER-000"),
    EMPTY_EMAIL_MEMBER(HttpStatus.NOT_FOUND, "Not Found Email", "해당 이메일은 회원 가입 되지 않은 이메일입니다.", "USER-001"),
    SAME_EMAIL(HttpStatus.CONFLICT, "Already Existed", "이미 가입된 이메일입니다.", "USER-002"),
    EMAIL_STRUCTURE(HttpStatus.FORBIDDEN, "Invalid Email Structure", "이메일 형식으로 작성해주세요.", "USER-003"),
    INVALID_PASSWORD(HttpStatus.CONFLICT, "Invalid Password", "비밀번호가 유효하지 않습니다.", "USER-004"),
    INVALID_EMAIL_CODE(HttpStatus.CONFLICT, "Invalid Email Code", "이메일 인증 코드가 일치하지 않습니다.", "USER-005"),
    INVALID_EMAIL(HttpStatus.CONFLICT, "Invalid Email", "이메일이 인증되지 않았습니다.", "USER-006"),
    EMAIL_NON_EXIST(HttpStatus.CONFLICT, "Not Existed Email", "해당 이메일의 회원을 찾을 수 없습니다.", "USER-007"),
    /* Email 도메인 예외 */
    NO_SUCH_ALGORITHM(HttpStatus.INTERNAL_SERVER_ERROR, "No Such Algorithm", "사용 가능한 암호화 알고리즘을 찾을 수 없습니다.", "EMAIL-001"),
    /* Token 도메인 예외 */
    FAILED_VALIDATE_ACCESS_TOKEN(HttpStatus.EXPECTATION_FAILED, "Invalid Access Token", "유효하지 않은 Access Token 입니다.", "TOKEN-001"),
    FAILED_VALIDATE_REFRESH_TOKEN(HttpStatus.EXPECTATION_FAILED, "Invalid Refresh Token", "유효하지 않은 Refresh Token 입니다.", "TOKEN-002"),
    FAILED_GET_ACCESS_TOKEN(HttpStatus.EXPECTATION_FAILED, "Failed to Get Access Token", "Access Token 을 가져오는데 실패했습니다.", "TOKEN-003"),
    IS_NOT_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "Invalid Refresh Token", "Refresh Token 이 아닙니다.", "TOKEN-004"),
    FAILED_GET_KAKAO_PROFILE(HttpStatus.EXPECTATION_FAILED, "Failed to Get Kakao Profile", "Kakao 유저 프로필을 가져오는데 실패했습니다.", "SOCIAL-001"),
    FAILED_GET_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "Failed to Get Refresh Token", "Refresh Token 을 얻을 수 없습니다.", "TOKEN-004"),
    /* Auth 도메인 예외 */
    FAILED_AUTHENTICATION(HttpStatus.UNAUTHORIZED, "Authentication Failed", "인증에 실패하였습니다.", "AUTH-001"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access Denied", "접근이 거부되었습니다.", "AUTH-002"),
    DIFFERENT_IP_ADDRESS(HttpStatus.BAD_REQUEST, "Different IP Address", "기존 IP 주소와 다른 IP 주소에서의 요청입니다.", "AUTH-003"),
    ANONYMOUS_USER(HttpStatus.UNAUTHORIZED, "Anonymous User", "익명의 유저가 접근하였습니다.", "AUTH-004"),
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "login required", "로그인 후 이용이 가능합니다.", "AUTH-005"),
    GUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "Guest Not Found", "해당하는 게스트가 없습니다.", "GUEST-001"),
    /* Club 도메인 예외 */
    INVALID_CLUB(HttpStatus.CONFLICT, "Invalid Club", "유효하지 않은 구단입니다.", "CLUB-001"),
    DUPLICATED_CLUB(HttpStatus.CONFLICT, "Duplicated Club", "이미 존재하는 구단입니다.", "CLUB-002"),
    NOT_FOUND_CLUB(HttpStatus.NOT_FOUND, "Not Found Club", "해당 구단을 찾을 수 없습니다.", "CLUB-003"),
    /* Club 도메인 예외 */
    REDIS_SAVE_FAILED(HttpStatus.BAD_REQUEST, "Not Saved RefreshToken", "리프레시 토큰이 저장되지 않았습니다.", "REDIS-001");



    private final String description;
    private final HttpStatus status;
    private final String code;
    private final String errorType;

    ErrorCode(HttpStatus status, String errorType, String description, String code) {
        this.status = status;
        this.errorType = errorType;
        this.description = description;
        this.code = code;
    }
}
