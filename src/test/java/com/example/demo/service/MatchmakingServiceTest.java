package com.example.demo.service;

import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.entity.User;
import com.example.demo.enums.UserRole;
import com.example.demo.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class MatchmakingServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private ApiConsumerService apiConsumerService;

	@InjectMocks
	private MatchmakingService matchmakingService;

	private UUID playerId1;
	private UUID playerId2;

	// criar testes para caso de sucesso

	@BeforeEach
	void setUp() {
		playerId1 = UUID.randomUUID();
		playerId2 = UUID.randomUUID();

		User user1 = new User(playerId1, "player1", "email1@example.com", "password", UserRole.LOOKING_FOR_MATCH, null,
				0, 0, 0);
		User user2 = new User(playerId2, "player2", "email2@example.com", "password", UserRole.LOOKING_FOR_MATCH, null,
				0, 0, 0);

		when(userRepository.findById(playerId1)).thenReturn(Optional.of(user1));
		when(userRepository.findById(playerId2)).thenReturn(Optional.of(user2));
	}

}
