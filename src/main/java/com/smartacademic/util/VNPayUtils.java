package com.smartacademic.util;

import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

/**
 * Helper cho việc tạo URL thanh toán VNPay & verify chữ ký callback.
 *
 * Quy tắc VNPay:
 *  - Sort tất cả tham số có prefix "vnp_" theo alphabet.
 *  - Build chuỗi hash: key=value&key=value... (URL-encode value bằng UTF-8).
 *  - HMAC-SHA512 chuỗi đó với hash secret → vnp_SecureHash.
 *  - Khi callback: lấy tất cả vnp_* (bỏ vnp_SecureHash + vnp_SecureHashType),
 *    tự build lại hash rồi so sánh với vnp_SecureHash gửi về.
 */
public final class VNPayUtils {

    private VNPayUtils() {}

    private static final SecureRandom RNG = new SecureRandom();

    /** Sinh mã giao dịch nội bộ (vnp_TxnRef) - 14 chữ số. */
    public static String generateTxnRef() {
        long t = System.currentTimeMillis() % 100_000_000L;
        int  r = 100_000 + RNG.nextInt(900_000);
        return String.format("%08d%06d", t, r);
    }

    /** Lấy IP client (có xét X-Forwarded-For trong môi trường có reverse proxy). */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if ("0:0:0:0:0:0:0:1".equals(ip)) ip = "127.0.0.1";
        return ip;
    }

    /**
     * Build hash data theo chuẩn VNPay từ map params (đã không chứa vnp_SecureHash).
     */
    public static String buildHashData(Map<String, String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        StringBuilder sb = new StringBuilder();
        for (Iterator<String> it = keys.iterator(); it.hasNext();) {
            String key = it.next();
            String val = params.get(key);
            if (val == null || val.isEmpty()) continue;
            sb.append(key).append('=')
              .append(URLEncoder.encode(val, StandardCharsets.US_ASCII));
            if (it.hasNext()) sb.append('&');
        }
        return sb.toString();
    }

    /** Build query string đã URL-encode (giống hash data) để append vào URL. */
    public static String buildQueryString(Map<String, String> params) {
        return buildHashData(params);
    }

    /** Tính HMAC-SHA512 chuỗi data với khoá bí mật, trả về hex lowercase. */
    public static String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Lỗi tính HMAC-SHA512: " + e.getMessage(), e);
        }
    }

    /**
     * Verify chữ ký từ callback: extract & remove vnp_SecureHash, build lại hash từ phần
     * còn lại, so sánh với hash gốc.
     */
    public static boolean verifySignature(Map<String, String> allParams, String hashSecret) {
        if (allParams == null || !allParams.containsKey("vnp_SecureHash")) return false;
        Map<String, String> mutable = new HashMap<>(allParams);
        String receivedHash = mutable.remove("vnp_SecureHash");
        mutable.remove("vnp_SecureHashType");

        String hashData = buildHashData(mutable);
        String calc = hmacSHA512(hashSecret, hashData);
        return calc.equalsIgnoreCase(receivedHash);
    }
}
