package com.banking.service;

import com.banking.dto.user.UserDTO;

import java.util.List;

public interface UserService {
    List<UserDTO> getAllUsers();
    List<UserDTO> getPendingCustomers();
    UserDTO getUserById(Long id);
    UserDTO approveUser(Long id);
}
