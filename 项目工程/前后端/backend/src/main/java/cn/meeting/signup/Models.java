package cn.meeting.signup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.Locale;

public final class Models {
    private Models() {}
    public record User(long id, String username, String displayName, String passwordHash) {}
    public record PublicUser(long id, String username, String displayName) {}
    public record LoginRequest(@NotBlank @Size(max=32) String username,
                               @NotBlank @Size(max=64) String password) {
        public LoginRequest { username=username==null?null:username.trim().toLowerCase(Locale.ROOT); }
    }
    public record LoginResult(String token, PublicUser user, int expiresInSeconds) {}
    public record CreateAccountRequest(
            @NotBlank @Pattern(regexp="[a-z][a-z0-9_]{2,31}",message="账号须为3至32位字母、数字或下划线，以字母开头") String username,
            @NotBlank @Size(max=32,message="称呼最多32个字符") String displayName,
            @NotBlank @Size(min=8,max=64,message="密码须为8至64个字符") String password) {
        public CreateAccountRequest {
            username=username==null?null:username.trim().toLowerCase(Locale.ROOT);
            displayName=displayName==null?null:displayName.strip();
        }
    }
    public record EventRow(long id, String title, int capacity) {}
    public record EventView(long id, String title, int capacity, int registeredCount,
                            int remainingSeats, Long myRegistrationId) {}
    public record RegistrationRequest() {}
    public record RegistrationView(long id, long eventId, String eventTitle, long userId,
                                   String username, String displayName, LocalDateTime createdAt) {}
    public record ApiError(String code, String message, String requestId) {}
}
