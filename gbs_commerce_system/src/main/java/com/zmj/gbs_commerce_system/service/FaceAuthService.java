package com.zmj.gbs_commerce_system.service;

import java.util.Optional;

public interface FaceAuthService {
    Optional<String> matchAndGetUsername(String base64Image);
}
