package com.forkast.backend.email;

public interface EmailService {

    void sendVerificationCode(String to, String firstName, String code);

    void sendPasswordResetCode(String to, String firstName, String code);

    void sendEmailChangeCode(String to, String firstName, String code);

    void sendEmailChangedNotice(String oldAddress, String firstName, String newAddress);

    void sendPasswordChangedNotice(String to, String firstName);
}