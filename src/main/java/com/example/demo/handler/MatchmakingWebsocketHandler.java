package com.example.demo.handler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class MatchmakingWebsocketHandler extends TextWebSocketHandler {

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		// TODO Auto-generated method stub
		System.out.println("[afterConnectionEstablished] session id " + session.getId());
		System.out.println("coneção estabelecida com sucesso");
		
		super.afterConnectionEstablished(session);
	}

	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
		
		System.out.println("[handleMessage] session id " + message.getPayload());
		System.out.println("Mensagem enviada");
//		super.handleMessage(session, message);
		
		String payload = (String) message.getPayload();
		
		if ("pong".equals(payload)) {
			ScheduledExecutorService executorService = Executors
					.newSingleThreadScheduledExecutor();
			
			Runnable task = () -> {
				try {
					session.sendMessage(message);
				} catch (Exception e) {
					throw new RuntimeException("Erro ao enviar mensagem > ", e);
				}
			};
			
			executorService.schedule(task, 4, TimeUnit.SECONDS);
			
			executorService.shutdown();
			
			System.out.println("Enviado a mensagem de ping");
			
		}
		
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		
		System.out.println("[afterConnectionClosed] session id " + session.getId());
		System.out.println("Encerrado a seção");
		
		super.afterConnectionClosed(session, status);
		session.close();
	}

}


























