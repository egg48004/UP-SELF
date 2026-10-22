package com.upself.web.model;

import lombok.Data;

import javax.validation.constraints.*;

public final class AuthBodies {
    @Data
    public static class Login {
        @NotBlank(message = "Enter your username.")
        @Pattern(
                regexp = "[A-Za-z0-9_.@-]{3,100}",
                message =
                        "Use 3 to 100 letters, digits, dots, underscores, hyphens or @ characters.")
        private String username;

        @NotBlank(message = "Enter your password.")
        @Size(max = 128, message = "The password is too long.")
        private String password;
    }

    @Data
    public static class Totp {
        @NotBlank
        @Size(max = 64)
        private String ticket;

        @Pattern(regexp = "[0-9]{6}", message = "Enter a six-digit verification code.")
        @NotBlank
        private String code;
    }

    @Data
    public static class Password {
        @NotBlank private String oldPassword;

        @NotBlank
        @Size(min = 10, max = 64, message = "The new password must contain 10 to 64 characters.")
        private String newPassword;
    }

    @Data
    public static class Create {
        @NotBlank
        @Pattern(
                regexp = "[A-Za-z0-9_.@-]{3,100}",
                message =
                        "Use 3 to 100 letters, digits, dots, underscores, hyphens or @ characters.")
        private String userName;

        @NotBlank
        @Size(max = 60)
        private String nickName;

        @NotNull private Long roleId;

        @NotBlank
        @Size(min = 10, max = 64)
        private String password;
    }

    @Data
    public static class Update {
        @NotNull private Long id;

        @NotBlank
        @Size(max = 60)
        private String nickName;

        @NotNull private Long roleId;

        @Pattern(regexp = "[01]")
        @NotBlank
        private String status;
    }

    @Data
    public static class ResetPassword {
        @NotNull private Long id;

        @NotBlank
        @Size(min = 10, max = 64)
        private String password;
    }
}
