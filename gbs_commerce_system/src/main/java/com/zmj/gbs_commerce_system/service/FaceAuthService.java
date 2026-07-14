package com.zmj.gbs_commerce_system.service;

import java.util.Optional;

public interface FaceAuthService {
    Optional<String> matchAndGetUsername(String base64Image);
    
    RegisterResult registerFace(String userId, String base64Image, String userInfo);
    
    boolean verifyFaceForMember(String base64Image, String expectedUserId);
    
    boolean checkFaceRegistered(String userId);
    
    class RegisterResult {
        private final boolean success;
        private final String message;
        
        public RegisterResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        
        public static RegisterResult success() { return new RegisterResult(true, "注册成功"); }
        public static RegisterResult fail(String message) { return new RegisterResult(false, message); }
    }
}
