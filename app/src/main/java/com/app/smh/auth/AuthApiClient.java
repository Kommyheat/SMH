package com.app.smh.auth;

import com.app.smh.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AuthApiClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build();
    private final Gson gson = new Gson();
    private final String baseUrl = BuildConfig.SERVER_BASE_URL;

    public MessageResponse signup(SignupRequest request) throws IOException, ApiException {
        String body = gson.toJson(request);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/auth/signup")
                .post(RequestBody.create(body, JSON))
                .build();

        String response = execute(httpRequest);
        JsonObject obj = gson.fromJson(response, JsonObject.class);
        String loginId = obj.has("loginId") && !obj.get("loginId").isJsonNull()
                ? obj.get("loginId").getAsString()
                : "";
        return new MessageResponse(loginId + " 계정의 회원가입이 완료되었습니다.");
    }

    public LoginResponse login(LoginRequest request) throws IOException, ApiException {
        String body = gson.toJson(request);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/auth/login")
                .post(RequestBody.create(body, JSON))
                .build();

        String response = execute(httpRequest);
        return gson.fromJson(response, LoginResponse.class);
    }

    public boolean checkLoginIdAvailable(String loginId) throws IOException, ApiException {
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/auth/check-login-id?loginId=" + loginId)
                .get()
                .build();

        String response = execute(httpRequest);
        MessageResponse messageResponse = gson.fromJson(response, MessageResponse.class);
        return "AVAILABLE".equals(messageResponse.message);
    }

    public FindLoginIdResponse findLoginId(FindLoginIdRequest request) throws IOException, ApiException {
        String body = gson.toJson(request);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/auth/find-login-id")
                .post(RequestBody.create(body, JSON))
                .build();

        String response = execute(httpRequest);
        return gson.fromJson(response, FindLoginIdResponse.class);
    }

    public FindLoginIdResponse findLoginIdByCode(FindLoginIdByCodeRequest request) throws IOException, ApiException {
        String body = gson.toJson(request);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/auth/find-login-id/by-code")
                .post(RequestBody.create(body, JSON))
                .build();

        String response = execute(httpRequest);
        return gson.fromJson(response, FindLoginIdResponse.class);
    }

    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) throws IOException, ApiException {
        String body = gson.toJson(request);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/auth/reset-password")
                .post(RequestBody.create(body, JSON))
                .build();

        String response = execute(httpRequest);
        return gson.fromJson(response, ResetPasswordResponse.class);
    }

    public MessageResponse sendEmailCode(EmailCodeSendRequest request) throws IOException, ApiException {
        String body = gson.toJson(request);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/auth/email/send-code")
                .post(RequestBody.create(body, JSON))
                .build();

        String response = execute(httpRequest);
        return gson.fromJson(response, MessageResponse.class);
    }

    public EmailCodeVerifyResponse verifyEmailCode(EmailCodeVerifyRequest request) throws IOException, ApiException {
        String body = gson.toJson(request);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/auth/email/verify-code")
                .post(RequestBody.create(body, JSON))
                .build();

        String response = execute(httpRequest);
        return gson.fromJson(response, EmailCodeVerifyResponse.class);
    }

    public MessageResponse resetPasswordByCode(ResetPasswordByCodeRequest request) throws IOException, ApiException {
        String body = gson.toJson(request);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/auth/reset-password/by-code")
                .post(RequestBody.create(body, JSON))
                .build();

        String response = execute(httpRequest);
        return gson.fromJson(response, MessageResponse.class);
    }

    public LinkCodeResponse getMyLinkCode(long userId) throws IOException, ApiException {
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/users/me/link-code?userId=" + userId)
                .get()
                .build();

        String response = execute(httpRequest);
        return gson.fromJson(response, LinkCodeResponse.class);
    }

    // 보호자 연동 요청 로직을 care_links 테이블 기준 API 명칭으로 통일
    public MessageResponse requestCareLink(CareLinkRequest request) throws IOException, ApiException {
        String body = gson.toJson(request);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/care-links/request")
                .post(RequestBody.create(body, JSON))
                .build();

        String response = execute(httpRequest);
        return gson.fromJson(response, MessageResponse.class);
    }

    // 현재 관계 상태를 care_links.status 기반으로 조회하기 위한 메서드 추가
    public CareLinkStatusResponse getCareLinkStatus(long userId) throws IOException, ApiException {
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/care-links/status?userId=" + userId)
                .get()
                .build();

        String response = execute(httpRequest);
        return gson.fromJson(response, CareLinkStatusResponse.class);
    }

    // 요청 수락도 care_links row를 기준으로 처리하므로 careLinkId 사용
    public MessageResponse acceptCareLink(CareLinkDecisionRequest request) throws IOException, ApiException {
        String body = gson.toJson(request);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/care-links/accept")
                .post(RequestBody.create(body, JSON))
                .build();

        String response = execute(httpRequest);
        return gson.fromJson(response, MessageResponse.class);
    }

    // 요청 거절도 동일하게 care_links row 기준으로 처리
    public MessageResponse rejectCareLink(CareLinkDecisionRequest request) throws IOException, ApiException {
        String body = gson.toJson(request);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/care-links/reject")
                .post(RequestBody.create(body, JSON))
                .build();

        String response = execute(httpRequest);
        return gson.fromJson(response, MessageResponse.class);
    }
    private String execute(Request request) throws IOException, ApiException {
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                String message = "요청에 실패했습니다.";
                try {
                    MessageResponse error = gson.fromJson(responseBody, MessageResponse.class);
                    if (error != null && error.message != null && !error.message.isEmpty()) {
                        message = error.message;
                    }
                } catch (Exception ignored) {
                }
                throw new ApiException(message);
            }
            return responseBody;
        }
    }

    public static class ApiException extends Exception {
        public ApiException(String message) {
            super(message);
        }
    }

    public static class SignupRequest {
        public String loginId;
        public String password;
        public String name;
        public String email;
        public String birthDate;
        public String passwordOk;
    }

    public static class LoginRequest {
        public String loginId;
        public String password;
    }

    public static class LoginResponse {
        public long id;
        public String loginId;
        public String name;
        public String birthDate;
        public String email;
    }

    public static class MessageResponse {
        public String message;

        public MessageResponse(String message) {
            this.message = message;
        }
    }

    public static class FindLoginIdRequest {
        public String name;
        public String email;
        public String birthDate;
    }

    public static class FindLoginIdResponse {
        public String loginId;
    }

    public static class ResetPasswordRequest {
        public String loginId;
        public String name;
        public String email;
        public String birthDate;
    }

    public static class ResetPasswordResponse {
        public String temporaryPassword;
    }

    public static class EmailCodeSendRequest {
        public String email;
        public String purpose;
    }

    public static class EmailCodeVerifyRequest {
        public String email;
        public String purpose;
        public String code;
    }

    public static class EmailCodeVerifyResponse {
        public boolean verified;
        public String verifyToken;
    }

    public static class FindLoginIdByCodeRequest {
        public String email;
        public String verifyToken;
    }

    public static class ResetPasswordByCodeRequest {
        public String email;
        public String verifyToken;
        public String newPassword;
    }

    public static class LinkCodeResponse {
        public String linkCode;
    }

    // guardian-links 가정 DTO에서 실제 DB 구조인 care_links 기준 DTO로 명칭 변경
    public static class CareLinkRequest {
        public long caregiverId;
        public String patientUserCode;
    }

    // linkRequestId가 아니라 care_links.id를 직접 다루므로 careLinkId로 변경
    public static class CareLinkDecisionRequest {
        public long patientId;
        public long careLinkId;
    }

    // care_links 테이블 컬럼과 화면 표시용 이름 정보를 함께 받도록 구성
    public static class CareLinkStatusResponse {
        public long id;
        public String status;
        public String linkedAt;
        public String disconnectedAt;

        public long caregiverId;
        public String caregiverName;

        public long patientId;
        public String patientName;
    }

    public void disconnectCareLink(long caregiverId, long linkId) throws IOException, ApiException {
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/care-links/" + linkId + "?caregiverId=" + caregiverId)
                .delete()
                .build();

        execute(httpRequest);
    }

    public UserProfileResponse getUserProfile(long userId) throws IOException, ApiException {
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/users/user?userId=" + userId)
                .get()
                .build();

        String response = execute(httpRequest);
        return gson.fromJson(response, UserProfileResponse.class);
    }

    public static class UserProfileResponse {
        public long id;
        public String loginId;
        public String name;
        public String birthDate;
        public String email;
    }
}
