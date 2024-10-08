package com.example.demo.dtos;

import java.util.UUID;

public class MatchDTO {

	private String id;
	private UUID idPlayerOne;
	private UUID idPlayerTwo;

	public MatchDTO(UUID idPlayerOne, UUID idPlayerTwo) {
		this.id = UUID.randomUUID().toString();
		this.idPlayerOne = idPlayerOne;
		this.idPlayerTwo = idPlayerTwo;
	}

	public String getId() {
		return id;
	}

	public String toJson() {
		return "{\"id\":\"" + id + "\",\"player1\":\"" + idPlayerOne + "\",\"player2\":\"" + idPlayerTwo + "\"}";
	}

}
