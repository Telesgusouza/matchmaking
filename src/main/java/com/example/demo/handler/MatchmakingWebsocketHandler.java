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

import com.example.demo.service.MatchmakingService;
import com.example.demo.service.UserService;

@Component
public class MatchmakingWebsocketHandler extends TextWebSocketHandler {

	@Autowired
	private MatchmakingService matchmakingService;

	@Autowired
	private UserService useService;

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		super.afterConnectionEstablished(session);
		System.out.println("[afterConnectionEstablished] session id " + session.getId());

		UUID userId = getUserIdFromSession(session);

		if (userId == null) {
			System.err.println("Erro: ID de usuário não encontrado na sessão");
			messageWebsockets("Erro: ID de usuário não encontrado na sessão", session);

			return;
		}

//		User user = useService.findById(userId);
		this.useService.lookingForMatch(userId);

		matchmakingService.addPlayerToQueue(userId, session);
	}

	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
		System.out.println("[handleMessage] session id " + message.getPayload());

		String payload = (String) message.getPayload();

		if ("pong".equals(payload)) {
			ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

			Runnable task = () -> {
				try {
					messageWebsockets("ping", session);
				} catch (Exception e) {
					throw new RuntimeException("Erro ao enviar mensagem > ", e);
				}
			};

			executorService.schedule(task, 4, TimeUnit.SECONDS);

			executorService.shutdown();

		}

	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		System.out.println("[afterConnectionClosed] session id " + session.getId());

		super.afterConnectionClosed(session, status);

		UUID idUser = getUserIdFromSession(session);

		matchmakingService.removePlayersFromQueue(idUser);

		session.close();
	}

	private UUID getUserIdFromSession(WebSocketSession session) {

		Optional<String> uriIdUser = Optional.ofNullable(session.getUri()).map(UriComponentsBuilder::fromUri)
				.map(UriComponentsBuilder::build).map(UriComponents::getQueryParams).map(it -> it.get("id_user"))
				.flatMap(it -> it.stream().findFirst()).map(String::trim);

		return UUID.fromString(uriIdUser.orElseThrow().toString());
	}

	private void messageWebsockets(String message, WebSocketSession session) {
		try {
			session.sendMessage(new TextMessage(message));
		} catch (IOException e) {
			new RuntimeException("Error ao enviar mensagem: " + e);
		}
	}
}
