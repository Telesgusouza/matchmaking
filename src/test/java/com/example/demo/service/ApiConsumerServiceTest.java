package com.example.demo.service;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.entity.User;
import com.example.demo.enums.Player;
import com.example.demo.enums.UserRole;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.exception.InvalidFieldException;
import com.example.demo.service.exception.ResourceNotFoundException;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

// voltar nesse teste
@SpringBootTest(properties = {

		"AWS_ACESSKEY=aws_acesskey", "AWS_BUCKET=aws_bucket", "AWS_SECRET=aws_secret",

		"JWT_SECRET=my-secret-key-for-tests",

		"aws.acesskey=${AWS_ACESSKEY}", "aws.secrety=${AWS_SECRET}", "aws.bucket=${AWS_BUCKET}",

		"api.security.token.secret=${JWT_SECRET:my-secret-key}",

})
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
public class ApiConsumerServiceTest {

	@Mock
	private OkHttpClient httpClient;

	@Mock
	private Call call;

	@Mock
	private Response response;

	@Mock
	private ResponseBody responseBody;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private ApiConsumerService apiConsumerService;

	/*
	
	ATENÇÃO, DEPOIS CRIAR TESTE DE SUCESSO
	
	*/
	
	@Test
	public void idCannotBeNull() {
		assertThrows(InvalidFieldException.class, () -> apiConsumerService.searchUserPhotoInApi(null));
	}
	
	@Test
	public void userNotFound() {
		UUID uuid = UUID.randomUUID();
		
		when(userRepository.findById(uuid)).thenReturn(Optional.empty());
		
		assertThrows(ResourceNotFoundException	.class, () -> apiConsumerService.searchUserPhotoInApi(uuid));
	}

}






































