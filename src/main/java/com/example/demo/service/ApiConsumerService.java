package com.example.demo.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.demo.dtos.RequestPhotoS3AwsDTO;
import com.google.gson.Gson;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Service
public class ApiConsumerService {

	private final OkHttpClient httpClient = new OkHttpClient();

	// projeto [TicTacToe-be] fornece a foto do usuario pela aws s3
	public String searchUserPhotoInApi(UUID id) throws Exception {
		Request request = new Request.Builder().url("http://localhost:8080/api/v1/file/" + id).build();
		Response response = httpClient.newCall(request).execute();

		try (response) {
			return response.body().string();
		}
	}

}
