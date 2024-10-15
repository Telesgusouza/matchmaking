package com.example.demo.handler;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.demo.config.TokenService;
import com.example.demo.entity.User;
import com.example.demo.enums.UserRole;
import com.example.demo.service.MatchmakingService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class MatchmakingWebsocketHandler extends TextWebSocketHandler {

	@Autowired
	private MatchmakingService matchmakingService;

	@Autowired
	private UserService useService;

	@Autowired
	private TokenService tokenService;

	private ObjectMapper mapper = new ObjectMapper();

	private UUID idUser;

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		super.afterConnectionEstablished(session);
		System.out.println("[afterConnectionEstablished] session id " + session.getId());

		UUID userId = getUserIdFromSession(session);

		if (userId == null) {
			System.err.println("Erro: ID de usuário não encontrado na sessão");
			return;
		} else {
			idUser = userId;
		}

		User user = useService.findById(userId);

		if (user != null && user.getRole() == UserRole.LOOKING_FOR_MATCH) {

			matchmakingService.addPlayerToQueue(userId, session);
		} else {
			System.err.println("Erro: Usuário não encontrado ou não está procurando por partida");
		}
	}

	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
//		System.out.println("[handleMessage] session id " + message.getPayload());

		String payload = (String) message.getPayload();

		if (payload.startsWith("match_found")) {

			System.out.println();
			System.out.println();

			System.out.println("====================================");
			System.out.println("aqui esta tudo bem  :|");

			System.out.println();
			System.out.println();

			handleMatchFound(payload, session);

		} else if ("pong".equals(payload)) {
			ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

			Runnable task = () -> {
				try {
					session.sendMessage(new TextMessage("ping"));
				} catch (Exception e) {
					throw new RuntimeException("Erro ao enviar mensagem > ", e);
				}
			};

			executorService.schedule(task, 4, TimeUnit.SECONDS);

			executorService.shutdown();

		}

//		DEIXOU DE EXISTIR POR ENQUANTO
//		else if (payload.startsWith("connect")) {
//			handleConnectMessage(payload, session);
//		}

	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		System.out.println("[afterConnectionClosed] session id " + session.getId());

		super.afterConnectionClosed(session, status);

		UUID idUser = getUserIdFromSession(session);

		matchmakingService.removePlayersFromQueue(idUser);

		session.close();
	}

	private void handleMatchFound(String payload, WebSocketSession session) {
		try {

			UUID idPlayer = getUserIdFromSession(session);

			UUID anotherUser = matchmakingService.findOpponent(idPlayer);

			if (anotherUser == null) {
				ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
				executor.schedule(() -> {
					try {

						System.out.println();
						System.out.println();

						System.out.println("====================================");
						System.out.println("passou por aqui");

						System.out.println();
						System.out.println();

						handleMatchFound(payload, session);

					} catch (RuntimeException e) {
						System.err.println("Erro ao enviar ping: " + e.getMessage());
					}
				}, 5, TimeUnit.SECONDS);
			}

			matchmakingService.createMatch(idPlayer, anotherUser);

//			if (payload.length() >= 12) {
//				
//				Match
////
////				MatchmakingService.Match match = new Gson().fromJson(payload.substring(12),
////						MatchmakingService.Match.class);
//
//				System.out.println("Partida encontrada: " + match.toJson());
//
//				// Aqui você pode enviar mais informações sobre a partida para o cliente
//				session.sendMessage(new TextMessage("match_started:" + match.toJson()));
//
//			}

		} catch (RuntimeException e) {
			System.err.println("Erro ao tratar mensagem de partida encontrada: " + e.getMessage());
		}
	}

	private void sendPing(WebSocketSession session) {
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		executor.schedule(() -> {
			try {
				session.sendMessage(new TextMessage("ping"));
			} catch (IOException e) {
				System.err.println("Erro ao enviar ping: " + e.getMessage());
			}
		}, 5, TimeUnit.SECONDS);
	}

	private UUID getUserIdFromSession(WebSocketSession session) {

		Optional<String> uriIdUser = Optional.ofNullable(session.getUri()).map(UriComponentsBuilder::fromUri)
				.map(UriComponentsBuilder::build).map(UriComponents::getQueryParams).map(it -> it.get("id_user"))
				.flatMap(it -> it.stream().findFirst()).map(String::trim);

//		return (UUID) session.getAttributes().get("USER_ID"); // Substitua por sua própria lógica
		return UUID.fromString(uriIdUser.orElseThrow().toString());
	}

	///////
	private void handleConnectMessage(String payload, WebSocketSession session) {

		try {
			String[] parts = payload.split(":");

			if (parts.length == 2 && parts[0].equals("connect")) {

				UUID userId = UUID.fromString(parts[1]);
				session.getAttributes().put("USER_ID", userId);

			} else {
				System.err.println("Mensagem de conexão inválida: " + payload);
			}
		} catch (IllegalArgumentException e) {
			System.err.println("Erro ao processar mensagem de conexão: " + e.getMessage());
		}
	}

}
