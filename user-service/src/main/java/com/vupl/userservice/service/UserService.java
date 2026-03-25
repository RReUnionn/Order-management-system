package com.vupl.userservice.service;

import com.vupl.userservice.dto.request.*;
import com.vupl.userservice.dto.response.AddressResponse;
import com.vupl.userservice.dto.response.AuthResponse;
import com.vupl.userservice.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserResponse getProfile(String email);
    UserResponse updateProfile(String email, UpdateProfileRequest request);
    void changePassword(String email, ChangePasswordRequest request);

    // Address
    List<AddressResponse> getAddresses(String email);
    AddressResponse addAddress(String email, AddressRequest request);
    AddressResponse updateAddress(String email, String addressId, AddressRequest request);
    void deleteAddress(String email, String addressId);
    void setDefaultAddress(String email, String addressId);
}
