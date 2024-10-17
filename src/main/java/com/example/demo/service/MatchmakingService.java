package com.example.demo.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.example.demo.dtos.RequestPhotoS3AwsDTO;
import com.example.demo.entity.Match;
import com.example.demo.entity.User;
import com.example.demo.enums.UserRole;
import com.example.demo.repository.MatchRepository;
import com.example.demo.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class MatchmakingService {

	private Map<UUID, WebSocketSession> waitingPlayers = new ConcurrentHashMap<>();
	private Map<UUID, Match> activeMatches = new ConcurrentHashMap<>();

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ApiConsumerService apiConsumerService;

	@Autowired
	private MatchRepository matchRepository;

	private ObjectMapper mapper = new ObjectMapper();

	public void addPlayerToQueue(UUID playerId, WebSocketSession session) {
		waitingPlayers.put(playerId, session);
		checkForMatch();
	}

	private void checkForMatch() {
		if (waitingPlayers.size() >= 2) {
			UUID playerOneId = waitingPlayers.keySet().iterator().next();
			UUID playerTwoId = waitingPlayers.keySet().stream().skip(1).findFirst().orElse(null);

			if (playerTwoId != null) {

				try {

					RequestPhotoS3AwsDTO photoPlayerOne = mapper.readValue(
							apiConsumerService.searchUserPhotoInApi(playerOneId), RequestPhotoS3AwsDTO.class);
					RequestPhotoS3AwsDTO photoPlayerTwo = mapper.readValue(
							apiConsumerService.searchUserPhotoInApi(playerTwoId), RequestPhotoS3AwsDTO.class);

					photoPlayerOne = photoPlayerOne != null ? photoPlayerOne : null;
					photoPlayerTwo = photoPlayerTwo != null ? photoPlayerTwo : null;

					Match newMatch = new Match(null, LocalDateTime.now(),

							playerOneId, playerTwoId,

							photoPlayerOne.photo(), photoPlayerTwo.photo(),

							0, 0, 0, 0);

					updateStatusUsers(playerOneId, playerTwoId);

					Match saveMatch = this.matchRepository.save(newMatch);

					WebSocketSession sessionOne = waitingPlayers.get(playerOneId);
					WebSocketSession sessionTwo = waitingPlayers.get(playerTwoId);

					if (sessionOne != null && sessionTwo != null) {

						handleMessage("Match found!: " + newMatch.getId(), sessionOne);
						handleMessage("Match found!: " + newMatch.getId(), sessionTwo);

					} else {

						// Log ou trata o caso em que uma das sessões não foi encontrada
						System.out.println("WebSocket session not found for one of the players");
						handleMessage("WebSocket session not found for one of the players", sessionOne);
						handleMessage("WebSocket session not found for one of the players", sessionTwo);

					}

					removePlayersFromQueue(playerOneId, playerTwoId);

					activeMatches.put(saveMatch.getId(), saveMatch);

				} catch (Exception e) {
					// TODO: handle exception
					System.out.println("surgiu um erro inesperado");
				}

			}
		}
	}

	public UUID findOpponent(UUID playerId) {

		User anotherUser = this.userRepository.findByRoleAndIdNot(UserRole.LOOKING_FOR_MATCH, playerId);
		if (anotherUser != null) {
			return anotherUser.getId();
		}

		return null;
	}

	private void updateStatusUsers(UUID... playerIds) {
		for (UUID playerId : playerIds) {
			Optional<User> userOptional = this.userRepository.findById(playerId);
			User user = userOptional.orElseThrow(() -> new RuntimeException("User not found"));

			user.setRole(UserRole.ON_DEPARTURE);
			this.userRepository.save(user);

		}

	}

	public void removePlayersFromQueue(UUID... playerIds) {
		for (UUID playerId : playerIds) {

			waitingPlayers.remove(playerId);
		}
	}

	public void removePlayerFromQueue(UUID playerId) {
		WebSocketSession removed = waitingPlayers.remove(playerId);
		System.out.println(
				"Removido jogador " + playerId + " da fila de espera. Total de jogadores: " + waitingPlayers.size());
		if (removed == null) {
			System.out.println("Aviso: Tentativa de remover jogador não presente na fila.");
		}
	}

	private void handleMessage(String message, WebSocketSession session) {
		try {
			session.sendMessage(new TextMessage(message));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
