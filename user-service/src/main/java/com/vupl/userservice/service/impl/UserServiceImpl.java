package com.vupl.userservice.service.impl;

import com.vupl.userservice.dto.request.*;
import com.vupl.userservice.dto.response.AddressResponse;
import com.vupl.userservice.dto.response.AuthResponse;
import com.vupl.userservice.dto.response.UserResponse;
import com.vupl.userservice.entity.RefreshToken;
import com.vupl.userservice.entity.Role;
import com.vupl.userservice.entity.User;
import com.vupl.userservice.entity.UserAddress;
import com.vupl.userservice.exception.AppException;
import com.vupl.userservice.repository.RefreshTokenRepository;
import com.vupl.userservice.repository.RoleRepository;
import com.vupl.userservice.repository.UserAddressRepository;
import com.vupl.userservice.repository.UserRepository;
import com.vupl.userservice.security.JwtService;
import com.vupl.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserAddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw AppException.badRequest("Email đã được sử dụng");

        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone()))
            throw AppException.badRequest("Số điện thoại đã được sử dụng");

        Role role = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> AppException.notFound("Role không tồn tại"));

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(role)
                .build();

        User saved = userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        saveRefreshToken(saved, refreshToken);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration())
                .user(UserResponse.from(user))
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> AppException.notFound("Người dùng không tồn tại"));

        if (user.getStatus() == User.UserStatus.BANNED)
            throw AppException.forbidden("Tài khoản đã bị khóa");

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        saveRefreshToken(user, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration())
                .user(UserResponse.from(user))
                .build();
    }

    @Override
    public void logout(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {
        User user = findByEmail(email);
        return UserResponse.from(user);
    }

    @Override
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = findByEmail(email);
        if (request.getPhone() != null
                && !request.getPhone().equals(user.getPhone())
                && userRepository.existsByPhone(request.getPhone())) {
            throw AppException.badRequest("Số điện thoại đã được sử dụng");
        }
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = findByEmail(email);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash()))
            throw AppException.badRequest("Mật khẩu hiện tại không đúng");
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(String email) {
        User user = findByEmail(email);
        return addressRepository.findByUserId(user.getId())
                .stream().map(AddressResponse::from).collect(Collectors.toList());
    }

    @Override
    public AddressResponse addAddress(String email, AddressRequest request) {
        User user = findByEmail(email);
        if (Boolean.TRUE.equals(request.getIsDefault()))
            addressRepository.clearDefaultByUserId(user.getId());

        UserAddress address = UserAddress.builder()
                .user(user)
                .recipientName(request.getRecipientName())
                .phone(request.getPhone())
                .province(request.getProvince())
                .district(request.getDistrict())
                .ward(request.getWard())
                .street(request.getStreet())
                .addressType(request.getAddressType())
                .isDefault(request.getIsDefault())
                .build();

        return AddressResponse.from(addressRepository.save(address));
    }

    @Override
    public AddressResponse updateAddress(String email, String addressId, AddressRequest request) {
        User user = findByEmail(email);
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> AppException.notFound("Địa chỉ không tồn tại"));

        if (!address.getUser().getId().equals(user.getId()))
            throw AppException.forbidden("Không có quyền chỉnh sửa địa chỉ này");

        if (Boolean.TRUE.equals(request.getIsDefault()))
            addressRepository.clearDefaultByUserId(user.getId());

        address.setRecipientName(request.getRecipientName());
        address.setPhone(request.getPhone());
        address.setProvince(request.getProvince());
        address.setDistrict(request.getDistrict());
        address.setWard(request.getWard());
        address.setStreet(request.getStreet());
        address.setAddressType(request.getAddressType());
        address.setIsDefault(request.getIsDefault());

        return AddressResponse.from(addressRepository.save(address));
    }

    @Override
    public void deleteAddress(String email, String addressId) {
        User user = findByEmail(email);
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> AppException.notFound("Địa chỉ không tồn tại"));
        if (!address.getUser().getId().equals(user.getId()))
            throw AppException.forbidden("Không có quyền xóa địa chỉ này");
        addressRepository.delete(address);
    }

    @Override
    public void setDefaultAddress(String email, String addressId) {
        User user = findByEmail(email);
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> AppException.notFound("Địa chỉ không tồn tại"));
        if (!address.getUser().getId().equals(user.getId()))
            throw AppException.forbidden("Không có quyền thao tác địa chỉ này");
        addressRepository.clearDefaultByUserId(user.getId());
        address.setIsDefault(true);
        addressRepository.save(address);
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.notFound("Người dùng không tồn tại"));
    }

    private void saveRefreshToken(User user, String rawToken) {
        // Không lưu token thô, chỉ lưu hash để bảo mật
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawToken))
                .expiresAt(LocalDateTime.now().plusSeconds(
                        jwtService.getRefreshTokenExpiration() / 1000))
                .build());
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Cannot hash token", e);
        }
    }
    @Override
    public AuthResponse refreshToken(String refreshToken) {
        // 1. Kiểm tra token còn hợp lệ không
        String email = jwtService.extractUsername(refreshToken);
        if (email == null) throw AppException.unauthorized("Token không hợp lệ");

        // 2. Tìm trong DB theo hash
        String tokenHash = hashToken(refreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> AppException.unauthorized("Refresh token không tồn tại"));

        if (stored.getRevoked())
            throw AppException.unauthorized("Refresh token đã bị thu hồi");

        if (stored.getExpiresAt().isBefore(LocalDateTime.now()))
            throw AppException.unauthorized("Refresh token đã hết hạn");

        // 3. Revoke token cũ (rotation: mỗi lần refresh dùng token mới)
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        // 4. Cấp token mới
        User user = stored.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String newAccessToken  = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        saveRefreshToken(user, newRefreshToken);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration())
                .user(UserResponse.from(user))
                .build();
    }
}
