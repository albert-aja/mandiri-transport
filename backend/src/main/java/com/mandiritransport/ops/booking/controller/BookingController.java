package com.mandiritransport.ops.booking.controller;

import com.mandiritransport.ops.booking.dto.BookingDtos.BookingDetailDto;
import com.mandiritransport.ops.booking.dto.BookingDtos.BookingTemplateRequest;
import com.mandiritransport.ops.booking.dto.BookingDtos.QuickBookingRequest;
import com.mandiritransport.ops.booking.entity.BookingTemplate;
import com.mandiritransport.ops.booking.service.BookingService;
import com.mandiritransport.ops.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('OWNER')")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/booking-templates")
    public ApiResponse<List<BookingTemplate>> listTemplates() {
        return ApiResponse.ok(bookingService.listTemplates());
    }

    @PostMapping("/booking-templates")
    public ApiResponse<BookingTemplate> createTemplate(@Valid @RequestBody BookingTemplateRequest req) {
        return ApiResponse.ok(bookingService.createTemplate(req));
    }

    @DeleteMapping("/booking-templates/{id}")
    public ApiResponse<Map<String, Object>> deleteTemplate(@PathVariable String id) {
        bookingService.deleteTemplate(id);
        return ApiResponse.ok(Map.of("deleted", true));
    }

    @GetMapping("/bookings")
    public ApiResponse<List<BookingDetailDto>> listBookings(
            @RequestParam(value = "status", required = false) String status) {
        return ApiResponse.ok(bookingService.listBookings(status));
    }

    @GetMapping("/bookings/{id}")
    public ApiResponse<BookingDetailDto> getBooking(@PathVariable String id) {
        return ApiResponse.ok(bookingService.getBooking(id));
    }

    @PostMapping("/bookings/quick")
    public ApiResponse<BookingDetailDto> createQuickBooking(@Valid @RequestBody QuickBookingRequest req) {
        return ApiResponse.ok(bookingService.createQuickBooking(req));
    }
}
