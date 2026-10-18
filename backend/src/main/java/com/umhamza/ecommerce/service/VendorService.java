package com.umhamza.ecommerce.service;

import com.umhamza.ecommerce.dto.VendorRequest;
import com.umhamza.ecommerce.dto.VendorResponse;
import com.umhamza.ecommerce.entity.Vendor;
import com.umhamza.ecommerce.repository.VendorRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class VendorService {

    private final VendorRepository vendorRepository;

    public VendorService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    @Transactional
    public VendorResponse createVendor(VendorRequest request) {
        String businessName = request.businessName().trim();

        if (vendorRepository.existsByBusinessNameIgnoreCase(businessName)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A vendor with this business name already exists."
            );
        }

        Vendor vendor = new Vendor();

        vendor.setBusinessName(businessName);
        vendor.setContactName(clean(request.contactName()));
        vendor.setEmail(clean(request.email()));
        vendor.setPhone(clean(request.phone()));
        vendor.setAddressLine1(clean(request.addressLine1()));
        vendor.setCity(clean(request.city()));
        vendor.setEmirate(clean(request.emirate()));
        vendor.setCountry(request.country().trim());
        vendor.setActive(
                request.active() == null || request.active()
        );

        Vendor savedVendor = vendorRepository.save(vendor);

        return toResponse(savedVendor);
    }

    @Transactional
public VendorResponse updateVendor(
        Long vendorId,
        VendorRequest request) {

    Vendor vendor = vendorRepository.findById(vendorId)
            .orElseThrow(() -> new EntityNotFoundException(
                    "Vendor not found with ID: " + vendorId
            ));

    String businessName = request.businessName().trim();

    if (vendorRepository
            .existsByBusinessNameIgnoreCaseAndVendorIdNot(
                    businessName, vendorId)) {

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A vendor with this business name already exists."
        );
    }

    vendor.setBusinessName(businessName);
    vendor.setContactName(clean(request.contactName()));
    vendor.setEmail(clean(request.email()));
    vendor.setPhone(clean(request.phone()));
    vendor.setAddressLine1(clean(request.addressLine1()));
    vendor.setCity(clean(request.city()));
    vendor.setEmirate(clean(request.emirate()));
    vendor.setCountry(request.country().trim());

    if (request.active() != null) {
        vendor.setActive(request.active());
    }

    Vendor updatedVendor = vendorRepository.save(vendor);

    return toResponse(updatedVendor);
}

@Transactional
public void deleteVendor(Long vendorId) {
    Vendor vendor = vendorRepository.findById(vendorId)
            .orElseThrow(() -> new EntityNotFoundException(
                    "Vendor not found with ID: " + vendorId
            ));

    vendorRepository.delete(vendor);
}
    public List<VendorResponse> getAllVendors() {
        return vendorRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public VendorResponse getVendorById(Long vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Vendor not found with ID: " + vendorId
                ));

        return toResponse(vendor);
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private VendorResponse toResponse(Vendor vendor) {
        return new VendorResponse(
                vendor.getVendorId(),
                vendor.getBusinessName(),
                vendor.getContactName(),
                vendor.getEmail(),
                vendor.getPhone(),
                vendor.getAddressLine1(),
                vendor.getCity(),
                vendor.getEmirate(),
                vendor.getCountry(),
                vendor.isActive(),
                vendor.getCreatedAt(),
                vendor.getUpdatedAt()
        );
    }
}