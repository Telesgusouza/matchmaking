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
	private Map<UUID, MatchDTO> activeMatches = new ConcurrentHashMap<>();

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
		
		System.out.println();
		System.out.println();
		
		System.out.println("======================================");
		System.out.println("======================================");
		
		System.out.println("Add player");
		System.out.println(waitingPlayers);
		System.out.println(activeMatches);
		
		System.out.println();
		System.out.println();
	}

	public UUID findOpponent(UUID playerId) {

		User anotherUser = this.userRepository.findByRoleAndIdNot(UserRole.LOOKING_FOR_MATCH, playerId);
		if (anotherUser != null) {
			return anotherUser.getId();
		}

		return null;
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
			System.out.println();
			System.out.println();
			System.out.println("========================================================");
			System.out.println("==================== CREATE MATCH ======================");
			System.out.println("========================================================");

			System.out.println("player + " + idPLayer);
			System.out.println("another + " + idAnotherPlayer);
			
			System.out.println();
			System.out.println();
			

			Match newMatch = new Match(null, LocalDateTime.now(),

					idPlayerOne, idPlayerTwo,

					mapper.readValue(apiConsumerService.searchUserPhotoInApi(idPlayerOne), RequestPhotoS3AwsDTO.class)
							.photo(),
					mapper.readValue(apiConsumerService.searchUserPhotoInApi(idPlayerTwo), RequestPhotoS3AwsDTO.class)
							.photo(),

					0, 0, 0, 0);

			updateStatusUsers(idPlayerOne, idPlayerTwo);
			
			System.out.println();
			System.out.println();

			System.out.println(newMatch);

			Match saveMatch = this.matchRepository.save(newMatch);

			System.out.println(saveMatch);
			
			try {
				
				System.out.println("=================================");
				System.out.println(waitingPlayers);
				
				
				WebSocketSession sessionOne = waitingPlayers.get(idPlayerOne);
				WebSocketSession sessionTwo = waitingPlayers.get(idPlayerTwo);

				System.out.println();
				System.out.println();

				System.out.println("CHEGAMOS NO TRY E CATCH");

				System.out.println(sessionOne);
				System.out.println(sessionTwo);

				System.out.println("ELE TAMBÉM CHEGOU AQUI blz");

				if (sessionOne != null && sessionTwo != null) {

					System.out.println("ELE TAMBÉM CHEGOU AQUI?");

					sessionOne.sendMessage(new TextMessage("Match found!: " + newMatch));
					sessionTwo.sendMessage(new TextMessage("Match found!: " + newMatch));
				} else {
					// Log ou trata o caso em que uma das sessões não foi encontrada
					System.out.println("WebSocket session not found for one of the players");
				}
			} catch (IOException e) {
				System.out.println("Error sending match found message: " + e);
			}

//			waitingPlayers.get(idPlayerOne).sendMessage(new TextMessage("create match:" + saveMatch.getId()));
//			waitingPlayers.get(idPlayerTwo).sendMessage(new TextMessage("create match:" + saveMatch.getId()));

			System.out.println("CHEGOU NO FINAL: create match");
			System.out.println();
			System.out.println();

			removePlayersFromQueue(idPlayerOne, idPlayerTwo);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public WebSocketSession getSessionByUserId(UUID userId) {
		// Implemente a lógica para encontrar a sessão WebSocket associada ao ID do
		// usuário
		// Esta implementação depende de como você está gerenciando as sessões
		// Aqui está um exemplo simples:
		Map<UUID, WebSocketSession> sessions = new HashMap<>();
		return sessions.get(userId);
	}

	private void updateStatusUsers(UUID idPlayerOne, UUID idPlayerTwo) {
		Optional<User> userPlayerOneOptional = this.userRepository.findById(idPlayerOne);
		Optional<User> userPlayerTwoOptional = this.userRepository.findById(idPlayerTwo);

		User userPlayerOne = userPlayerOneOptional.orElseThrow(() -> new RuntimeException("User not found"));
		User userPlayerTwo = userPlayerOneOptional.orElseThrow(() -> new RuntimeException("User not found"));

		userPlayerOne.setRole(UserRole.ON_DEPARTURE);
		userPlayerTwo.setRole(UserRole.ON_DEPARTURE);

		this.userRepository.save(userPlayerOne);
		this.userRepository.save(userPlayerTwo);
	}

	private void checkForMatch() {
		if (waitingPlayers.size() >= 2) {
			UUID player1Id = waitingPlayers.keySet().iterator().next();
			UUID player2Id = waitingPlayers.keySet().stream().skip(1).findFirst().orElse(null);

			if (player2Id != null) {

				MatchDTO match = new MatchDTO(player1Id, player2Id);

				notifyPlayersAboutMatch(player1Id, player2Id, match);

//				removePlayersFromQueue(player1Id, player2Id);

				activeMatches.put(UUID.fromString(match.getId()), match);

			}
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

		    if (session1!= null && session1.isOpen()) {
		    	System.out.println("Total de jogadores na fila: " + waitingPlayers.size());
		        session1.sendMessage(new TextMessage("match_found:" + match.toJson()));
		    } else {
		        System.out.println("Jogador 1 não tem sessão ativa");
		    }

		    if (session2!= null && session2.isOpen()) {
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
