package com.example.demo.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.entity.User;
import com.example.demo.enums.UserRole;
import com.example.demo.repository.UserRepository;

@Service
public class UserService {

	@Autowired
	private UserRepository userRepository;

	public User findById(UUID id) {
		Optional<User> user = this.userRepository.findById(id);
		return user.orElseThrow();
	}
	
	public void lookingForMatch(UUID id) {
		Optional<User> userOptional = this.userRepository.findById(id);
		User user = userOptional.orElseThrow(() -> new RuntimeException("User not found"));
		user.setRole(UserRole.LOOKING_FOR_MATCH);
		
		this.userRepository.save(user);
	}
}
