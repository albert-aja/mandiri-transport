package com.mandiritransport.ops.customer.dto;

import com.mandiritransport.ops.customer.entity.CustomerAddress;
import com.mandiritransport.ops.customer.entity.CustomerNote;
import com.mandiritransport.ops.pricing.entity.CustomerPricingAgreement;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public final class CustomerDtos {

    private CustomerDtos() {
    }

    public record CustomerRequest(
            @NotBlank(message = "Nama pelanggan wajib diisi") String name,
            @NotBlank(message = "Nomor telepon wajib diisi") String phone,
            String whatsappPhone,
            String companyName,
            String preferredVehicleType
    ) {
    }

    public record CustomerAddressRequest(
            @NotBlank(message = "Label alamat wajib diisi") String label,
            @NotBlank(message = "Alamat lengkap wajib diisi") String addressText,
            BigDecimal latitude,
            BigDecimal longitude,
            String googleMapsUrl,
            Boolean isDefault
    ) {
    }

    public record CustomerNoteRequest(
            String category,
            @NotBlank(message = "Isi catatan wajib diisi") String noteText,
            Boolean visibleToDrivers
    ) {
    }

    public record CustomerDetailDto(
            String id,
            String name,
            String phone,
            String whatsappPhone,
            String companyName,
            String preferredVehicleType,
            String whatsappUrl,
            List<CustomerAddress> addresses,
            List<CustomerNote> notes,
            List<CustomerPricingAgreement> pricingAgreements
    ) {
    }

    public record RenderWhatsappRequest(
            @NotBlank(message = "templateCode wajib diisi") String templateCode,
            String tripId,
            String bookingId,
            String phone
    ) {
    }

    public record RenderWhatsappResponse(
            String phone,
            String renderedMessage,
            String whatsappUrl
    ) {
    }
}
