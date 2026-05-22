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
import java.util.List;
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


    // 약 등록
    public Long createMedication(MedicationSaveRequest request) throws IOException, ApiException {
        String body = gson.toJson(request);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/medications")
                .post(RequestBody.create(body, JSON))
                .build();
        String response = execute(httpRequest);
        return gson.fromJson(response, Long.class);
    }

    // 스케줄 등록
    public Long createSchedule(ScheduleSaveRequest request) throws IOException, ApiException {
        String body = gson.toJson(request);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/schedules")
                .post(RequestBody.create(body, JSON))
                .build();
        String response = execute(httpRequest);
        return gson.fromJson(response, Long.class);
    }

    // DTO 추가
    public static class MedicationSaveRequest {
        public Long userId;
        public String medicationName;
        public String ingredient;
        public String purpose;
        public String startDate;
        public String endDate;
    }

    public static class ScheduleSaveRequest {
        public Long userId;
        public Long medicationId;
        public String timeSlot;
        public String scheduledTime;
        public double quantity;
        public String unit;
        public boolean notificationEnabled;
    }

    // 사용자 스케줄 목록 조회
    public List<MedicationScheduleResponse> getSchedulesByUser(long userId)
            throws IOException, ApiException {
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/schedules/user/" + userId)
                .get()
                .build();
        String response = execute(httpRequest);
        java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<
                List<MedicationScheduleResponse>>(){}.getType();
        return gson.fromJson(response, type);
    }

    // 복약 완료 기록
    public void takeMedication(long scheduleId, IntakeTakeRequest request)
            throws IOException, ApiException {
        String body = gson.toJson(request);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/intakes/" + scheduleId + "/take")
                .post(RequestBody.create(body, JSON))
                .build();
        execute(httpRequest);
    }

    // 복약 완료 취소
    public void cancelTake(long scheduleId, IntakeTakeRequest request)
            throws IOException, ApiException {
        String body = gson.toJson(request);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/intakes/" + scheduleId + "/cancel")
                .post(RequestBody.create(body, JSON))
                .build();
        execute(httpRequest);
    }

    // DTO 추가
    public static class MedicationScheduleResponse {
        public long id;           // scheduleId
        public long medicationId;
        public String medicationName;
        public String timeSlot;   // MORNING, LUNCH, DINNER, BEDTIME
        public String scheduledTime;
        public double quantity;
        public String unit;
        public boolean notificationEnabled;
    }

    public static class IntakeTakeRequest {
        public long userId;
        public String date;  // "yyyy-MM-dd"
        public String memo;
    }

    // 피보호자 오늘 복약 현황 (보호자용)
    public PatientIntakeStatusResponse getPatientIntakeStatus(
            long caregiverId, long patientId) throws IOException, ApiException {
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/patients/" + patientId
                        + "/intake-status?caregiverId=" + caregiverId)
                .get()
                .build();
        String response = execute(httpRequest);
        return gson.fromJson(response, PatientIntakeStatusResponse.class);
    }

    // 피보호자 월별 복약 기록 (보호자용)
    public List<IntakeLogResponse> getPatientMonthlyLogs(
            long patientId, int year, int month) throws IOException, ApiException {
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/intakes/users/" + patientId
                        + "/monthly?year=" + year + "&month=" + month)
                .get()
                .build();
        String response = execute(httpRequest);
        java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<
                List<IntakeLogResponse>>(){}.getType();
        return gson.fromJson(response, type);
    }

    // DTO 추가
    public static class PatientIntakeStatusResponse {
        public long patientId;
        public String patientName;
        public List<IntakeItemResponse> IntakeLogs;
    }

    public static class IntakeItemResponse {
        public String medicationName;
        public String scheduledTime;
        public String status;   // TAKEN, SCHEDULED, MISSED
        public String takenAt;
        public String memo;
        public double quantity;
        public String unit;
    }

    public static class IntakeLogResponse {
        public long intakeLogId;
        public long scheduleId;
        public long medicationId;
        public String medicationName;
        public String date;
        public String scheduledTime;
        public String timeSlot;
        public double quantity;
        public String unit;
        public String status;   // TAKEN, SCHEDULED, MISSED
        public String takenAt;
        public String memo;
    }

    // 사용자 약 목록 조회
    public List<MedicationResponse> getMedicationsByUser(long userId)
            throws IOException, ApiException {
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/medications/user/" + userId)
                .get()
                .build();
        String response = execute(httpRequest);
        java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<
                List<MedicationResponse>>(){}.getType();
        return gson.fromJson(response, type);
    }

    public static class MedicationResponse {
        public long id;
        public String medicationName;
        public String ingredient;
        public String purpose;
        public String startDate;
        public String endDate;
        public String status;
    }

    // 약 상태 변경 (ACTIVE → STOPPED)
    public void changeMedicationStatus(long medicationId, long userId, String status)
            throws IOException, ApiException {
        MedicationStatusChangeRequest request = new MedicationStatusChangeRequest();
        request.userId = userId;
        request.status = status;

        String body = gson.toJson(request);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/medications/" + medicationId + "/status")
                .patch(RequestBody.create(body, JSON))
                .build();
        execute(httpRequest);
    }

    // DTO 추가
    public static class MedicationStatusChangeRequest {
        public Long userId;
        public String status;
    }

    // 스케줄 시간 업데이트
    public void updateScheduleTime(long scheduleId, long userId, String scheduledTime)
            throws IOException, ApiException {

        ScheduleTimeUpdateRequest request = new ScheduleTimeUpdateRequest();
        request.userId = userId;
        request.scheduledTime = scheduledTime;
        // 기존 값 유지용 더미값
        request.timeSlot = null;
        request.quantity = 1.0;
        request.unit = "정";
        request.notificationEnabled = true;

        String body = gson.toJson(request);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/schedules/" + scheduleId)
                .put(RequestBody.create(body, JSON))
                .build();
        execute(httpRequest);
    }

    public static class ScheduleTimeUpdateRequest {
        public Long userId;
        public Long medicationId;
        public String timeSlot;
        public String scheduledTime;
        public double quantity;
        public String unit;
        public boolean notificationEnabled;
    }

}
