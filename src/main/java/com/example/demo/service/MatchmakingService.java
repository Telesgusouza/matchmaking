package com.example.demo.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.example.demo.dtos.MatchDTO;
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

					// descomentar depois
//					updateStatusUsers(playerOneId, playerTwoId);

					Match saveMatch = this.matchRepository.save(newMatch);

					try {
						WebSocketSession sessionOne = waitingPlayers.get(playerOneId);
						WebSocketSession sessionTwo = waitingPlayers.get(playerTwoId);

						if (sessionOne != null && sessionTwo != null) {

							sessionOne.sendMessage(new TextMessage("Match found!: " + newMatch.getId()));
							sessionTwo.sendMessage(new TextMessage("Match found!: " + newMatch.getId()));
						} else {
							// Log ou trata o caso em que uma das sessões não foi encontrada
							System.out.println("WebSocket session not found for one of the players");
						}
					} catch (IOException e) {
						System.out.println("Error sending match found message: " + e);
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

	public void createMatch(UUID idPLayer, UUID idAnotherPlayer) {

		// definindo quem é o player um e dois
		UUID idPlayerOne;
		UUID idPlayerTwo;

		Integer random = new Random().nextInt(2) + 1;
		idPlayerOne = random == 1 ? idPLayer : idAnotherPlayer;
		idPlayerTwo = random == 1 ? idAnotherPlayer : idPLayer;

		// pegando a foto de ambos
		try {

			Match newMatch = new Match(null, LocalDateTime.now(),

					idPlayerOne, idPlayerTwo,

					mapper.readValue(apiConsumerService.searchUserPhotoInApi(idPlayerOne), RequestPhotoS3AwsDTO.class)
							.photo(),
					mapper.readValue(apiConsumerService.searchUserPhotoInApi(idPlayerTwo), RequestPhotoS3AwsDTO.class)
							.photo(),

					0, 0, 0, 0);

			updateStatusUsers(idPlayerOne, idPlayerTwo);

			Match saveMatch = this.matchRepository.save(newMatch);

			try {
				WebSocketSession sessionOne = waitingPlayers.get(idPlayerOne);
				WebSocketSession sessionTwo = waitingPlayers.get(idPlayerTwo);

				if (sessionOne != null && sessionTwo != null) {

					System.out.println("ELE TAMBÉM CHEGOU AQUI?");

					sessionOne.sendMessage(new TextMessage("Match found!: " + newMatch.getId()));
					sessionTwo.sendMessage(new TextMessage("Match found!: " + newMatch.getId()));
				} else {
					// Log ou trata o caso em que uma das sessões não foi encontrada
					System.out.println("WebSocket session not found for one of the players");
				}
			} catch (IOException e) {
				System.out.println("Error sending match found message: " + e);
			}

			removePlayersFromQueue(idPlayerOne, idPlayerTwo);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public UUID findOpponent(UUID playerId) {

		User anotherUser = this.userRepository.findByRoleAndIdNot(UserRole.LOOKING_FOR_MATCH, playerId);
		if (anotherUser != null) {
			return anotherUser.getId();
		}

		return null;
	}

	public WebSocketSession getSessionByUserId(UUID userId) {
		// Implemente a lógica para encontrar a sessão WebSocket associada ao ID do
		// usuário
		// Esta implementação depende de como você está gerenciando as sessões
		// Aqui está um exemplo simples:
		Map<UUID, WebSocketSession> sessions = new HashMap<>();
		return sessions.get(userId);
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

	private void notifyPlayersAboutMatch(UUID player1Id, UUID player2Id, MatchDTO match) {
		try {

			WebSocketSession session1 = waitingPlayers.get(player1Id);
			WebSocketSession session2 = waitingPlayers.get(player2Id);

			if (session1 != null && session1.isOpen()) {
				System.out.println("Total de jogadores na fila: " + waitingPlayers.size());
				session1.sendMessage(new TextMessage("match:" + match.toJson()));
			} else {
				System.out.println("Jogador 1 não tem sessão ativa");
			}

			if (session2 != null && session2.isOpen()) {
				session2.sendMessage(new TextMessage("match_found:" + match.toJson()));
			} else {
				System.out.println("Jogador 2 não tem sessão ativa");
			}

		} catch (IOException e) {
			System.err.println("Erro ao notificar jogadores sobre a partida: " + e.getMessage());
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

}
