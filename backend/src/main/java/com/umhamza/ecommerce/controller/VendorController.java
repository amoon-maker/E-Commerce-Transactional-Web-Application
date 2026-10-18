package com.umhamza.ecommerce.controller;

import com.umhamza.ecommerce.dto.VendorRequest;
import com.umhamza.ecommerce.dto.VendorResponse;
import com.umhamza.ecommerce.service.VendorService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/vendors")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @GetMapping
    public ResponseEntity<List<VendorResponse>> getAllVendors() {
        return ResponseEntity.ok(
                vendorService.getAllVendors()
        );
    }

    @GetMapping("/{vendorId}")
    public ResponseEntity<VendorResponse> getVendorById(
            @PathVariable Long vendorId) {

        return ResponseEntity.ok(
                vendorService.getVendorById(vendorId)
        );
    }

    @PostMapping
    public ResponseEntity<VendorResponse> createVendor(
            @Valid @RequestBody VendorRequest request) {

        VendorResponse createdVendor =
                vendorService.createVendor(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{vendorId}")
                .buildAndExpand(createdVendor.vendorId())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(createdVendor);
    }

    @PutMapping("/{vendorId}")
    public ResponseEntity<VendorResponse> updateVendor(
            @PathVariable Long vendorId,
            @Valid @RequestBody VendorRequest request) {

        return ResponseEntity.ok(
                vendorService.updateVendor(vendorId, request)
        );
    }

    @DeleteMapping("/{vendorId}")
public ResponseEntity<Void> deleteVendor(
        @PathVariable Long vendorId) {

    vendorService.deleteVendor(vendorId);
    return ResponseEntity.noContent().build();
}
}