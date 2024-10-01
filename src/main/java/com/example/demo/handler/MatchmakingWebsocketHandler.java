package com.example.demo.handler;

import java.io.IOException;
import java.util.List;
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

import com.example.demo.config.TokenService;
import com.example.demo.entity.User;
import com.example.demo.enums.UserRole;
import com.example.demo.service.MatchmakingService;
import com.example.demo.service.UserService;
import com.google.gson.Gson;

@Component
public class MatchmakingWebsocketHandler extends TextWebSocketHandler {

	@Autowired
	private MatchmakingService matchmakingService;

	@Autowired
	private UserService useService;

	@Autowired
	private TokenService tokenService;

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		super.afterConnectionEstablished(session);
		System.out.println("[afterConnectionEstablished] session id " + session.getId());

		UUID userId = getUserIdFromSession(session);

		if (userId == null) {
			System.err.println("Erro: ID de usuário não encontrado na sessão");
			return;
		}

		User user = useService.findById(userId);
		if (user != null && user.getRole() == UserRole.LOOKING_FOR_MATCH) {
			matchmakingService.addPlayerToQueue(userId, session);
		} else {
			System.err.println("Erro: Usuário não encontrado ou não está procurando por partida");
		}
	}



//	private UUID getUserIdFromSession(WebSocketSession session) {
//	    // Aqui você precisa implementar a lógica para obter o ID do usuário
//	    // Por exemplo, se o ID estiver na URL da conexão:
//	    String path = session.getUri().getPath();
//	    String[] parts = path.split("/");
//	    
//	    if (parts.length > 2 && parts[2].matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
//	        return UUID.fromString(parts[2]);
//	    }
//	    
//	    // Se o ID não estiver na URL, você pode tentar obtê-lo de um token de autenticação
//	    // ou de outra forma que seja adequada para sua aplicação
//	    
//	    return null;
//	}

	////////////////

	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
//		System.out.println("[handleMessage] session id " + message.getPayload());

		String payload = (String) message.getPayload();

		if (payload.startsWith("match_found")) {

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

		} else if (payload.startsWith("connect")) {
            handleConnectMessage(payload, session);
        }

	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {

		System.out.println("[afterConnectionClosed] session id " + session.getId());
		System.out.println("Encerrado a seção");

		super.afterConnectionClosed(session, status);
		session.close();
	}

	private void handleMatchFound(String payload, WebSocketSession session) {
		try {

			System.out.println("==================== handle Match Found =====================");
			System.out.println(payload);
			System.out.println(payload.length() >= 12);

			if (payload.length() >= 12) {

				MatchmakingService.Match match = new Gson().fromJson(payload.substring(12),
						MatchmakingService.Match.class);

				System.out.println("Partida encontrada: " + match.toJson());

				// Aqui você pode enviar mais informações sobre a partida para o cliente
				session.sendMessage(new TextMessage("match_started:" + match.toJson()));

			}

		} catch (IOException e) {
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
		// Implemente esta lógica para obter o ID do usuário da sessão WebSocket
		// Por exemplo, você poderia extrair o ID da URL da conexão ou de um token de
		// autenticação
		return (UUID) session.getAttributes().get("USER_ID"); // Substitua por sua própria lógica
	}
	
	///////
	 private void handleConnectMessage(String payload, WebSocketSession session) {
		 
		 	System.out.println("================= conectar ==================");
		 	System.out.println(payload);
		 
		 
	        try {
	            String[] parts = payload.split(":");
	            if (parts.length == 2 && parts[0].equals("connect")) {
	                UUID userId = UUID.fromString(parts[1]);
	                session.getAttributes().put("USER_ID", userId);
	                System.out.println("Usuário conectado: " + userId);
	            } else {
	                System.err.println("Mensagem de conexão inválida: " + payload);
	            }
	        } catch (IllegalArgumentException e) {
	            System.err.println("Erro ao processar mensagem de conexão: " + e.getMessage());
	        }
	    }

}
