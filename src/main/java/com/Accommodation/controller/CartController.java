package com.Accommodation.controller;

import com.Accommodation.dto.CartItemDto;
import com.Accommodation.dto.CartListItemDto;
import com.Accommodation.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/cart")
    public String cartList(Model model, Principal principal) {
        List<CartListItemDto> cartItems = cartService.getCartItems(principal.getName());
        CartListItemDto unavailableCartItem = cartService.findFirstUnavailableCartItem(principal.getName());
        int totalPrice = cartItems.stream()
                .mapToInt(CartListItemDto::getTotalPrice)
                .sum();

        model.addAttribute("compactHeader", true);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartCount", cartItems.size());
        model.addAttribute("cartTotalPrice", totalPrice);
        model.addAttribute("unavailableCartAccomId",
                unavailableCartItem != null ? unavailableCartItem.getAccomId() : null);
        model.addAttribute("unavailableCartAccomName",
                unavailableCartItem != null ? unavailableCartItem.getAccomName() : null);
        return "cart/cartList";
    }

    @PostMapping("/cart")
    @ResponseBody
    public ResponseEntity<?> addToCart(@RequestBody @Valid CartItemDto cartItemDto,
                                       BindingResult bindingResult,
                                       Principal principal) {
        if (bindingResult.hasErrors()) {
            StringBuilder sb = new StringBuilder();
            for (FieldError error : bindingResult.getFieldErrors()) {
                sb.append(error.getDefaultMessage());
            }
            return new ResponseEntity<>(sb.toString(), HttpStatus.BAD_REQUEST);
        }

        Long cartItemId;
        try {
            cartItemId = cartService.addCartItem(cartItemDto, principal.getName());
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(cartItemId, HttpStatus.OK);
    }

    @PutMapping("/cart/{cartItemId}")
    @ResponseBody
    public ResponseEntity<?> updateCartItem(@PathVariable Long cartItemId,
                                            @RequestBody @Valid CartItemDto cartItemDto,
                                            BindingResult bindingResult,
                                            Principal principal) {
        if (bindingResult.hasErrors()) {
            StringBuilder sb = new StringBuilder();
            for (FieldError error : bindingResult.getFieldErrors()) {
                sb.append(error.getDefaultMessage());
            }
            return new ResponseEntity<>(sb.toString(), HttpStatus.BAD_REQUEST);
        }

        try {
            cartService.updateCartItem(cartItemId, cartItemDto, principal.getName());
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(cartItemId, HttpStatus.OK);
    }

    @DeleteMapping("/cart/{cartItemId}")
    @ResponseBody
    public ResponseEntity<?> removeCartItem(@PathVariable Long cartItemId,
                                            Principal principal) {
        try {
            cartService.removeCartItem(cartItemId, principal.getName());
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(cartItemId, HttpStatus.OK);
    }

    @PostMapping("/cart/{cartItemId}/confirm")
    @ResponseBody
    public ResponseEntity<?> confirmCartItem(@PathVariable Long cartItemId,
                                             Principal principal) {
        Long orderId;
        try {
            orderId = cartService.confirmCartItem(cartItemId, principal.getName());
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(orderId, HttpStatus.OK);
    }

    @PostMapping("/cart/confirm-all")
    @ResponseBody
    public ResponseEntity<?> confirmAllCartItems(Principal principal) {
        try {
            List<Long> orderIds = cartService.confirmAllCartItems(principal.getName());
            return new ResponseEntity<>(orderIds, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
