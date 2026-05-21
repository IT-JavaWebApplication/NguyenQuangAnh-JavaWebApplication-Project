package com.smartacademic.service;

import com.smartacademic.entity.MentoringSession;

import java.util.concurrent.CompletableFuture;

/** Gửi email bất đồng bộ (Async) để không chặn luồng request chính. */
public interface EmailService {

    CompletableFuture<Boolean> sendBookingConfirmation(String toEmail, String studentName, MentoringSession session);

    CompletableFuture<Boolean> sendCancellationNotice(String toEmail, String studentName, MentoringSession session, String reason);

    CompletableFuture<Boolean> sendEvaluationReady(String toEmail, String studentName, MentoringSession session);

    CompletableFuture<Boolean> sendOverdueAlert(String toEmail, String studentName, Long borrowingRecordId);
}
