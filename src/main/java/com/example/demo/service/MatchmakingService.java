package com.example.demo.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.example.demo.entity.User;
import com.example.demo.enums.UserRole;
import com.example.demo.repository.UserRepository;

@Service
public class MatchmakingService {

	private Map<UUID, WebSocketSession> waitingPlayers = new ConcurrentHashMap<>();
	private Map<UUID, Match> activeMatches = new ConcurrentHashMap<>();
	
	@Autowired
	private UserRepository userRepository;

	public void addPlayerToQueue(UUID playerId, WebSocketSession session) {
		waitingPlayers.put(playerId, session);
		checkForMatch();
	}
	
	///////////////////////////////////
	
	public UUID findOpponent(UUID playerId) {

		
		User anotherUser = this.userRepository.findByRoleAndIdNot(UserRole.LOOKING_FOR_MATCH, playerId);
		if (anotherUser != null) {
			return anotherUser.getId();
		} 
		
	    return null;
	}

	public WebSocketSession getSessionByUserId(UUID userId) {
	    // Implemente a lógica para encontrar a sessão WebSocket associada ao ID do usuário
	    // Esta implementação depende de como você está gerenciando as sessões
	    // Aqui está um exemplo simples:
	    Map<UUID, WebSocketSession> sessions = new HashMap<>();
	    return sessions.get(userId);
	}
	
	///////////////////////////////////
	

	private void checkForMatch() {
		if (waitingPlayers.size() >= 2) {
			UUID player1Id = waitingPlayers.keySet().iterator().next();
			UUID player2Id = waitingPlayers.keySet().stream().skip(1).findFirst().orElse(null);

			if (player2Id != null) {
				Match match = new Match(player1Id, player2Id);
				activeMatches.put(UUID.fromString(match.getId()), match);

				removePlayersFromQueue(player1Id, player2Id);
				notifyPlayersAboutMatch(player1Id, player2Id, match);
			}
		}
	}

	private void removePlayersFromQueue(UUID... playerIds) {
		for (UUID playerId : playerIds) {
			waitingPlayers.remove(playerId);
		}
	}

	private void notifyPlayersAboutMatch(UUID player1Id, UUID player2Id, Match match) {
		try {
			waitingPlayers.get(player1Id).sendMessage(new TextMessage("match_found:" + match.toJson()));
			waitingPlayers.get(player2Id).sendMessage(new TextMessage("match_found:" + match.toJson()));
		} catch (IOException e) {
			System.err.println("Erro ao notificar jogadores sobre a partida: " + e.getMessage());
		}
	}

	public void removePlayerFromQueue(UUID playerId) {
		waitingPlayers.remove(playerId);
	}

	public static class Match {
		private String id;
		private UUID player1Id;
		private UUID player2Id;

		public Match(UUID player1Id, UUID player2Id) {
			this.id = UUID.randomUUID().toString();
			this.player1Id = player1Id;
			this.player2Id = player2Id;
		}

		public String getId() {
			return id;
		}

		public String toJson() {
			return "{\"id\":\"" + id + "\",\"player1\":\"" + player1Id + "\",\"player2\":\"" + player2Id + "\"}";
		}
	}

}
