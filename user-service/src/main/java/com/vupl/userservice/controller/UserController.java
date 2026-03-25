package com.vupl.userservice.controller;

import com.vupl.userservice.dto.request.AddressRequest;
import com.vupl.userservice.dto.request.ChangePasswordRequest;
import com.vupl.userservice.dto.request.UpdateProfileRequest;
import com.vupl.userservice.dto.response.AddressResponse;
import com.vupl.userservice.dto.response.ApiResponse;
import com.vupl.userservice.dto.response.UserResponse;
import com.vupl.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getProfile(principal.getName())));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            Principal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateProfile(principal.getName(), request)));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Principal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok("Đổi mật khẩu thành công", null));
    }

    @GetMapping("/me/addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getAddresses(principal.getName())));
    }

    @PostMapping("/me/addresses")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            Principal principal,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.ok(userService.addAddress(principal.getName(), request)));
    }

    @PutMapping("/me/addresses/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            Principal principal,
            @PathVariable String id,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateAddress(principal.getName(), id, request)));
    }

    @DeleteMapping("/me/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(Principal principal, @PathVariable String id) {
        userService.deleteAddress(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/addresses/{id}/default")
    public ResponseEntity<ApiResponse<Void>> setDefaultAddress(
            Principal principal,
            @PathVariable String id) {
        userService.setDefaultAddress(principal.getName(), id);
        return ResponseEntity.ok(ApiResponse.ok("Đã đặt địa chỉ mặc định", null));
    }
}
