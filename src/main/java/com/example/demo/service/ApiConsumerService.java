package com.example.demo.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.repository.UserRepository;
import com.example.demo.service.exception.InvalidFieldException;
import com.example.demo.service.exception.ResourceNotFoundException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Service
public class ApiConsumerService {

	private OkHttpClient httpClient = new OkHttpClient();

	@Autowired
	private UserRepository userRepository;

	// projeto [TicTacToe-be] fornece a foto do usuario pela aws s3
	public String searchUserPhotoInApi(UUID id) throws Exception {

		// ResourceNotFoundException
		if (id == null) {
			throw new InvalidFieldException("id cannot be null");
		} else if (!userRepository.findById(id).isPresent()) {
			throw new ResourceNotFoundException("user not found");
		}

		Request request = new Request.Builder().url("http://localhost:8080/api/v1/file/" + id).build();
		Response response = httpClient.newCall(request).execute();

		try (response) {
			return response.body().string();
		}
	}

}
